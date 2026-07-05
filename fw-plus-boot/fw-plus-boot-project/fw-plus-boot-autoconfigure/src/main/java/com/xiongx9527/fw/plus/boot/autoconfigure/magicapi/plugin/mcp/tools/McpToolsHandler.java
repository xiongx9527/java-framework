package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.tools;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.SaTokenMcpAuthService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcError;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.*;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;

import java.util.*;

public class McpToolsHandler {

	private final List<McpToolProvider> providers;

	private final SaTokenMcpAuthService authService;

	private final MagicAPIMcpProperties properties;

	public McpToolsHandler(List<McpToolProvider> providers) {
		this(providers, null, null);
	}

	public McpToolsHandler(List<McpToolProvider> providers, SaTokenMcpAuthService authService,
						   MagicAPIMcpProperties properties) {
		this.providers = providers == null ? Collections.emptyList() : providers;
		this.authService = authService;
		this.properties = properties;
	}

	public Map<String, Object> listTools(McpRequestContext context, Map<String, Object> params) {
		requireScope(context);
		McpPageRequest pageRequest = pageRequest(params);
		List<McpToolDefinition> tools = new ArrayList<>();
		String nextCursor = null;
		for (McpToolProvider provider : providers) {
			McpPage<McpToolDefinition> page = provider.listTools(new McpProviderContext(context), pageRequest);
			if (page != null && page.getItems() != null) {
				tools.addAll(page.getItems());
				nextCursor = page.getNextCursor();
			}
		}
		Map<String, Object> result = new HashMap<>();
		result.put("tools", tools);
		if (nextCursor != null) {
			result.put("nextCursor", nextCursor);
		}
		return result;
	}

	public Map<String, Object> callTool(McpRequestContext context, Map<String, Object> params) {
		requireScope(context);
		McpToolCallRequest request = callRequest(params);
		for (McpToolProvider provider : providers) {
			McpToolResult toolResult = provider.callTool(new McpProviderContext(context), request);
			if (toolResult != null) {
				validateOutputSize(toolResult);
				return toMap(toolResult);
			}
		}
		throw new McpJsonRpcException(McpJsonRpcError.INVALID_PARAMS, "Tool not found: " + request.getName());
	}

	private McpPageRequest pageRequest(Map<String, Object> params) {
		McpPageRequest pageRequest = new McpPageRequest();
		if (params != null && params.get("cursor") != null) {
			pageRequest.setCursor(String.valueOf(params.get("cursor")));
		}
		return pageRequest;
	}

	@SuppressWarnings("unchecked")
	private McpToolCallRequest callRequest(Map<String, Object> params) {
		McpToolCallRequest request = new McpToolCallRequest();
		if (params != null) {
			request.setName(params.get("name") == null ? null : String.valueOf(params.get("name")));
			Object arguments = params.get("arguments");
			if (arguments instanceof Map) {
				request.setArguments((Map<String, Object>) arguments);
			}
		}
		return request;
	}

	private Map<String, Object> toMap(McpToolResult toolResult) {
		Map<String, Object> result = new HashMap<>();
		result.put("content", toolResult.getContent());
		result.put("structuredContent", toolResult.getStructuredContent());
		result.put("isError", toolResult.isError());
		return result;
	}

	private void requireScope(McpRequestContext context) {
		if (authService != null && properties != null && properties.getAuth().isEnabled()) {
			authService.requireScope(context == null ? null : context.getPrincipal(), properties.getAuth().getToolsScope());
		}
	}

	private void validateOutputSize(McpToolResult toolResult) {
		if (properties != null && properties.getMaxResponseChars() > 0
				&& String.valueOf(toolResult).length() > properties.getMaxResponseChars()) {
			throw new McpJsonRpcException(McpJsonRpcError.INVALID_PARAMS, "Tool output too large");
		}
	}

	public static Map<String, Object> listChanged() {
		Map<String, Object> notification = new HashMap<>();
		notification.put("jsonrpc", "2.0");
		notification.put("method", "notifications/tools/list_changed");
		return notification;
	}
}
