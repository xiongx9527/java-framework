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
     * Constructor with internationalized message args.
     *
     * <p>文案取自资源文件 {@code i18n/messages} 中 key 为 {@link #CODE} 的条目。</p>
     *
     * @param i18nArgs 文案占位参数
     */
    protected BaseRuntimeException(Object[] i18nArgs) {
        super(CODE, i18nArgs);
    }

    /**
     * 构造国际化运行时异常：文案由资源文件按 {@link #CODE} 解析。
     *
     * @param i18nArgs 文案占位参数
     */
    public static BaseRuntimeException i18n(Object... i18nArgs) {
        // 包了一层 Object[] 以避开与 (String) 构造器的歧义
        return new BaseRuntimeException(i18nArgs);
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
