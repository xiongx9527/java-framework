package com.xiongx9527.fw.plus.mybatis.datachange.api;

public final class FieldChange {
	private final String columnName;
	private final String propertyName;
	private final Object beforeValue;
	private final Object afterValue;

	public FieldChange(String columnName, String propertyName, Object beforeValue, Object afterValue) {
		this.columnName = columnName;
		this.propertyName = propertyName;
		this.beforeValue = beforeValue;
		this.afterValue = afterValue;
	}

	public String getColumnName() { return columnName; }
	public String getPropertyName() { return propertyName; }
	public Object getBeforeValue() { return beforeValue; }
	public Object getAfterValue() { return afterValue; }
}
