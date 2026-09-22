package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config;

/**
 * MCP 实例配置属性
 *
 * <p>对齐 spec 8.1：每实例可独立配置 name、path、server-name/version、
 * instructions、五件套能力开关、会话超时。
 *
 * <p>spec 非目标能力（sampling/roots）不暴露配置项。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public class McpInstanceProperties {

	private String name = "default";
	private String path = "/mcp";
	private boolean enabled = true;
	private String serverName = "fw-plus-mcp-server";
	private String serverVersion = "0.1.1";
	private String description;
	private String instructions;
	private boolean toolsEnabled = true;
	private boolean resourcesEnabled = true;
	private boolean promptsEnabled = true;
	private boolean completionsEnabled = true;
	private boolean loggingEnabled = true;
	private long sessionTimeoutSeconds = 1800L; // 30 minutes
	private String ssePath = "/mcp/sse";
	private String websocketPath = "/mcp/ws";

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getServerVersion() {
		return serverVersion;
	}

	public void setServerVersion(String serverVersion) {
		this.serverVersion = serverVersion;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getInstructions() {
		return instructions;
	}

	public void setInstructions(String instructions) {
		this.instructions = instructions;
	}

	public boolean isToolsEnabled() {
		return toolsEnabled;
	}

	public void setToolsEnabled(boolean toolsEnabled) {
		this.toolsEnabled = toolsEnabled;
	}

	public boolean isResourcesEnabled() {
		return resourcesEnabled;
	}

	public void setResourcesEnabled(boolean resourcesEnabled) {
		this.resourcesEnabled = resourcesEnabled;
	}

	public boolean isPromptsEnabled() {
		return promptsEnabled;
	}

	public void setPromptsEnabled(boolean promptsEnabled) {
		this.promptsEnabled = promptsEnabled;
	}

	public boolean isCompletionsEnabled() {
		return completionsEnabled;
	}

	public void setCompletionsEnabled(boolean completionsEnabled) {
		this.completionsEnabled = completionsEnabled;
	}

	public boolean isLoggingEnabled() {
		return loggingEnabled;
	}

	public void setLoggingEnabled(boolean loggingEnabled) {
		this.loggingEnabled = loggingEnabled;
	}

	public long getSessionTimeoutSeconds() {
		return sessionTimeoutSeconds;
	}

	public void setSessionTimeoutSeconds(long sessionTimeoutSeconds) {
		this.sessionTimeoutSeconds = sessionTimeoutSeconds;
	}
}
