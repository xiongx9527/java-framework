package com.xiongx9527.fw.plus.ai.base.model;

import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link LlmPort} 的骨架实现：统一收口事件派发，adapter 只实现
 * {@link #doStream(LlmRequest, LlmStreamHandler)}。
 *
 * <p>两条通道的事件都在这里派发一次且仅一次：</p>
 * <ul>
 *   <li>{@link #stream} —— 事件直接派发给 {@code handler} 与监听器</li>
 *   <li>{@link #complete} —— 由聚合处理器收集，结束时派发响应事件</li>
 * </ul>
 *
 * <p>因此 adapter 不需要（也不应该）自己再派发监听器事件。原生支持非流式的后端，
 * 在 {@code doStream} 里先 {@code onToken(完整内容, null)} 再 {@code onComplete()} 即可，
 * 事件语义不变。</p>
 */
public abstract class AbstractLlmPort implements LlmPort {

    @Override
    public final void stream(LlmRequest request, LlmStreamHandler handler) {
        if (request == null) {
            throw new LlmException("llm request is required");
        }
        LlmStreamHandler target = handler == null ? NullStreamHandler.INSTANCE : handler;
        notifyRequest(request);
        doStream(request, new EventingStreamHandler(request, target));
    }

    @Override
    public final LlmResponse complete(LlmRequest request) {
        if (request == null) {
            throw new LlmException("llm request is required");
        }
        notifyRequest(request);
        AggregatingStreamHandler aggregator = new AggregatingStreamHandler(request);
        doStream(request, aggregator);
        return aggregator.result();
    }

    /** 用一次最小真实补全请求校验模型及当前端点、鉴权配置。 */
    protected final ModelValidation validateModelWithCompletion(String model) {
        if (model == null || model.trim().isEmpty()) {
            return ModelValidation.invalid(model, "model id is required", 0L);
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

    /**
     * 发起一次上游请求并按增量回调 {@code handler}。
     *
     * <p>实现方只负责协议、鉴权与报文解析；事件派发、非流式聚合由本类完成。</p>
     */
    protected abstract void doStream(LlmRequest request, LlmStreamHandler handler);

    private void notifyRequest(LlmRequest request) {
        List<LlmListener> listeners = request.getListeners();
        for (LlmListener listener : listeners) {
            try {
                listener.onRequest(request);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private static void notifyToken(LlmRequest request, String content, String reasoningContent) {
        for (LlmListener listener : request.getListeners()) {
            try {
                listener.onToken(content, reasoningContent);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private static void notifyUsage(LlmRequest request, LlmUsage usage) {
        for (LlmListener listener : request.getListeners()) {
            try {
                listener.onUsage(usage);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private static void notifyComplete(LlmRequest request) {
        for (LlmListener listener : request.getListeners()) {
            try {
                listener.onComplete();
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private static void notifyError(LlmRequest request, String message) {
        for (LlmListener listener : request.getListeners()) {
            try {
                listener.onError(message);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private static void notifyResponse(LlmRequest request, LlmResponse response) {
        if (response == null) {
            return;
        }
        for (LlmListener listener : request.getListeners()) {
            try {
                listener.onResponse(response);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    /** 把增量事件同时转发给调用方 handler 与监听器。 */
    private static final class EventingStreamHandler implements LlmStreamHandler {

        private final LlmRequest request;

        private final LlmStreamHandler delegate;

        private LlmUsage usage = LlmUsage.ZERO;

        private EventingStreamHandler(LlmRequest request, LlmStreamHandler delegate) {
            this.request = request;
            this.delegate = delegate;
        }

        @Override
        public void onToken(String content, String reasoningContent) {
            notifyToken(request, content, reasoningContent);
            delegate.onToken(content, reasoningContent);
        }

        @Override
        public void onComplete() {
            notifyUsage(request, usage);
            notifyComplete(request);
            delegate.onComplete();
        }

        @Override
        public void onError(String message) {
            notifyError(request, message);
            delegate.onError(message);
        }

        @Override
        public void onToolCall(ToolExecutionRequest call) {
            delegate.onToolCall(call);
        }

        @Override
        public void onUsage(long totalTokens) {
            this.usage = new LlmUsage(0L, 0L, totalTokens);
            delegate.onUsage(totalTokens);
        }

        @Override
        public void onUsageDetail(long inputTokens, long outputTokens, long totalTokens) {
            this.usage = new LlmUsage(inputTokens, outputTokens, totalTokens);
            delegate.onUsageDetail(inputTokens, outputTokens, totalTokens);
        }
    }

    /** 非流式聚合：收集正文 / 思考过程 / 用量，同时派发监听器事件。 */
    private static final class AggregatingStreamHandler implements LlmStreamHandler {

        private final LlmRequest request;

        private final StringBuilder content = new StringBuilder();

        private final StringBuilder reasoning = new StringBuilder();

        private final List<ToolExecutionRequest> toolCalls = new ArrayList<ToolExecutionRequest>();

        private LlmUsage usage = LlmUsage.ZERO;

        private String error;

        private AggregatingStreamHandler(LlmRequest request) {
            this.request = request;
        }

        @Override
        public void onToken(String delta, String reasoningDelta) {
            if (delta != null) {
                content.append(delta);
            }
            if (reasoningDelta != null) {
                reasoning.append(reasoningDelta);
            }
            notifyToken(request, delta, reasoningDelta);
        }

        @Override
        public void onComplete() {
            if (error == null) {
                LlmResponse response = LlmResponse.builder()
                        .content(content.length() == 0 ? null : content.toString())
                        .reasoningContent(reasoning.length() == 0 ? null : reasoning.toString())
                        .usage(usage)
                        .toolCalls(toolCalls)
                        .build();
                notifyResponse(request, response);
            }
            notifyUsage(request, usage);
            notifyComplete(request);
        }

        @Override
        public void onError(String message) {
            this.error = message;
            notifyError(request, message);
        }

        @Override
        public void onToolCall(ToolExecutionRequest call) {
            if (call != null) {
                toolCalls.add(call);
            }
        }

        @Override
        public void onUsage(long totalTokens) {
            this.usage = new LlmUsage(0L, 0L, totalTokens);
        }

        @Override
        public void onUsageDetail(long inputTokens, long outputTokens, long totalTokens) {
            this.usage = new LlmUsage(inputTokens, outputTokens, totalTokens);
        }

        private LlmResponse result() {
            if (error != null) {
                throw new LlmException(error);
            }
            return LlmResponse.builder()
                    .content(content.length() == 0 ? null : content.toString())
                    .reasoningContent(reasoning.length() == 0 ? null : reasoning.toString())
                    .usage(usage)
                    .toolCalls(toolCalls)
                    .build();
        }
    }

    /** {@code null} handler 的兜底。 */
    private enum NullStreamHandler implements LlmStreamHandler {

        INSTANCE;

        @Override
        public void onToken(String content, String reasoningContent) {
        }

        @Override
        public void onComplete() {
        }

        @Override
        public void onError(String message) {
        }
    }
}
