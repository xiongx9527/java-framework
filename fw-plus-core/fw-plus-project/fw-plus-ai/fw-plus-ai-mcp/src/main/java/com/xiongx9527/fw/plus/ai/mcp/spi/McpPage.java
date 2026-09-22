package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.List;

/**
 * MCP 分页响应
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpPage<T> {
	private List<T> items;
	private String nextCursor;
	private boolean hasMore;

	public McpPage() {
	}

	public McpPage(List<T> items, String nextCursor, boolean hasMore) {
		this.items = items;
		this.nextCursor = nextCursor;
		this.hasMore = hasMore;
	}

	public List<T> getItems() {
		return items;
	}

	public void setItems(List<T> items) {
		this.items = items;
	}

	public String getNextCursor() {
		return nextCursor;
	}

	public void setNextCursor(String nextCursor) {
		this.nextCursor = nextCursor;
	}

	public boolean isHasMore() {
		return hasMore;
	}

	public void setHasMore(boolean hasMore) {
		this.hasMore = hasMore;
	}
}
