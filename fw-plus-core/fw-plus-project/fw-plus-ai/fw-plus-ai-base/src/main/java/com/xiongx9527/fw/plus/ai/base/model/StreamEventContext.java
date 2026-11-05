package com.xiongx9527.fw.plus.ai.base.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 流式事件附带的上下文，对齐 LangChain4j 1.x 各 {@code *Context} 重载。
 *
 * <p>1.x 的上下文重载（如 {@code onPartialResponse(resp, ctx)}）携带一个 {@code StreamingHandle}，
 * 用于在增量回调内取消流。本类的 {@link #stream()} 即该句柄，另附原始 {@link #request()} 与
 * 跨事件可写的 {@link #attributes()}，便于监听器在多次增量之间传递状态（如拼装未映射的原始分片）。</p>
 *
 * <p>非流式调用（{@code complete}）路径下 {@link #stream()} 为 {@code null}；{@link #attributes()}
 * 在整次调用内为同一个 {@link Map} 实例，可安全用于跨事件累积。</p>
 */
public final class StreamEventContext {

    private final LlmTokenStream stream;

    private final LlmRequest request;

    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

    StreamEventContext(LlmTokenStream stream, LlmRequest request) {
        this.stream = stream;
        this.request = request;
    }

    /**
     * 当前流的句柄；回调内可调用 {@link LlmTokenStream#cancel()} 取消。
     * 非流式路径为 {@code null}。
     */
    public LlmTokenStream stream() {
        return stream;
    }

    /** 触发本次调用的原始请求。 */
    public LlmRequest request() {
        return request;
    }

    /** 跨事件可写属性；同一调用内所有事件共享同一 {@link Map} 实例。 */
    public Map<String, Object> attributes() {
        return attributes;
    }
}
