package com.xiongx9527.fw.plus.ai.base.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 响应就绪事件的上下文：完整响应 + 可在监听器之间传递的属性。
 * 对齐 LangChain4j 1.x 的 {@code ChatModelResponseContext}。
 */
public final class LlmResponseContext {

    private final LlmResponse response;

    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

    public LlmResponseContext(LlmResponse response) {
        if (response == null) {
            throw new IllegalArgumentException("response is required");
        }
        this.response = response;
    }

    public LlmResponse response() {
        return response;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }
}
