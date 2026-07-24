package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.dbbrowser.model;

import java.util.List;
import java.util.Map;

public class DbBrowserPreviewResult {
	private List<String> columns;
	private List<Map<String, Object>> rows;
	private int page;
	private int pageSize;
	private boolean hasMore;
	private String appliedSort;
	private List<DbBrowserFilter> appliedFilters;

	public List<String> getColumns() {
		return columns;
	}

	public void setColumns(List<String> columns) {
		this.columns = columns;
	}

	public List<Map<String, Object>> getRows() {
		return rows;
	}

	public void setRows(List<Map<String, Object>> rows) {
		this.rows = rows;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public boolean isHasMore() {
		return hasMore;
	}

	public void setHasMore(boolean hasMore) {
		this.hasMore = hasMore;
	}

	public String getAppliedSort() {
		return appliedSort;
	}

	public void setAppliedSort(String appliedSort) {
		this.appliedSort = appliedSort;
	}

	public List<DbBrowserFilter> getAppliedFilters() {
		return appliedFilters;
	}

	public void setAppliedFilters(List<DbBrowserFilter> appliedFilters) {
		this.appliedFilters = appliedFilters;
	}
}
