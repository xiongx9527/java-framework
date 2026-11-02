package com.xiongx9527.fw.plus.ai.exception;

/**
 * AI 层异常基类（非配置类）。
 *
 * <p><b>作用</b>：统一承载「AI 能力运行时」的异常（解析失败、模型不可用、内容被拒等），
 * 使上层可以按基类捕获而不必依赖具体框架的异常体系。配置类错误走
 * {@link IllegalConfigurationException}（已接入框架 {@code BizException}）。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.exception.LangChain4jException}
 * （其下还有 {@code InvalidRequestException} / {@code TimeoutException} / {@code RateLimitException} 等子类，
 * 属批次③补齐范围）。</p>
 *
 * @author xiongxiao
 */
public class AiException extends RuntimeException {

    /**
     * 构造异常。
     *
     * @param message 消息
     */
    public AiException(String message) {
        super(message);
    }

    /**
     * 构造异常。
     *
     * @param message 消息
     * @param cause   原因
     */
    public AiException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 构造异常。
     *
     * @param cause 原因
     */
    public AiException(Throwable cause) {
        super(cause);
    }
}
