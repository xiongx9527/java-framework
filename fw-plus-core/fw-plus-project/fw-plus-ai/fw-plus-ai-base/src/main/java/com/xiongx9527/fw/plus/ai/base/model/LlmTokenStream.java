package com.xiongx9527.fw.plus.ai.base.model;

import com.xiongx9527.fw.plus.ai.base.intent.IntentEvent;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalEvent;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionResult;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** 可配置回调并显式启动的一次 LLM 流式调用。 */
public final class LlmTokenStream {

    /** 启动实际调用，并将流事件交给提供的处理器。 */
    public interface Starter {
        void start(LlmStreamHandler handler);
    }

    private final Starter starter;
    private final List<LlmStreamHandler> handlers = new ArrayList<LlmStreamHandler>();
    private final List<Consumer<LlmResponse>> completeResponseHandlers = new ArrayList<Consumer<LlmResponse>>();
    private final AtomicBoolean started = new AtomicBoolean();

    private final AtomicBoolean cancelled = new AtomicBoolean();

    private LlmTokenStream(Starter starter) {
        if (starter == null) {
            throw new IllegalArgumentException("stream starter is required");
        }
        this.starter = starter;
    }

    public static LlmTokenStream create(Starter starter) {
        return new LlmTokenStream(starter);
    }

    /** 注册正文和思考过程增量回调。 */
    public synchronized LlmTokenStream onToken(BiConsumer<String, String> callback) {
        ensureNotStarted();
        if (callback != null) {
            handlers.add(new HandlerAdapter() {
                @Override
                public void onToken(String content, String reasoningContent) {
                    callback.accept(content, reasoningContent);
                }
            });
        }
        return this;
    }

    /** 注册正文增量回调。 */
    public synchronized LlmTokenStream onPartialResponse(Consumer<String> callback) {
        ensureNotStarted();
        if (callback == null) {
            return this;
        }
        return onToken((content, reasoning) -> callback.accept(content));
    }

    /** 注册思考过程增量回调。 */
    public synchronized LlmTokenStream onPartialThinking(Consumer<String> callback) {
        ensureNotStarted();
        if (callback == null) {
            return this;
        }
        return onToken((content, reasoning) -> callback.accept(reasoning));
    }

    public synchronized LlmTokenStream onComplete(Runnable callback) {
        ensureNotStarted();
        if (callback != null) {
            handlers.add(new HandlerAdapter() {
                @Override
                public void onComplete() {
                    callback.run();
                }
            });
        }
        return this;
    }

    public synchronized LlmTokenStream onError(Consumer<Throwable> callback) {
        ensureNotStarted();
        if (callback != null) {
            handlers.add(new HandlerAdapter() {
                @Override
                public void onError(String message) {
                    callback.accept(new LlmException(message));
                }
            });
        }
        return this;
    }

    /** 注册最终聚合响应回调，包含正文、思考过程、用量和工具调用。 */
    public synchronized LlmTokenStream onCompleteResponse(Consumer<LlmResponse> callback) {
        ensureNotStarted();
        if (callback != null) {
            completeResponseHandlers.add(callback);
        }
        return this;
    }

    /** 注册检索完成事件。 */
    public synchronized LlmTokenStream onRetrieved(Consumer<RetrievalEvent> callback) {
        ensureNotStarted();
        if (callback != null) {
            handlers.add(new HandlerAdapter() {
                @Override
                public void onRetrieval(RetrievalEvent event) {
                    callback.accept(event);
                }
            });
        }
        return this;
    }

    /** 注册工具执行完成事件。 */
    public synchronized LlmTokenStream onToolExecuted(Consumer<ToolExecutionResult> callback) {
        ensureNotStarted();
        if (callback != null) {
            handlers.add(new HandlerAdapter() {
                @Override
                public void onToolResponse(ToolExecutionResult result) {
                    callback.accept(result);
                }
            });
        }
        return this;
    }

    /** 注册完整事件处理器，适用于工具、检索、意图、上下文和用量等事件。 */
    public synchronized LlmTokenStream handler(LlmStreamHandler handler) {
        ensureNotStarted();
        if (handler != null) {
            handlers.add(handler);
        }
        return this;
    }

    /** 启动流。每个流对象只能启动一次。 */
    public void start() {
        if (!started.compareAndSet(false, true)) {
            throw new IllegalStateException("token stream has already been started");
        }
        final List<LlmStreamHandler> registered;
        final List<Consumer<LlmResponse>> responseHandlers;
        synchronized (this) {
            registered = new ArrayList<LlmStreamHandler>(handlers);
            responseHandlers = new ArrayList<Consumer<LlmResponse>>(completeResponseHandlers);
        }
        CompositeHandler composite = new CompositeHandler(registered, responseHandlers);
        CancellationGuard guard = new CancellationGuard(composite, cancelled);
        try {
            starter.start(guard);
        } catch (RuntimeException e) {
            guard.onError(errorMessage(e));
        }
    }

    /**
     * 取消流：调用后所有回调（包括已排队但尚未派发的事件）都不再向下游派发。
     *
     * <p>对齐 LangChain4j 1.x {@code StreamingHandle} 的语义；线程安全，可在流式 IO 线程之外调用。
     * 取消不改变上游请求本身，adapter 如需中断 IO 应自行覆写。</p>
     */
    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    private synchronized void ensureNotStarted() {
        if (started.get()) {
            throw new IllegalStateException("callbacks cannot be registered after token stream start");
        }
    }

    private static String errorMessage(Throwable error) {
        return error.getMessage() == null || error.getMessage().isEmpty()
                ? error.getClass().getSimpleName() : error.getMessage();
    }

    /** 取消守卫：cancel 之后不再派发任何事件。 */
    private static final class CancellationGuard implements LlmStreamHandler {

        private final LlmStreamHandler delegate;

        private final AtomicBoolean cancelled;

        private CancellationGuard(LlmStreamHandler delegate, AtomicBoolean cancelled) {
            this.delegate = delegate;
            this.cancelled = cancelled;
        }

        @Override
        public void onToken(String content, String reasoningContent) {
            if (!cancelled.get()) {
                delegate.onToken(content, reasoningContent);
            }
        }

        @Override
        public void onComplete() {
            if (!cancelled.get()) {
                delegate.onComplete();
            }
        }

        @Override
        public void onError(String message) {
            if (!cancelled.get()) {
                delegate.onError(message);
            }
        }

        @Override
        public void onToolCall(ToolExecutionRequest call) {
            if (!cancelled.get()) {
                delegate.onToolCall(call);
            }
        }

        @Override
        public void onPartialToolCall(String id, String name, String argumentsDelta) {
            if (!cancelled.get()) {
                delegate.onPartialToolCall(id, name, argumentsDelta);
            }
        }

        @Override
        public void onCompleteToolCall(ToolExecutionRequest call) {
            if (!cancelled.get()) {
                delegate.onCompleteToolCall(call);
            }
        }

        @Override
        public void onToolResponse(ToolExecutionResult result) {
            if (!cancelled.get()) {
                delegate.onToolResponse(result);
            }
        }

        @Override
        public void onIntent(IntentEvent event) {
            if (!cancelled.get()) {
                delegate.onIntent(event);
            }
        }

        @Override
        public void onRetrieval(RetrievalEvent event) {
            if (!cancelled.get()) {
                delegate.onRetrieval(event);
            }
        }

        @Override
        public void onContextInject(ContextInjectEvent event) {
            if (!cancelled.get()) {
                delegate.onContextInject(event);
            }
        }

        @Override
        public void onUsage(long totalTokens) {
            if (!cancelled.get()) {
                delegate.onUsage(totalTokens);
            }
        }

        @Override
        public void onUsageDetail(long inputTokens, long outputTokens, long totalTokens) {
            if (!cancelled.get()) {
                delegate.onUsageDetail(inputTokens, outputTokens, totalTokens);
            }
        }

        @Override
        public void onUnmappedRawEvent(Object rawEvent) {
            if (!cancelled.get()) {
                delegate.onUnmappedRawEvent(rawEvent);
            }
        }

        @Override
        public void onToken(String content, String reasoningContent, StreamEventContext context) {
            if (!cancelled.get()) {
                delegate.onToken(content, reasoningContent, context);
            }
        }

        @Override
        public void onPartialToolCall(String id, String name, String argumentsDelta, StreamEventContext context) {
            if (!cancelled.get()) {
                delegate.onPartialToolCall(id, name, argumentsDelta, context);
            }
        }

        @Override
        public void onCompleteToolCall(ToolExecutionRequest call, StreamEventContext context) {
            if (!cancelled.get()) {
                delegate.onCompleteToolCall(call, context);
            }
        }

        @Override
        public void onToolCall(ToolExecutionRequest call, StreamEventContext context) {
            if (!cancelled.get()) {
                delegate.onToolCall(call, context);
            }
        }

        @Override
        public void onToolResponse(ToolExecutionResult result, StreamEventContext context) {
            if (!cancelled.get()) {
                delegate.onToolResponse(result, context);
            }
        }
    }

    private abstract static class HandlerAdapter implements LlmStreamHandler {
        @Override public void onToken(String content, String reasoningContent) { }
        @Override public void onComplete() { }
        @Override public void onError(String message) { }
    }

    private static final class CompositeHandler implements LlmStreamHandler {
        private final List<LlmStreamHandler> delegates;
        private final List<Consumer<LlmResponse>> responseHandlers;
        private final StringBuilder content = new StringBuilder();
        private final StringBuilder reasoning = new StringBuilder();
        private final List<ToolExecutionRequest> toolCalls = new ArrayList<ToolExecutionRequest>();
        private LlmUsage usage = LlmUsage.ZERO;

        private CompositeHandler(List<LlmStreamHandler> delegates,
                                 List<Consumer<LlmResponse>> responseHandlers) {
            this.delegates = delegates;
            this.responseHandlers = responseHandlers;
        }

        @Override public void onToken(String content, String reasoningContent) {
            synchronized (this) {
                if (content != null) this.content.append(content);
                if (reasoningContent != null) this.reasoning.append(reasoningContent);
            }
            for (LlmStreamHandler delegate : delegates) delegate.onToken(content, reasoningContent);
        }
        @Override public void onComplete() {
            LlmResponse response;
            synchronized (this) {
                response = LlmResponse.builder()
                        .content(content.length() == 0 ? null : content.toString())
                        .reasoningContent(reasoning.length() == 0 ? null : reasoning.toString())
                        .usage(usage)
                        .toolCalls(toolCalls)
                        .build();
            }
            for (Consumer<LlmResponse> callback : responseHandlers) callback.accept(response);
            for (LlmStreamHandler delegate : delegates) delegate.onComplete();
        }
        @Override public void onError(String message) {
            for (LlmStreamHandler delegate : delegates) delegate.onError(message);
        }
        @Override public void onToolCall(ToolExecutionRequest call) {
            if (call != null) {
                synchronized (this) {
                    toolCalls.add(call);
                }
            }
            for (LlmStreamHandler delegate : delegates) delegate.onToolCall(call);
        }
        @Override public void onPartialToolCall(String id, String name, String argumentsDelta) {
            for (LlmStreamHandler delegate : delegates) {
                delegate.onPartialToolCall(id, name, argumentsDelta);
            }
        }
        @Override public void onCompleteToolCall(ToolExecutionRequest call) {
            for (LlmStreamHandler delegate : delegates) delegate.onCompleteToolCall(call);
        }
        @Override public void onToolResponse(ToolExecutionResult result) {
            for (LlmStreamHandler delegate : delegates) delegate.onToolResponse(result);
        }
        @Override public void onIntent(IntentEvent event) {
            for (LlmStreamHandler delegate : delegates) delegate.onIntent(event);
        }
        @Override public void onRetrieval(RetrievalEvent event) {
            for (LlmStreamHandler delegate : delegates) delegate.onRetrieval(event);
        }
        @Override public void onContextInject(ContextInjectEvent event) {
            for (LlmStreamHandler delegate : delegates) delegate.onContextInject(event);
        }
        @Override public void onUsage(long totalTokens) {
            synchronized (this) {
                usage = new LlmUsage(0L, 0L, totalTokens);
            }
            for (LlmStreamHandler delegate : delegates) delegate.onUsage(totalTokens);
        }
        @Override public void onUsageDetail(long inputTokens, long outputTokens, long totalTokens) {
            synchronized (this) {
                usage = new LlmUsage(inputTokens, outputTokens, totalTokens);
            }
            for (LlmStreamHandler delegate : delegates) {
                delegate.onUsageDetail(inputTokens, outputTokens, totalTokens);
            }
        }
        @Override public void onUnmappedRawEvent(Object rawEvent) {
            for (LlmStreamHandler delegate : delegates) delegate.onUnmappedRawEvent(rawEvent);
        }
        @Override public void onToken(String content, String reasoningContent, StreamEventContext context) {
            for (LlmStreamHandler delegate : delegates) delegate.onToken(content, reasoningContent, context);
        }
        @Override public void onPartialToolCall(String id, String name, String argumentsDelta, StreamEventContext context) {
            for (LlmStreamHandler delegate : delegates) delegate.onPartialToolCall(id, name, argumentsDelta, context);
        }
        @Override public void onCompleteToolCall(ToolExecutionRequest call, StreamEventContext context) {
            for (LlmStreamHandler delegate : delegates) delegate.onCompleteToolCall(call, context);
        }
        @Override public void onToolCall(ToolExecutionRequest call, StreamEventContext context) {
            for (LlmStreamHandler delegate : delegates) delegate.onToolCall(call, context);
        }
        @Override public void onToolResponse(ToolExecutionResult result, StreamEventContext context) {
            for (LlmStreamHandler delegate : delegates) delegate.onToolResponse(result, context);
        }
    }
}
