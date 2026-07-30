package com.xiongx9527.fw.plus.tinyengine.dynamic.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicQuery {

	private String tableName;
	private List<String> fields = new ArrayList<String>();
	private Map<String, Object> conditions = new LinkedHashMap<String, Object>();
	private Integer currentPage = Integer.valueOf(1);
	private Integer pageSize = Integer.valueOf(10);
	private String orderBy;
	private String orderType = "ASC";

	public String getTableName() { return tableName; }
	public void setTableName(String tableName) { this.tableName = tableName; }
	public List<String> getFields() { return fields; }
	public void setFields(List<String> fields) { this.fields = fields == null ? new ArrayList<String>() : new ArrayList<String>(fields); }
	public Map<String, Object> getConditions() { return conditions; }
	public void setConditions(Map<String, Object> conditions) { this.conditions = conditions == null ? new LinkedHashMap<String, Object>() : new LinkedHashMap<String, Object>(conditions); }
	public Integer getCurrentPage() { return currentPage; }
	public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }
	public Integer getPageSize() { return pageSize; }
	public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
	public String getOrderBy() { return orderBy; }
	public void setOrderBy(String orderBy) { this.orderBy = orderBy; }
	public String getOrderType() { return orderType; }
	public void setOrderType(String orderType) { this.orderType = orderType; }
}
