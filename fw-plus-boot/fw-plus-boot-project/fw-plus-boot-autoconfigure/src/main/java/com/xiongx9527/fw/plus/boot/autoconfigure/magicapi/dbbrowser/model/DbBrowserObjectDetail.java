package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model;

import java.util.List;

public class DbBrowserObjectDetail {
	private DbBrowserObjectInfo object;
	private List<DbBrowserColumnInfo> columns;
	private List<String> primaryKeys;
	private List<DbBrowserIndexInfo> indexes;

	public DbBrowserObjectInfo getObject() { return object; }
	public void setObject(DbBrowserObjectInfo object) { this.object = object; }
	public List<DbBrowserColumnInfo> getColumns() { return columns; }
	public void setColumns(List<DbBrowserColumnInfo> columns) { this.columns = columns; }
	public List<String> getPrimaryKeys() { return primaryKeys; }
	public void setPrimaryKeys(List<String> primaryKeys) { this.primaryKeys = primaryKeys; }
	public List<DbBrowserIndexInfo> getIndexes() { return indexes; }
	public void setIndexes(List<DbBrowserIndexInfo> indexes) { this.indexes = indexes; }
}
