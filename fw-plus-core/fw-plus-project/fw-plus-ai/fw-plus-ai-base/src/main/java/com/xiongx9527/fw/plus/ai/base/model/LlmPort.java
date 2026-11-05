package com.xiongx9527.fw.plus.ai.base.model;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

/**
 * 大语言模型端口。
 *
 * <p><b>流式与非流式是同一端口的两条通道</b>：</p>
 * <ul>
 *   <li>{@link #stream(LlmRequest)} —— 返回 {@link LlmTokenStream}，由调用方注册回调并 {@code start()}</li>
 *   <li>{@link #complete(LlmRequest)} —— 一次性返回，默认由流式通道聚合而来</li>
 * </ul>
 *
 * <p>两条通道都会向 {@link LlmRequest#getListeners()} 派发同一组事件
 * （{@link LlmListener}），所以日志 / 计费 / 追踪只需实现一次。推荐 adapter 继承
 * {@link AbstractLlmPort}：它把事件派发统一收口，adapter 只实现「怎么发一次上游请求」。</p>
 *
 * <p>失败语义：{@link #stream} 不抛异常，错误通过 {@link LlmStreamHandler#onError(String)}
 * 与 {@link LlmListener#onError(String)} 交付（流式回调发生在 IO 线程上，抛异常会丢上下文）；
 * {@link #complete} 在聚合到错误时抛 {@link LlmException}，便于上层统一 try/catch 与重试。</p>
 */
public interface LlmPort {

    /** 返回该端口可稳定提供的协议能力。 */
    default LlmCapabilities capabilities() {
        return LlmCapabilities.basic();
    }

    /** 端口级监听器；业务侧可覆写，无需再手动包 {@code ListeningLlmPort} 装饰器。 */
    default List<LlmListener> listeners() {
        return Collections.emptyList();
    }

    /** 返回一个绑定了端口级监听器的视图（本实例不变）。 */
    default LlmPort withListeners(List<LlmListener> listeners) {
        return new ListenerBoundLlmPort(this, listeners);
    }

    /** 异步补全；默认在公共线程池执行 {@link #complete(LlmRequest)}，adapter 可覆写为原生异步。 */
    default CompletableFuture<LlmResponse> completeAsync(LlmRequest request) {
        return CompletableFuture.supplyAsync(() -> complete(request));
    }

    /**
     * 能力协商：请求要求了本端口声明不具备的能力时显式拒绝，避免静默降级。
     *
     * <p><b>框架不自动强制</b>：多数既有 adapter 未覆写 {@link #capabilities()}（默认
     * {@link LlmCapabilities#basic()}）却实际支持工具调用，自动拦截会误伤。本方法供业务侧
     * 在调用前显式预检；需要硬约束的 adapter 可覆写 {@code stream/complete} 并调用它。</p>
     */
    default LlmRequest negotiate(LlmRequest request) {
        if (request == null) {
            return null;
        }
        LlmCapabilities caps = capabilities();
        if (request.hasTools() && !caps.isToolCalling()) {
            throw new UnsupportedOperationException("tool calling is not supported by this llm port");
        }
        if (request.getResponseFormat() != null && !caps.isStructuredOutput()) {
            throw new UnsupportedOperationException("structured output is not supported by this llm port");
        }
        return request;
    }

    /**
     * 流式补全：返回 {@link LlmTokenStream}，调用方注册回调后调用 {@code start()} 启动；
     * 事件以 {@code onComplete} 或 {@code onError} 收尾。
     */
    LlmTokenStream stream(LlmRequest request);

    /**
     * 非流式补全：聚合流式通道的正文、思考过程与用量，一次性返回。
     *
     * @throws LlmException 流式通道回调了 {@code onError}
     */
    default LlmResponse complete(LlmRequest request) {
        if (request == null) {
            throw new LlmException("llm request is required");
        }
        final StringBuilder content = new StringBuilder();
        final StringBuilder reasoning = new StringBuilder();
        final LlmUsage[] usage = {LlmUsage.ZERO};
        final String[] error = {null};
        final CountDownLatch completed = new CountDownLatch(1);
        stream(request).handler(new LlmStreamHandler() {
            @Override
            public void onToken(String delta, String reasoningDelta) {
                if (delta != null) {
                    content.append(delta);
                }
                if (reasoningDelta != null) {
                    reasoning.append(reasoningDelta);
                }
            }

            @Override
            public void onComplete() {
                completed.countDown();
            }

            @Override
            public void onError(String message) {
                error[0] = message;
                completed.countDown();
            }

            @Override
            public void onUsageDetail(long inputTokens, long outputTokens, long totalTokens) {
                usage[0] = new LlmUsage(inputTokens, outputTokens, totalTokens);
            }
        }).start();
        awaitCompletion(completed);
        if (error[0] != null) {
            throw new LlmException(error[0]);
        }
        return LlmResponse.builder()
                .content(content.length() == 0 ? null : content.toString())
                .reasoningContent(reasoning.length() == 0 ? null : reasoning.toString())
                .usage(usage[0])
                .build();
    }

    static void awaitCompletion(CountDownLatch completed) {
        try {
            completed.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LlmException("interrupted while waiting for llm response", e);
        }
    }

    /** 单轮 system + user 的非流式补全快捷方式。 */
    default LlmResponse complete(String systemPrompt, String userPrompt) {
        return complete(LlmRequest.systemUser(systemPrompt, userPrompt));
    }

    /**
     * 枚举该后端可用的对话模型，供配置页选型与启动自检使用。
     *
     * <p>不支持枚举的后端返回空列表（不抛异常）；列表项的 {@link ModelInfo#getType()} 为
     * {@link ModelType#LLM}。支持 {@code /models} 类端点的 adapter 覆写本方法。</p>
     */
    default List<ModelInfo> listModels() {
        return ModelCatalogSupport.empty();
    }

    /**
     * 校验给定模型是否可用：目录能枚举时按目录判定；目录为空时用一次 {@code max_tokens=1}
     * 的真实补全探测，这样地址、鉴权、模型名三者同时得到验证。
     *
     * <p>校验失败通过 {@link ModelValidation#isValid()} 表达，不抛异常；网络不可达等异常也收敛
     * 为失败结果并给出可展示原因。</p>
     */
    default ModelValidation validateModel(String model) {
        if (model == null || model.trim().isEmpty()) {
            return ModelValidation.invalid(model, "model id is required", 0L);
        }
        List<ModelInfo> catalog = listModels();
        if (!catalog.isEmpty()) {
            return ModelCatalogSupport.validate(ModelType.LLM, catalog, model, 0L);
        }
        long start = System.currentTimeMillis();
        try {
            complete(LlmRequest.builder()
                    .model(model)
                    .maxTokens(1)
                    .addMessage(ChatMessage.user("ping"))
                    .build());
            return ModelValidation.valid(model, System.currentTimeMillis() - start);
        } catch (RuntimeException e) {
            String message = e.getMessage() == null ? "model probe failed" : e.getMessage();
            return ModelValidation.invalid(model, message, System.currentTimeMillis() - start);
        }
    }
}
