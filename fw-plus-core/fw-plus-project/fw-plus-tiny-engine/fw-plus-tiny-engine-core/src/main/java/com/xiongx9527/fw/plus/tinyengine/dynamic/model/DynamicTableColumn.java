package com.xiongx9527.fw.plus.tinyengine.dynamic.model;

public class DynamicTableColumn {

	private String name;
	private String type;
	private boolean nullable;

	public DynamicTableColumn() {
	}

	public DynamicTableColumn(String name, String type, boolean nullable) {
		this.name = name;
		this.type = type;
		this.nullable = nullable;
	}

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	public boolean isNullable() { return nullable; }
	public void setNullable(boolean nullable) { this.nullable = nullable; }
}
