package com.xiongx9527.fw.plus.ai.base.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 把端口级 {@link LlmListener} 绑定到任意 {@link LlmPort} 的轻量视图。
 *
 * <p>由 {@link LlmPort#withListeners(List)} 创建。流式通道通过注册一个转发 handler 派发，
 * 非流式通道在 {@code complete} 前后派发；异常一律吞掉，不影响主链路。
 * 这样业务侧不必再手动包 {@code ListeningLlmPort} 装饰器。</p>
 */
public final class ListenerBoundLlmPort implements LlmPort {

    private final LlmPort delegate;

    private final List<LlmListener> listeners;

    public ListenerBoundLlmPort(LlmPort delegate, List<LlmListener> listeners) {
        if (delegate == null) {
            throw new IllegalArgumentException("llm port is required");
        }
        this.delegate = delegate;
        this.listeners = Collections.unmodifiableList(listeners == null
                ? new ArrayList<LlmListener>() : new ArrayList<LlmListener>(listeners));
    }

    @Override
    public LlmCapabilities capabilities() {
        return delegate.capabilities();
    }

    @Override
    public List<LlmListener> listeners() {
        return listeners;
    }

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
        return delegate.stream(request).handler(new Forwarder());
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        fireRequest(request);
        try {
            LlmResponse response = delegate.complete(request);
            fireResponse(response);
            fireComplete();
            return response;
        } catch (RuntimeException e) {
            fireError(e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage(), e);
            throw e;
        }
    }

    private void fireRequest(LlmRequest request) {
        LlmRequestContext context = new LlmRequestContext(request);
        for (LlmListener listener : listeners) {
            try {
                listener.onRequest(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void fireResponse(LlmResponse response) {
        LlmResponseContext context = new LlmResponseContext(response);
        for (LlmListener listener : listeners) {
            try {
                listener.onResponse(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void fireComplete() {
        for (LlmListener listener : listeners) {
            try {
                listener.onComplete();
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void fireError(String message, Throwable cause) {
        LlmErrorContext context = new LlmErrorContext(message, cause);
        for (LlmListener listener : listeners) {
            try {
                listener.onError(context);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private final class Forwarder implements LlmStreamHandler {

        @Override
        public void onToken(String content, String reasoningContent) {
            for (LlmListener listener : listeners) {
                try {
                    listener.onToken(content, reasoningContent);
                } catch (RuntimeException ignored) {
                    // 监听器异常不得影响主链路
                }
            }
        }

        @Override
        public void onUsageDetail(long inputTokens, long outputTokens, long totalTokens) {
            for (LlmListener listener : listeners) {
                try {
                    listener.onUsage(new LlmUsage(inputTokens, outputTokens, totalTokens));
                } catch (RuntimeException ignored) {
                    // 监听器异常不得影响主链路
                }
            }
        }

        @Override
        public void onComplete() {
            fireComplete();
        }

        @Override
        public void onError(String message) {
            fireError(message, null);
        }
    }
}
