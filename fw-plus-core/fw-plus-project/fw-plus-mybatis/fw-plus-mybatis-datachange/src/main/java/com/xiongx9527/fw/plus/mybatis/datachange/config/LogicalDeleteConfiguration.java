package com.xiongx9527.fw.plus.mybatis.datachange.config;

public class LogicalDeleteConfiguration {
	private String column;
	private Object deletedValue;
	private Object notDeletedValue;

	public String getColumn() { return column; }
	public void setColumn(String column) { this.column = column; }
	public Object getDeletedValue() { return deletedValue; }
	public void setDeletedValue(Object deletedValue) { this.deletedValue = deletedValue; }
	public Object getNotDeletedValue() { return notDeletedValue; }
	public void setNotDeletedValue(Object notDeletedValue) { this.notDeletedValue = notDeletedValue; }
}
