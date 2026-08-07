package com.xiongx9527.fw.plus.mybatis.datachange.api;

public final class FieldChange {
	private final String column;
	private final Object beforeValue;
	private final Object afterValue;

	public FieldChange(String column, Object beforeValue, Object afterValue) {
		this.column = column;
		this.beforeValue = beforeValue;
		this.afterValue = afterValue;
	}

	public String getColumn() { return column; }
	public Object getBeforeValue() { return beforeValue; }
	public Object getAfterValue() { return afterValue; }
}
