package com.xiongx9527.fw.plus.mybatis.datachange.adapter;

public final class LogicalDeleteMetadata {
	private final String column;
	private final Object deletedValue;
	private final Object notDeletedValue;

	public LogicalDeleteMetadata(String column, Object deletedValue, Object notDeletedValue) {
		this.column = column;
		this.deletedValue = deletedValue;
		this.notDeletedValue = notDeletedValue;
	}

	public String getColumn() { return column; }
	public Object getDeletedValue() { return deletedValue; }
	public Object getNotDeletedValue() { return notDeletedValue; }
}
