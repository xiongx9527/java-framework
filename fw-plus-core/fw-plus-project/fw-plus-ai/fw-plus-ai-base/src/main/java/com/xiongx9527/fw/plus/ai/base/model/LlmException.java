package com.xiongx9527.fw.plus.ai.base.model;

/**
 * 大语言调用失败（模型未配置、上游错误、网络异常等）。
 */
public class LlmException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
