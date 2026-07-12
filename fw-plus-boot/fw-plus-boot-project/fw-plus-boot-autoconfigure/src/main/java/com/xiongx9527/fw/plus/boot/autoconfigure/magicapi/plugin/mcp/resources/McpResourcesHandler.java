package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.resources;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.SaTokenMcpAuthService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcError;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.*;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;

import java.net.URI;
import java.util.*;

public class McpResourcesHandler {

	private final List<McpResourceProvider> providers;

	private final McpResourceSubscriptionService subscriptionService;

	private final SaTokenMcpAuthService authService;

	private final MagicAPIMcpProperties properties;

	public McpResourcesHandler(List<McpResourceProvider> providers, McpResourceSubscriptionService subscriptionService) {
		this(providers, subscriptionService, null, null);
	}

	public McpResourcesHandler(List<McpResourceProvider> providers, McpResourceSubscriptionService subscriptionService,
							   SaTokenMcpAuthService authService, MagicAPIMcpProperties properties) {
		this.providers = providers == null ? Collections.emptyList() : providers;
		this.subscriptionService = subscriptionService;
		this.authService = authService;
		this.properties = properties;
	}

	public Map<String, Object> listResources(McpRequestContext context, Map<String, Object> params) {
		requireScope(context);
		McpPageRequest pageRequest = pageRequest(params);
		List<McpResourceDefinition> resources = new ArrayList<>();
		String nextCursor = null;
		for (McpResourceProvider provider : providers) {
			McpPage<McpResourceDefinition> page = provider.listResources(new McpProviderContext(context), pageRequest);
			if (page != null && page.getItems() != null) {
				resources.addAll(page.getItems());
				nextCursor = page.getNextCursor();
			}
		}
		Map<String, Object> result = new HashMap<>();
		result.put("resources", resources);
		if (nextCursor != null) {
			result.put("nextCursor", nextCursor);
		}
		return result;
	}

	public Map<String, Object> readResource(McpRequestContext context, Map<String, Object> params) {
		requireScope(context);
		McpResourceReadRequest request = readRequest(params);
		validateUri(request.getUri());
		for (McpResourceProvider provider : providers) {
			McpResourceReadResult result = provider.readResource(new McpProviderContext(context), request);
			if (result != null) {
				validateOutputSize(result);
				Map<String, Object> response = new HashMap<>();
				response.put("contents", result.getContents() == null ? Collections.emptyList() : result.getContents());
				return response;
			}
		}
		throw new McpJsonRpcException(McpJsonRpcError.INVALID_PARAMS, "Resource not found: " + request.getUri());
	}

	public Map<String, Object> listTemplates(McpRequestContext context, Map<String, Object> params) {
		requireScope(context);
		McpPageRequest pageRequest = pageRequest(params);
		List<McpResourceTemplate> templates = new ArrayList<>();
		String nextCursor = null;
		for (McpResourceProvider provider : providers) {
			McpPage<McpResourceTemplate> page = provider.listTemplates(new McpProviderContext(context), pageRequest);
			if (page != null && page.getItems() != null) {
				templates.addAll(page.getItems());
				nextCursor = page.getNextCursor();
			}
		}
		Map<String, Object> result = new HashMap<>();
		result.put("resourceTemplates", templates);
		if (nextCursor != null) {
			result.put("nextCursor", nextCursor);
		}
		return result;
	}

	public Map<String, Object> subscribe(McpRequestContext context, Map<String, Object> params) {
		requireScope(context);
		McpResourceReadRequest request = readRequest(params);
		validateUri(request.getUri());
		subscriptionService.subscribe(context == null ? null : context.getSessionId(), request.getUri());
		return Collections.emptyMap();
	}

	public Map<String, Object> unsubscribe(McpRequestContext context, Map<String, Object> params) {
		requireScope(context);
		McpResourceReadRequest request = readRequest(params);
		validateUri(request.getUri());
		subscriptionService.unsubscribe(context == null ? null : context.getSessionId(), request.getUri());
		return Collections.emptyMap();
	}

	private McpPageRequest pageRequest(Map<String, Object> params) {
		McpPageRequest pageRequest = new McpPageRequest();
		if (params != null && params.get("cursor") != null) {
			pageRequest.setCursor(String.valueOf(params.get("cursor")));
		}
		return pageRequest;
	}

	private McpResourceReadRequest readRequest(Map<String, Object> params) {
		McpResourceReadRequest request = new McpResourceReadRequest();
		if (params != null && params.get("uri") != null) {
			request.setUri(String.valueOf(params.get("uri")));
		}
		return request;
	}

	private void validateUri(String uri) {
		try {
			URI parsed = URI.create(uri);
			if (!"mcp".equals(parsed.getScheme()) || parsed.getHost() == null) {
				throw new IllegalArgumentException();
			}
		} catch (Exception e) {
			throw new McpJsonRpcException(McpJsonRpcError.INVALID_PARAMS, "Invalid resource uri");
		}
	}

	private void requireScope(McpRequestContext context) {
		if (authService != null && properties != null && properties.getAuth().isEnabled()) {
			authService.requireScope(context == null ? null : context.getPrincipal(), properties.getAuth().getResourcesScope());
		}
	}

	private void validateOutputSize(McpResourceReadResult result) {
		if (properties != null && properties.getMaxResponseChars() > 0
				&& String.valueOf(result).length() > properties.getMaxResponseChars()) {
			throw new McpJsonRpcException(McpJsonRpcError.INVALID_PARAMS, "Resource output too large");
		}
	}
}
