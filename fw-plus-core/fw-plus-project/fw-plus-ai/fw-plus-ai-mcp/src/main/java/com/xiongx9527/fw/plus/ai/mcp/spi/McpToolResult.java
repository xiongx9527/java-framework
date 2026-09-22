package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具执行结果
 *
 * <p>对齐 MCP 规范：content 数组 + isError 标记 + 可选 structuredContent。
 * 工具执行业务失败使用 isError=true 的结果，而非协议层错误。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public class McpToolResult {
	private List<McpContent> content;
	private boolean isError;
	private Map<String, Object> structuredContent;

	public McpToolResult() {
	}

	public McpToolResult(List<McpContent> content) {
		this.content = content;
	}

	/**
	 * 创建成功结果（文本内容）
	 */
	public static McpToolResult success(String text) {
		McpToolResult result = new McpToolResult();
		result.content = new ArrayList<McpContent>();
		result.content.add(McpContent.text(text));
		result.isError = false;
		return result;
	}

	/**
	 * 创建成功结果（多内容）
	 */
	public static McpToolResult success(List<McpContent> content) {
		McpToolResult result = new McpToolResult();
		result.content = content;
		result.isError = false;
		return result;
	}

	/**
	 * 创建错误结果（业务错误，isError=true，非协议错误）
	 */
	public static McpToolResult error(String message) {
		McpToolResult result = new McpToolResult();
		result.content = new ArrayList<McpContent>();
		result.content.add(McpContent.text(message));
		result.isError = true;
		return result;
	}

	public List<McpContent> getContent() {
		return content;
	}

	public void setContent(List<McpContent> content) {
		this.content = content;
	}

	public boolean isError() {
		return isError;
	}

	public void setError(boolean error) {
		isError = error;
	}

	public Map<String, Object> getStructuredContent() {
		return structuredContent;
	}

	public void setStructuredContent(Map<String, Object> structuredContent) {
		this.structuredContent = structuredContent;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		List<Map<String, Object>> contentList = new ArrayList<Map<String, Object>>();
		if (content != null) {
			for (McpContent item : content) {
				contentList.add(item.toMap());
			}
		}
		map.put("content", contentList);
		map.put("isError", isError);
		if (structuredContent != null) {
			map.put("structuredContent", structuredContent);
		}
		return map;
	}
}
