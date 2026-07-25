package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.db.model;

import java.util.List;

public class DbBrowserPreviewRequest {
	private String datasourceKey;
	private String catalog;
	private String schema;
	private String objectName;
	private String objectType;
	private Integer page;
	private Integer pageSize;
	private String sortColumn;
	private String sortDirection;
	private List<DbBrowserFilter> filters;

	public String getDatasourceKey() {
		return datasourceKey;
	}

	public void setDatasourceKey(String datasourceKey) {
		this.datasourceKey = datasourceKey;
	}

	public String getCatalog() {
		return catalog;
	}

	public void setCatalog(String catalog) {
		this.catalog = catalog;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public Integer getPage() {
		return page;
	}

	public void setPage(Integer page) {
		this.page = page;
	}

	public Integer getPageSize() {
		return pageSize;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public String getSortColumn() {
		return sortColumn;
	}

	public void setSortColumn(String sortColumn) {
		this.sortColumn = sortColumn;
	}

	public String getSortDirection() {
		return sortDirection;
	}

	public void setSortDirection(String sortDirection) {
		this.sortDirection = sortDirection;
	}

	public List<DbBrowserFilter> getFilters() {
		return filters;
	}

	public void setFilters(List<DbBrowserFilter> filters) {
		this.filters = filters;
	}
}
