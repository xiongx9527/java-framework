package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model;

public class DbBrowserRoutineInfo {
	private String catalog;
	private String schema;
	private String name;
	private String type;
	private String remarks;

	public String getCatalog() { return catalog; }
	public void setCatalog(String catalog) { this.catalog = catalog; }
	public String getSchema() { return schema; }
	public void setSchema(String schema) { this.schema = schema; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	public String getRemarks() { return remarks; }
	public void setRemarks(String remarks) { this.remarks = remarks; }
}
