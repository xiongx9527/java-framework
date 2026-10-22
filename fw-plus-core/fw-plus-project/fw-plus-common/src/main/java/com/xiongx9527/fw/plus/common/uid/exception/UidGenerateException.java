package com.xiongx9527.fw.plus.common.uid.exception;

import com.xiongx9527.fw.plus.common.exception.BizException;

/**
 * UidGenerateException
 * 
 * @author yutianbao
 */
public class UidGenerateException extends BizException {

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = -27048199131316992L;

    /** 状态码 50010002（SERVER/INFRASTRUCTURE/002 分布式 UID 生成失败） */
    public static final int CODE = 50_010_002;

    /**
     * Default constructor
     */
    public UidGenerateException() {
        super(CODE, (String) null);
    }

    /**
     * Constructor with message & cause
     * 
     * @param message
     * @param cause
     */
    public UidGenerateException(String message, Throwable cause) {
        super(CODE, message, cause);
    }

    /**
     * Constructor with message
     * 
     * @param message
     */
    public UidGenerateException(String message) {
        super(CODE, message);
    }
    
    /**
     * Constructor with message format
     * 
     * @param msgFormat
     * @param args
     */
    public UidGenerateException(String msgFormat, Object... args) {
        super(CODE, String.format(msgFormat, args));
    }

    /**
     * Constructor with cause
     * 
     * @param cause
     */
    public UidGenerateException(Throwable cause) {
        super(CODE, cause);
    }

}
