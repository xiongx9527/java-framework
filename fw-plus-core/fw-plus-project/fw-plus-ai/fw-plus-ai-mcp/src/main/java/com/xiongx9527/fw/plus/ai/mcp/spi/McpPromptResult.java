package com.xiongx9527.fw.plus.ai.mcp.spi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 提示词结果
 *
 * <p>对齐 MCP 规范 prompts/get 响应：description（可选）+ messages 列表（role + content）。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpPromptResult {
	private String description;
	private List<McpPromptMessage> messages;

	public McpPromptResult() {
	}

	public McpPromptResult(String description, List<McpPromptMessage> messages) {
		this.description = description;
		this.messages = messages;
	}

	public static McpPromptResult of(String description, List<McpPromptMessage> messages) {
		return new McpPromptResult(description, messages);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<McpPromptMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<McpPromptMessage> messages) {
		this.messages = messages;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		if (description != null) {
			map.put("description", description);
		}
		if (messages != null) {
			List<Map<String, Object>> messageList = new ArrayList<Map<String, Object>>();
			for (McpPromptMessage msg : messages) {
				messageList.add(msg.toMap());
			}
			map.put("messages", messageList);
		}
		return map;
	}

	/**
	 * 提示词消息（role + content）
	 */
	public static class McpPromptMessage {
		private String role;
		private McpContent content;

		public McpPromptMessage() {
		}

		public McpPromptMessage(String role, McpContent content) {
			this.role = role;
			this.content = content;
		}

		public static McpPromptMessage of(String role, McpContent content) {
			return new McpPromptMessage(role, content);
		}

		public static McpPromptMessage user(McpContent content) {
			return new McpPromptMessage("user", content);
		}

		public static McpPromptMessage assistant(McpContent content) {
			return new McpPromptMessage("assistant", content);
		}

		public String getRole() {
			return role;
		}

		public void setRole(String role) {
			this.role = role;
		}

		public McpContent getContent() {
			return content;
		}

		public void setContent(McpContent content) {
			this.content = content;
		}

		public Map<String, Object> toMap() {
			Map<String, Object> map = new LinkedHashMap<>();
			map.put("role", role);
			if (content != null) {
				map.put("content", content.toMap());
			}
			return map;
		}
	}
}
