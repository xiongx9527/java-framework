package com.xiongx9527.fw.plus.ai.mcp.server;

/**
 * MCP 服务器信息
 *
 * <p>对齐 MCP 2025-11-25 规范 serverInfo：名称、版本、可选 description。
 *
 * @author xiongxiao
 * @date 2025-09-20
 */
public class McpServerInfo {
	private final String name;
	private final String version;
	private final String description;

	public McpServerInfo(String name, String version) {
		this(name, version, null);
	}

	public McpServerInfo(String name, String version, String description) {
		this.name = name;
		this.version = version;
		this.description = description;
	}

	public String name() {
		return name;
	}

	public String version() {
		return version;
	}

	public String description() {
		return description;
	}

	@Override
	public String toString() {
		return "McpServerInfo{name='" + name + '\'' +
				", version='" + version + '\'' +
				(description != null ? ", description='" + description + '\'' : "") +
				'}';
	}
}
