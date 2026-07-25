package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model;

public class DbBrowserIndexInfo {
	private String name;
	private Boolean nonUnique;
	private String columnName;
	private Integer ordinalPosition;
	private String ascOrDesc;
	private Short type;

	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public Boolean getNonUnique() { return nonUnique; }
	public void setNonUnique(Boolean nonUnique) { this.nonUnique = nonUnique; }
	public String getColumnName() { return columnName; }
	public void setColumnName(String columnName) { this.columnName = columnName; }
	public Integer getOrdinalPosition() { return ordinalPosition; }
	public void setOrdinalPosition(Integer ordinalPosition) { this.ordinalPosition = ordinalPosition; }
	public String getAscOrDesc() { return ascOrDesc; }
	public void setAscOrDesc(String ascOrDesc) { this.ascOrDesc = ascOrDesc; }
	public Short getType() { return type; }
	public void setType(Short type) { this.type = type; }
}
