package com.xiongx9527.fw.plus.mybatis.datachange.api;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class DataChangeException extends BizException {
	private static final long serialVersionUID = 1L;

	/** 状态码 50019001（SERVER/CDC_STREAM/001 数据变更后置处理失败） */
	public static final int CODE = 50_019_001;

	private final DataChangeErrorCode errorCode;

	public DataChangeException(DataChangeErrorCode errorCode, String message) {
		this(CODE, errorCode, message);
	}

	public DataChangeException(DataChangeErrorCode errorCode, String message, Throwable cause) {
		this(CODE, errorCode, message, cause);
	}

	/** 子类（如 {@code SqlAnalysisException}）自声明状态码时使用 */
	protected DataChangeException(int code, DataChangeErrorCode errorCode, String message) {
		super(code, message);
		this.errorCode = errorCode;
	}

	/** 子类（如 {@code SqlAnalysisException}）自声明状态码时使用 */
	protected DataChangeException(int code, DataChangeErrorCode errorCode, String message, Throwable cause) {
		super(code, message, cause);
		this.errorCode = errorCode;
	}

	/** 数据变更错误码；8 位统一状态码由父类 {@link BizException#getCode()} 提供 */
	public DataChangeErrorCode getErrorCode() { return errorCode; }
}
