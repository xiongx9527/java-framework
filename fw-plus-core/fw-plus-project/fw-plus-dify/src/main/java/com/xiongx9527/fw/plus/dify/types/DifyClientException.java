package com.xiongx9527.fw.plus.dify.types;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class DifyClientException extends BizException {

    /** 状态码 50008002（SERVER/THIRD_PARTY/002 Dify HTTP 客户端调用失败） */
    public static final int CODE = 50_008_002;

    /**
     * Constructs a new runtime exception with {@code null} as its
     */
    public DifyClientException() {
        super(CODE, (String) null);
    }

    /**
     * Constructs a new runtime exception with the specified detail message.
     * @param message String
     */
    public DifyClientException(String message) {
        super(CODE, message);
    }

    /**
     * Constructs a new runtime exception with the specified detail message and  Throwable
     * @param message String
     * @param cause Throwable
     */
    public DifyClientException(String message, Throwable cause) {
        super(CODE, message, cause);
    }

    /**
     * Constructs a new runtime exception with the Throwable
     * @param cause Throwable
     */
    public DifyClientException(Throwable cause) {
        super(CODE, cause);
    }

}
