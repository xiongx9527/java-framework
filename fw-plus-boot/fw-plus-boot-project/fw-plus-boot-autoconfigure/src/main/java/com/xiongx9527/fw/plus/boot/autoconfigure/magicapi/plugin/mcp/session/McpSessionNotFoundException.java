package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session;

public class McpSessionNotFoundException extends RuntimeException {

	public McpSessionNotFoundException(String sessionId) {
		super("MCP session not found: " + sessionId);
	}
}
