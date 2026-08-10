package com.xiongx9527.fw.plus.ai.tinyflow;

public class TinyflowException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public TinyflowException(String message) {
		super(message);
	}

	public TinyflowException(String message, Throwable cause) {
		super(message, cause);
	}
}
