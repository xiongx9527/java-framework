package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 补全结果
 *
 * <p>对齐 MCP 规范 completion/complete 响应：{ completion: { values, total, hasMore } }。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpCompletionResult {
	private List<String> completions;
	private boolean hasMore;
	private int total;

	public McpCompletionResult() {
	}

	public McpCompletionResult(List<String> completions, boolean hasMore, int total) {
		this.completions = completions;
		this.hasMore = hasMore;
		this.total = total;
	}

	public static McpCompletionResult of(List<String> values) {
		return new McpCompletionResult(values, false, values != null ? values.size() : 0);
	}

	public List<String> getCompletions() {
		return completions;
	}

	public void setCompletions(List<String> completions) {
		this.completions = completions;
	}

	public boolean isHasMore() {
		return hasMore;
	}

	public void setHasMore(boolean hasMore) {
		this.hasMore = hasMore;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		Map<String, Object> completion = new LinkedHashMap<>();
		List<String> values = completions != null ? completions : new ArrayList<String>();
		completion.put("values", values);
		completion.put("total", total);
		completion.put("hasMore", hasMore);
		map.put("completion", completion);
		return map;
	}
}
