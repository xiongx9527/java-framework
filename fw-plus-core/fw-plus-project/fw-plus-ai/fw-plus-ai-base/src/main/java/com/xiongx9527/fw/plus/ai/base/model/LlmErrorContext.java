package com.xiongx9527.fw.plus.ai.base.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 失败事件的上下文：可直接展示的原因 + 原始异常 + 跨监听器属性。
 * 对齐 LangChain4j 1.x 的 {@code ChatModelErrorContext}。
 */
public final class LlmErrorContext {

    private final String message;

    private final Throwable cause;

    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();

    public LlmErrorContext(String message, Throwable cause) {
        this.message = message == null ? "unknown error" : message;
        this.cause = cause;
    }

    public String message() {
        return message;
    }

    public Throwable cause() {
        return cause;
    }

    public Map<String, Object> attributes() {
        return attributes;
    }
}
