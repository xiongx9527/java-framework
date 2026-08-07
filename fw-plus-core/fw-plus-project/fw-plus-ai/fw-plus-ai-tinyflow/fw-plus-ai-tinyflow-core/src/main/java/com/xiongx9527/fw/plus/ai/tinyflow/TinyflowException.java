package com.xiongx9527.fw.plus.ai.tinyflow;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class TinyflowException extends BizException {

	private static final long serialVersionUID = 1L;

	public TinyflowException(String message) {
		super(message);
	}

	public TinyflowException(String message, Throwable cause) {
		super(message, cause);
	}
}
