package com.xiongx9527.fw.plus.tinyengine.dynamic.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class DynamicDelete {
	private String tableName;
	private Map<String, Object> conditions = new LinkedHashMap<String, Object>();
	public DynamicDelete() { }
	public DynamicDelete(String tableName, Map<String, Object> conditions) { this.tableName = tableName; setConditions(conditions); }
	public String getTableName() { return tableName; }
	public void setTableName(String tableName) { this.tableName = tableName; }
	public Map<String, Object> getConditions() { return conditions; }
	public void setConditions(Map<String, Object> conditions) { this.conditions = conditions == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(conditions); }
}
