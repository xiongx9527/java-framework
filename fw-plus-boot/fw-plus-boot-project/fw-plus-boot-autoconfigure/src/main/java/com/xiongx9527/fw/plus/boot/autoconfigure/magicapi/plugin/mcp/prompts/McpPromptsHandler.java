package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.prompts;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.SaTokenMcpAuthService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcError;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.*;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;

import java.util.*;

public class McpPromptsHandler {

	private final List<McpPromptProvider> providers;

	private final SaTokenMcpAuthService authService;

	private final MagicAPIMcpProperties properties;

	public McpPromptsHandler(List<McpPromptProvider> providers) {
		this(providers, null, null);
	}

	public McpPromptsHandler(List<McpPromptProvider> providers, SaTokenMcpAuthService authService,
							 MagicAPIMcpProperties properties) {
		this.providers = providers == null ? Collections.emptyList() : providers;
		this.authService = authService;
		this.properties = properties;
	}

	public Map<String, Object> listPrompts(McpRequestContext context, Map<String, Object> params) {
		requireScope(context);
		McpPageRequest pageRequest = pageRequest(params);
		List<McpPromptDefinition> prompts = new ArrayList<>();
		String nextCursor = null;
		for (McpPromptProvider provider : providers) {
			McpPage<McpPromptDefinition> page = provider.listPrompts(new McpProviderContext(context), pageRequest);
			if (page != null && page.getItems() != null) {
				prompts.addAll(page.getItems());
				nextCursor = page.getNextCursor();
			}
		}
		Map<String, Object> result = new HashMap<>();
		result.put("prompts", prompts);
		if (nextCursor != null) {
			result.put("nextCursor", nextCursor);
		}
		return result;
	}

	public Map<String, Object> getPrompt(McpRequestContext context, Map<String, Object> params) {
		requireScope(context);
		McpPromptGetRequest request = getRequest(params);
		validateRequiredArguments(context, request);
		for (McpPromptProvider provider : providers) {
			McpPromptResult promptResult = provider.getPrompt(new McpProviderContext(context), request);
			if (promptResult != null) {
				Map<String, Object> result = new HashMap<>();
				result.put("description", promptResult.getDescription());
				result.put("messages", promptResult.getMessages() == null ? Collections.emptyList() : promptResult.getMessages());
				return result;
			}
		}
		throw new McpJsonRpcException(McpJsonRpcError.INVALID_PARAMS, "Prompt not found: " + request.getName());
	}

	private void validateRequiredArguments(McpRequestContext context, McpPromptGetRequest request) {
		for (McpPromptProvider provider : providers) {
			McpPage<McpPromptDefinition> page = provider.listPrompts(new McpProviderContext(context), new McpPageRequest());
			if (page == null || page.getItems() == null) {
				continue;
			}
			for (McpPromptDefinition definition : page.getItems()) {
				if (!Objects.equals(definition.getName(), request.getName()) || definition.getArguments() == null) {
					continue;
				}
				for (McpPromptArgument argument : definition.getArguments()) {
					if (argument.isRequired() && (request.getArguments() == null
							|| !request.getArguments().containsKey(argument.getName()))) {
						throw new McpJsonRpcException(McpJsonRpcError.INVALID_PARAMS,
								"Missing prompt argument: " + argument.getName());
					}
				}
			}
		}
	}

	private McpPageRequest pageRequest(Map<String, Object> params) {
		McpPageRequest pageRequest = new McpPageRequest();
		if (params != null && params.get("cursor") != null) {
			pageRequest.setCursor(String.valueOf(params.get("cursor")));
		}
		return pageRequest;
	}

	@SuppressWarnings("unchecked")
	private McpPromptGetRequest getRequest(Map<String, Object> params) {
		McpPromptGetRequest request = new McpPromptGetRequest();
		if (params != null) {
			request.setName(params.get("name") == null ? null : String.valueOf(params.get("name")));
			Object arguments = params.get("arguments");
			if (arguments instanceof Map) {
				request.setArguments((Map<String, Object>) arguments);
			}
		}
		return request;
	}

	private void requireScope(McpRequestContext context) {
		if (authService != null && properties != null && properties.getAuth().isEnabled()) {
			authService.requireScope(context == null ? null : context.getPrincipal(), properties.getAuth().getPromptsScope());
		}
	}

	public static Map<String, Object> listChanged() {
		Map<String, Object> notification = new HashMap<>();
		notification.put("jsonrpc", "2.0");
		notification.put("method", "notifications/prompts/list_changed");
		return notification;
	}
}
