package com.xiongx9527.fw.plus.ai.base.context;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 统一的 AI 调用失败上下文：可直接展示的原因、原始异常、是否可重试、以及排障属性。
 *
 * <p>各能力的异常（{@code LlmException} / {@code ModerationException} /
 * {@code ToolExecutionException} / {@code GuardrailException}）都继承
 * {@code BizException}，但携带的信息分散；本类在跨能力边界（重试、降级、上报）时
 * 提供统一视图。</p>
 */
public final class AiErrorContext {

    private final String message;

    private final Throwable cause;

    private final boolean retryable;

    private final Map<String, Object> attributes;

    public AiErrorContext(String message, Throwable cause, boolean retryable,
                           Map<String, Object> attributes) {
        this.message = message == null ? "unknown error" : message;
        this.cause = cause;
        this.retryable = retryable;
        this.attributes = attributes == null ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(attributes);
    }

    public static AiErrorContext of(Throwable error) {
        return of(error, false);
    }

    public static AiErrorContext of(Throwable error, boolean retryable) {
        String message = error == null ? "unknown error"
                : error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage();
        return new AiErrorContext(message, error, retryable, null);
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }

    public boolean isRetryable() {
        return retryable;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public AiErrorContext attribute(String key, Object value) {
        if (key != null) {
            this.attributes.put(key, value);
        }
        return this;
    }
}
