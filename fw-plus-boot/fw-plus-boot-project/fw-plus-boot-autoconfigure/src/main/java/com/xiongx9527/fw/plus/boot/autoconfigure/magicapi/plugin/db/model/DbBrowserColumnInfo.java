package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model;

public class DbBrowserColumnInfo {
	private String name;
	private Integer jdbcType;
	private String typeName;
	private Integer size;
	private Integer decimalDigits;
	private Integer nullable;
	private String defaultValue;
	private String remarks;
	private Integer ordinalPosition;

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public Integer getJdbcType() { return jdbcType; }
	public void setJdbcType(Integer jdbcType) { this.jdbcType = jdbcType; }
	public String getTypeName() { return typeName; }
	public void setTypeName(String typeName) { this.typeName = typeName; }
	public Integer getSize() { return size; }
	public void setSize(Integer size) { this.size = size; }
	public Integer getDecimalDigits() { return decimalDigits; }
	public void setDecimalDigits(Integer decimalDigits) { this.decimalDigits = decimalDigits; }
	public Integer getNullable() { return nullable; }
	public void setNullable(Integer nullable) { this.nullable = nullable; }
	public String getDefaultValue() { return defaultValue; }
	public void setDefaultValue(String defaultValue) { this.defaultValue = defaultValue; }
	public String getRemarks() { return remarks; }
	public void setRemarks(String remarks) { this.remarks = remarks; }
	public Integer getOrdinalPosition() { return ordinalPosition; }
	public void setOrdinalPosition(Integer ordinalPosition) { this.ordinalPosition = ordinalPosition; }
}
