package com.xiongx9527.fw.plus.mybatis.datachange.api;

public final class DataChangeFailure {
	private final DataChangeEvent event;
	private final DataChangeException exception;

	public DataChangeFailure(DataChangeEvent event, DataChangeException exception) {
		this.event = event;
		this.exception = exception;
	}

	public DataChangeEvent getEvent() { return event; }
	public DataChangeException getException() { return exception; }
	public DataChangeErrorCode getCode() { return exception == null ? DataChangeErrorCode.UNKNOWN : exception.getErrorCode(); }
}
