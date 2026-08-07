package com.xiongx9527.fw.plus.mybatis.datachange.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TableMonitor {
	private String catalog;
	private String schema;
	private String name;
	private List<String> keyColumns = new ArrayList<String>();
	private List<String> includeColumns = new ArrayList<String>();
	private List<String> excludeColumns = new ArrayList<String>();
	private Integer maxSnapshotRows;
	private LogicalDeleteConfiguration logicalDelete;

	public String getCatalog() { return catalog; }
	public void setCatalog(String catalog) { this.catalog = catalog; }
	public String getSchema() { return schema; }
	public void setSchema(String schema) { this.schema = schema; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public List<String> getKeyColumns() { return Collections.unmodifiableList(new ArrayList<String>(keyColumns)); }
	public void setKeyColumns(List<String> keyColumns) { this.keyColumns = copy(keyColumns); }
	public List<String> getIncludeColumns() { return Collections.unmodifiableList(new ArrayList<String>(includeColumns)); }
	public void setIncludeColumns(List<String> includeColumns) { this.includeColumns = copy(includeColumns); }
	public List<String> getExcludeColumns() { return Collections.unmodifiableList(new ArrayList<String>(excludeColumns)); }
	public void setExcludeColumns(List<String> excludeColumns) { this.excludeColumns = copy(excludeColumns); }
	public Integer getMaxSnapshotRows() { return maxSnapshotRows; }
	public void setMaxSnapshotRows(Integer maxSnapshotRows) { this.maxSnapshotRows = maxSnapshotRows; }
	public LogicalDeleteConfiguration getLogicalDelete() { return logicalDelete; }
	public void setLogicalDelete(LogicalDeleteConfiguration logicalDelete) { this.logicalDelete = logicalDelete; }

	private List<String> copy(List<String> columns) {
		return columns == null ? new ArrayList<String>() : new ArrayList<String>(columns);
	}
}
