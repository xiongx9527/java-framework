package com.xiongx9527.fw.plus.tinyengine.dynamic.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class DynamicPageResult {
	private final List<Map<String, Object>> list;
	private final long total;
	private final int currentPage;
	private final int pageSize;
	public DynamicPageResult(List<Map<String, Object>> list, long total, int currentPage, int pageSize) {
		this.list = Collections.unmodifiableList(new ArrayList<Map<String, Object>>(list));
		this.total = total;
		this.currentPage = currentPage;
		this.pageSize = pageSize;
	}
	public List<Map<String, Object>> getList() { return list; }
	public long getTotal() { return total; }
	public int getCurrentPage() { return currentPage; }
	public int getPageSize() { return pageSize; }
}
