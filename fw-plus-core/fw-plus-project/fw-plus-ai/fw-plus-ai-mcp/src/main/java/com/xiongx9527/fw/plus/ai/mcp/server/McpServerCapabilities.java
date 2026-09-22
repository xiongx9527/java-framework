package com.xiongx9527.fw.plus.ai.mcp.server;

/**
 * MCP 服务器能力声明
 *
 * <p>对齐 MCP 2025-11-25 规范：logging、tools、resources、prompts、completions。
 * Sampling/Roots/Tasks/Elicitation 等非目标能力不声明。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpServerCapabilities {
	private final boolean logging;
	private final boolean tools;
	private final boolean resources;
	private final boolean prompts;
	private final boolean completions;

	private McpServerCapabilities(Builder builder) {
		this.logging = builder.logging;
		this.tools = builder.tools;
		this.resources = builder.resources;
		this.prompts = builder.prompts;
		this.completions = builder.completions;
	}

	public boolean logging() {
		return logging;
	}

	public boolean tools() {
		return tools;
	}

	public boolean resources() {
		return resources;
	}

	public boolean prompts() {
		return prompts;
	}

	public boolean completions() {
		return completions;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private boolean logging = false;
		private boolean tools = false;
		private boolean resources = false;
		private boolean prompts = false;
		private boolean completions = false;

		public Builder logging(boolean logging) {
			this.logging = logging;
			return this;
		}

		public Builder tools(boolean tools) {
			this.tools = tools;
			return this;
		}

		public Builder resources(boolean resources) {
			this.resources = resources;
			return this;
		}

		public Builder prompts(boolean prompts) {
			this.prompts = prompts;
			return this;
		}

		public Builder completions(boolean completions) {
			this.completions = completions;
			return this;
		}

		public McpServerCapabilities build() {
			return new McpServerCapabilities(this);
		}
	}
}
