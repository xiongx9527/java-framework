package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.logging;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.SaTokenMcpAuthService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;

import java.util.Collections;
import java.util.Map;

public class McpLoggingHandler {

	private final McpLogService logService;

	private final SaTokenMcpAuthService authService;

	private final MagicAPIMcpProperties properties;

	public McpLoggingHandler(McpLogService logService) {
		this(logService, null, null);
	}

	public McpLoggingHandler(McpLogService logService, SaTokenMcpAuthService authService,
							 MagicAPIMcpProperties properties) {
		this.logService = logService;
		this.authService = authService;
		this.properties = properties;
	}

	public Map<String, Object> setLevel(McpRequestContext context, Map<String, Object> params) {
		requireScope(context);
		String level = params == null || params.get("level") == null ? null : String.valueOf(params.get("level"));
		logService.setLevel(context == null ? null : context.getSessionId(), level);
		return Collections.emptyMap();
	}

	private void requireScope(McpRequestContext context) {
		if (authService != null && properties != null && properties.getAuth().isEnabled()) {
			authService.requireScope(context == null ? null : context.getPrincipal(), properties.getAuth().getLoggingScope());
		}
	}
}
