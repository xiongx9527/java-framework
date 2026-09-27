package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 提示词消息体。
 */
@Data
public class MagicMcpPromptMessage {

	private String role = "user";

	private String content;

	public MagicMcpPromptMessage() {
	}

	public MagicMcpPromptMessage(String role, String content) {
		this.role = role;
		this.content = content;
	}

	/**
	 * 提示词参数声明。
	 */
	@Data
	public static class Argument {

		private String name;

		private String description;

		private boolean required = true;
	}

	/**
	 * 提示词定义中的消息集合。
	 */
	@Data
	public static class Messages {

		private List<MagicMcpPromptMessage> messages = new ArrayList<MagicMcpPromptMessage>();
	}
}
