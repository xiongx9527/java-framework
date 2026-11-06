package com.xiongx9527.fw.plus.ai.nativeimpl.retrieval;

/**
 * Dify 检索异常：网络错误、HTTP 非 2xx、响应解析失败统一收敛为运行期异常。
 */
public class DifyRetrievalException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DifyRetrievalException(String message) {
        super(message);
    }

    public DifyRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }
}
