package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session;

import com.xiongx9527.fw.plus.common.exception.BizException;

public class McpSessionNotFoundException extends BizException {

	public McpSessionNotFoundException(String sessionId) {
		super("MCP session not found: " + sessionId);
	}
}
