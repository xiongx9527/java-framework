package com.xiongx9527.fw.plus.tinyengine.dynamic.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class DynamicInsert {
	private String tableName;
	private Map<String, Object> data = new LinkedHashMap<String, Object>();
	public DynamicInsert() { }
	public DynamicInsert(String tableName, Map<String, Object> data) { this.tableName = tableName; setData(data); }
	public String getTableName() { return tableName; }
	public void setTableName(String tableName) { this.tableName = tableName; }
	public Map<String, Object> getData() { return data; }
	public void setData(Map<String, Object> data) { this.data = data == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(data); }
}
