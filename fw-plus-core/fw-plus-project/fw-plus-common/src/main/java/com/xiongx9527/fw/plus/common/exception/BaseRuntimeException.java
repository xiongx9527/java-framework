package com.xiongx9527.fw.plus.common.exception;

public class BaseRuntimeException extends BizException {

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = -27048199131316992L;

    /** 状态码 50010001（SERVER/INFRASTRUCTURE/001 公共运行时异常） */
    public static final int CODE = 50_010_001;

    /**
     * Default constructor
     */
    public BaseRuntimeException() {
        super(CODE, (String) null);
    }

    /**
     * Constructor with message & cause
     *
     * @param message
     * @param cause
     */
    public BaseRuntimeException(String message, Throwable cause) {
        super(CODE, message, cause);
    }

    /**
     * Constructor with message
     *
     * @param message
     */
    public BaseRuntimeException(String message) {
        super(CODE, message);
    }

    /**
     * Constructor with message format
     *
     * @param msgFormat
     * @param args
     */
    public BaseRuntimeException(String msgFormat, Object... args) {
        super(CODE, String.format(msgFormat, args));
    }

    /**
     * Constructor with cause
     *
     * @param cause
     */
    public BaseRuntimeException(Throwable cause) {
        super(CODE, cause);
    }

}
