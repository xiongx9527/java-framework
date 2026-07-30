package com.xiongx9527.fw.plus.tinyengine.dynamic.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class DynamicUpdate {
	private String tableName;
	private Map<String, Object> data = new LinkedHashMap<String, Object>();
	private Map<String, Object> conditions = new LinkedHashMap<String, Object>();
	public DynamicUpdate() { }
	public DynamicUpdate(String tableName, Map<String, Object> data, Map<String, Object> conditions) { this.tableName = tableName; setData(data); setConditions(conditions); }
	public String getTableName() { return tableName; }
	public void setTableName(String tableName) { this.tableName = tableName; }
	public Map<String, Object> getData() { return data; }
	public void setData(Map<String, Object> data) { this.data = data == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(data); }
	public Map<String, Object> getConditions() { return conditions; }
	public void setConditions(Map<String, Object> conditions) { this.conditions = conditions == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(conditions); }
}
