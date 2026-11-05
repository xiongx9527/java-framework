package com.xiongx9527.fw.plus.ai.askdata.llm;

import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 韧性 {@link LlmPort} 装饰器（§6.26 / D.4）：在「同一模型内」提供重试 + 熔断，
 * 不负责跨供应商故障转移（属产品策略，见 §6.26.13）。
 *
 * <p>职责：
 * <ul>
 *   <li>重试：仅对可重试异常（网络 / 超时 / 429 / 5xx 等瞬时错误）按指数退避重试，默认 2 次；</li>
 *   <li>熔断：连续可重试失败达到阈值后打开，冷却期内直接拒绝（不再打挂掉的依赖），
 *       冷却结束后放一个探活请求（HALF_OPEN），成功则关闭，失败则重新打开；</li>
 *   <li>仅记元数据日志（重试次数 / 熔断状态切换），不记提示词、模型原文或业务数据（§6.37）。</li>
 * </ul>
 *
 * <p><b>流式通道不重试</b>：流式一旦向调用方吐出过增量，重放会产生重复内容；
 * 流式请求直接透传，韧性只保护非流式调用（{@link #complete(LlmRequest)}）。</p>
 *
 * <p>线程安全：熔断状态机在内部锁上串行化；计数器与开关均为易失 / 原子语义。</p>
 */
public class ResilientLlmPort implements LlmPort {

    private static final Logger log = LoggerFactory.getLogger(ResilientLlmPort.class);

    private enum Breaker { CLOSED, OPEN, HALF_OPEN }

    private static final class State {
        final Breaker breaker;
        final int consecutiveFailures;
        final long openedAt;

        State(Breaker breaker, int consecutiveFailures, long openedAt) {
            this.breaker = breaker;
            this.consecutiveFailures = consecutiveFailures;
            this.openedAt = openedAt;
        }
    }

    /** 默认瞬时错误判定：网络 / 超时 / 429 / 5xx / 连接重置等。 */
    public static final Predicate<Throwable> TRANSIENT = t -> {
        if (t == null) {
            return false;
        }
        if (t instanceof IOException || t instanceof SocketTimeoutException
                || t instanceof TimeoutException) {
            return true;
        }
        Throwable cause = t.getCause();
        if (cause instanceof IOException || cause instanceof SocketTimeoutException
                || cause instanceof TimeoutException) {
            return true;
        }
        String m = t.getMessage();
        if (m == null) {
            return false;
        }
        String lower = m.toLowerCase(Locale.ROOT);
        return lower.contains("timeout") || lower.contains("timed out")
                || lower.contains("429") || lower.contains("503") || lower.contains("504")
                || lower.contains("connection") || lower.contains("reset")
                || lower.contains("socket") || lower.contains("unavailable")
                || lower.contains("try again") || lower.contains("econnrefused")
                || lower.contains("rate limit");
    };

    @Getter
    @Builder
    public static class ResilienceConfig {
        /** 额外重试次数（不含首次），默认 2。 */
        @lombok.Builder.Default
        private final int maxRetries = 2;
        /** 退避基准毫秒，默认 200。 */
        @lombok.Builder.Default
        private final long baseDelayMs = 200L;
        /** 退避上限毫秒，默认 1000。 */
        @lombok.Builder.Default
        private final long maxDelayMs = 1000L;
        /** 连续可重试失败达到该值即打开熔断，默认 3。 */
        @lombok.Builder.Default
        private final int failureThreshold = 3;
        /** 熔断冷却毫秒，默认 30000。 */
        @lombok.Builder.Default
        private final long cooldownMs = 30000L;
        /** 非可重试异常是否也计入熔断计数，默认 false（熔断只反映瞬时故障）。 */
        @lombok.Builder.Default
        private final boolean countNonRetryableAsFailure = false;
        /** 可重试判定，默认 {@link #TRANSIENT}。 */
        @lombok.Builder.Default
        private final Predicate<Throwable> retryable = TRANSIENT;
    }

    private final LlmPort delegate;
    private final ResilienceConfig config;
    private final Object lock = new Object();
    private volatile State state = new State(Breaker.CLOSED, 0, 0L);

    public ResilientLlmPort(LlmPort delegate) {
        this(delegate, ResilienceConfig.builder().build());
    }

    public ResilientLlmPort(LlmPort delegate, ResilienceConfig config) {
        if (delegate == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.delegate_required"));
        }
        this.delegate = delegate;
        this.config = config == null ? ResilienceConfig.builder().build() : config;
    }

    /** 当前熔断状态，供观测（不暴露内部计数细节）。 */
    public String getCircuitState() {
        return state.breaker.name();
    }

    /** 流式通道直接透传（重放已吐出的增量会产生重复内容）。 */
    @Override
    public List<ModelInfo> listModels() {
        return delegate.listModels();
    }

    @Override
    public ModelValidation validateModel(String model) {
        return delegate.validateModel(model);
    }

    @Override
    public LlmTokenStream stream(LlmRequest request) {
        return delegate.stream(request);
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        boolean halfOpen;
        synchronized (lock) {
            halfOpen = state.breaker == Breaker.HALF_OPEN;
        }
        int maxAttempts = halfOpen ? 1 : (1 + Math.max(0, config.getMaxRetries()));
        Throwable last = null;
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            if (!allowRequest()) {
                throw new LlmResilienceException(I18nMessageResolver.resolveKey("askdata.llm.breaker_open"));
            }
            try {
                LlmResponse result = delegate.complete(request);
                onSuccess();
                return result;
            } catch (Exception e) {
                last = e;
                boolean retryable = config.getRetryable().test(e);
                // 每一次物理调用失败都计入熔断（一次请求内的多次重试也是多次模型失败）
                onFailure(e, retryable);
                if (attempt < maxAttempts - 1 && retryable) {
                    long delay = backoff(attempt);
                    if (delay > 0) {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new LlmResilienceException(I18nMessageResolver.resolveKey("askdata.llm.retry_interrupted"), ie);
                        }
                    }
                    if (log.isDebugEnabled()) {
                        log.debug(I18nMessageResolver.resolveKey("askdata.log.llm.retry_failed"), attempt + 1, delay, e.toString());
                    }
                    continue;
                }
                break;
            }
        }
        throw new LlmResilienceException(I18nMessageResolver.resolveKey("askdata.llm.retry_exhausted",
                last == null ? "unknown" : last.getMessage()), last);
    }

    private long backoff(int attempt) {
        long delay = config.getBaseDelayMs() << attempt;
        return Math.min(delay, config.getMaxDelayMs());
    }

    private boolean allowRequest() {
        synchronized (lock) {
            if (state.breaker == Breaker.CLOSED || state.breaker == Breaker.HALF_OPEN) {
                return true;
            }
            if (System.currentTimeMillis() - state.openedAt >= config.getCooldownMs()) {
                state = new State(Breaker.HALF_OPEN, 0, 0L);
                if (log.isDebugEnabled()) {
                    log.debug(I18nMessageResolver.resolveKey("askdata.log.llm.breaker_half_open"));
                }
                return true;
            }
            return false;
        }
    }

    private void onSuccess() {
        synchronized (lock) {
            if (state.breaker != Breaker.CLOSED) {
                if (log.isDebugEnabled()) {
                    log.debug(I18nMessageResolver.resolveKey("askdata.log.llm.probe_success"));
                }
            }
            state = new State(Breaker.CLOSED, 0, 0L);
        }
    }

    private void onFailure(Exception e, boolean retryable) {
        synchronized (lock) {
            if (state.breaker == Breaker.HALF_OPEN) {
                state = new State(Breaker.OPEN, 0, System.currentTimeMillis());
                if (log.isDebugEnabled()) {
                    log.debug(I18nMessageResolver.resolveKey("askdata.log.llm.probe_failed"), e.toString());
                }
                return;
            }
            if (state.breaker == Breaker.CLOSED) {
                boolean count = retryable || config.isCountNonRetryableAsFailure();
                if (!count) {
                    return;
                }
                int n = state.consecutiveFailures + 1;
                if (n >= config.getFailureThreshold()) {
                    state = new State(Breaker.OPEN, n, System.currentTimeMillis());
                    if (log.isDebugEnabled()) {
                        log.debug(I18nMessageResolver.resolveKey("askdata.log.llm.breaker_open"), n);
                    }
                } else {
                    state = new State(Breaker.CLOSED, n, 0L);
                }
            }
        }
    }
}
