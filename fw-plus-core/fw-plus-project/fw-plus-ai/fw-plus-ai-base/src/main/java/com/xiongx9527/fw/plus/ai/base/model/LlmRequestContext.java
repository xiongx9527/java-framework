package com.xiongx9527.fw.plus.ai.base.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 请求开始事件的上下文：完整请求 + 可在监听器之间传递的属性。
 *
 * <p>对齐 LangChain4j 1.x 的 {@code ChatModelRequestContext}；{@link #attributes()} 与
 * 1.23 的 {@code ChatRequestOptions.listenerAttributes()} 用途一致。</p>
 */
public final class LlmRequestContext {

    private final LlmRequest request;

    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

    public LlmRequestContext(LlmRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request is required");
        }
        this.request = request;
    }

    public LlmRequest request() {
        return request;
    }

    /** 可变的跨监听器属性表（同一请求内共享）。 */
    public Map<String, Object> attributes() {
        return attributes;
    }
}
