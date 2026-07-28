package com.xiongx9527.fw.plus.tinyengine.dynamic.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DynamicTableDefinition {

	private String tableName;
	private List<DynamicColumnDefinition> columns = new ArrayList<DynamicColumnDefinition>();

	public DynamicTableDefinition() {
	}

	public DynamicTableDefinition(String tableName, List<DynamicColumnDefinition> columns) {
		this.tableName = tableName;
		setColumns(columns);
	}

	public String getTableName() { return tableName; }
	public void setTableName(String tableName) { this.tableName = tableName; }
	public List<DynamicColumnDefinition> getColumns() { return Collections.unmodifiableList(columns); }
	public void setColumns(List<DynamicColumnDefinition> columns) {
		this.columns = columns == null ? new ArrayList<DynamicColumnDefinition>() : new ArrayList<DynamicColumnDefinition>(columns);
	}
}
