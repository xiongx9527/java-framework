package com.xiongx9527.fw.plus.tinyengine.common.model;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public class PageQueryVo<T> {

	public static final int PAGESIZE_MAX = 200;
	public static final int PAGESIZE_DEFAULT = 10;

	private int pageSize = PAGESIZE_DEFAULT;
	private int current;
	private T data;

	public void setPageSize(int pageSize) {
		if (pageSize > PAGESIZE_MAX) {
			this.pageSize = PAGESIZE_MAX;
		}
		else if (pageSize <= 0) {
			this.pageSize = PAGESIZE_DEFAULT;
		}
		else {
			this.pageSize = pageSize;
		}
	}

	public int getPageSize() { return pageSize; }
	public int getCurrent() { return current; }
	public void setCurrent(int current) { this.current = current; }
	public T getData() { return data; }
	public void setData(T data) { this.data = data; }

	public <E> Page<E> getPage() {
		Page<E> page = new Page<E>();
		page.setCurrent(current);
		page.setSize(pageSize);
		return page;
	}
}
