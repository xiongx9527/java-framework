package com.xiongx9527.fw.plus.tinyengine.dynamic.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DynamicTableAlteration {

	private String tableName;
	private List<DynamicColumnDefinition> addColumns = new ArrayList<DynamicColumnDefinition>();
	private List<String> dropColumns = new ArrayList<String>();

	public DynamicTableAlteration() {
	}

	public DynamicTableAlteration(String tableName, List<DynamicColumnDefinition> addColumns, List<String> dropColumns) {
		this.tableName = tableName;
		setAddColumns(addColumns);
		setDropColumns(dropColumns);
	}

	public String getTableName() { return tableName; }
	public void setTableName(String tableName) { this.tableName = tableName; }
	public List<DynamicColumnDefinition> getAddColumns() { return Collections.unmodifiableList(addColumns); }
	public void setAddColumns(List<DynamicColumnDefinition> addColumns) { this.addColumns = addColumns == null ? new ArrayList<DynamicColumnDefinition>() : new ArrayList<DynamicColumnDefinition>(addColumns); }
	public List<String> getDropColumns() { return Collections.unmodifiableList(dropColumns); }
	public void setDropColumns(List<String> dropColumns) { this.dropColumns = dropColumns == null ? new ArrayList<String>() : new ArrayList<String>(dropColumns); }
}
