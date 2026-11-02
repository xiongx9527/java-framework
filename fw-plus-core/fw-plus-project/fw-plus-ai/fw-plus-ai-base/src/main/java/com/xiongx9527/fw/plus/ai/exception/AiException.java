package com.xiongx9527.fw.plus.ai.exception;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * AI 层异常基类（非配置类）。
 *
 * <p><b>作用</b>：统一承载「AI 能力运行时」的异常（解析失败、模型不可用、内容被拒等），
 * 使上层可以按基类捕获而不必依赖具体框架的异常体系。配置类错误走
 * {@link IllegalConfigurationException}（同样继承框架 {@link BizException}）。</p>
 *
 * <p><b>继承关系</b>：直接继承框架统一 {@link BizException}（8 位状态码异常体系），
 * 因此所有 AI 层异常（含配置类）最终都归属 {@code BizException}，可被全局异常处理器统一拦截。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像 {@code dev.langchain4j.exception.LangChain4jException}
 * （其下还有 {@code InvalidRequestException} / {@code TimeoutException} / {@code RateLimitException} 等子类，
 * 属批次③补齐范围）。</p>
 *
 * @author xiongxiao
 */
public class AiException extends BizException {

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
