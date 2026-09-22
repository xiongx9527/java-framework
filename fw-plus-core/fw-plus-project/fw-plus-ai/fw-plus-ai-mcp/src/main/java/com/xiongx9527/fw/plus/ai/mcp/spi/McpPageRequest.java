package com.xiongx9527.fw.plus.ai.mcp.spi;

/**
 * MCP 分页请求
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpPageRequest {
	private int page = 1;
	private int pageSize = 20;
	private String cursor; // 游标分页

	public static McpPageRequest of(int page, int pageSize) {
		McpPageRequest request = new McpPageRequest();
		request.page = page;
		request.pageSize = pageSize;
		return request;
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

	public String getCursor() {
		return cursor;
	}

	public void setCursor(String cursor) {
		this.cursor = cursor;
	}
}
