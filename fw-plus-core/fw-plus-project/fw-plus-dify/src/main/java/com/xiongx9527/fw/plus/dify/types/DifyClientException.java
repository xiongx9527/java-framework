package com.xiongx9527.fw.plus.dify.types;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class DifyClientException extends BizException {

    /** 状态码 50008002（SERVER/THIRD_PARTY/002 Dify HTTP 客户端调用失败） */
    public static final int CODE = 50_008_002;

    /** 登录失败（文案由资源文件按状态码提供） */
    public static final int LOGIN_FAILED = 50_008_101;

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

    /**
     * 以「具体错误码 + 参数」构造，文案在读取时按当前请求语言解析。
     *
     * @param messageCode 具体错误码（如 {@link #LOGIN_FAILED}）
     * @param args        文案占位参数
     */
    private DifyClientException(int messageCode, Object[] args) {
        super(messageCode, args);
    }

    /**
     * 国际化构造入口。
     *
     * @param messageCode 具体错误码
     * @param args        文案占位参数
     */
    public static DifyClientException of(int messageCode, Object... args) {
        return new DifyClientException(messageCode, args);
    }

}
