package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcError;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSession;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionNotFoundException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionState;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class McpLifecycleHandlers {

	private final McpSessionService sessionService;

	private final List<String> supportedProtocolVersions;

	private final String defaultProtocolVersion;

	private final Capabilities capabilities;

	public McpLifecycleHandlers(McpSessionService sessionService) {
		this(sessionService, null);
	}

	public McpLifecycleHandlers(McpSessionService sessionService, MagicAPIMcpProperties properties) {
		this(sessionService, properties, new Capabilities());
	}

	public McpLifecycleHandlers(McpSessionService sessionService, MagicAPIMcpProperties properties,
							 Capabilities capabilities) {
		this.sessionService = sessionService;
		MagicAPIMcpProperties.Protocol protocol = properties == null ? new MagicAPIMcpProperties.Protocol() : properties.getProtocol();
		this.defaultProtocolVersion = protocol.getDefaultVersion();
		this.supportedProtocolVersions = protocol.getSupportedVersions() == null
				? Collections.<String>emptyList() : new ArrayList<>(protocol.getSupportedVersions());
		this.capabilities = capabilities == null ? new Capabilities() : capabilities;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Object> initialize(McpRequestContext context, Map<String, Object> params) {
		Map<String, Object> safeParams = params == null ? Collections.emptyMap() : params;
		String protocolVersion = stringValue(safeParams.get("protocolVersion"), defaultProtocolVersion);
		if (!supportedProtocolVersions.contains(protocolVersion)) {
			throw new McpJsonRpcException(McpJsonRpcError.INVALID_PARAMS,
					"Unsupported protocol version: " + protocolVersion);
		}
		Object capabilities = safeParams.get("capabilities");
		Map<String, Object> clientCapabilities = capabilities instanceof Map ? (Map<String, Object>) capabilities : Collections.emptyMap();
		McpSession session = sessionService.createSession(context.getPrincipal(), clientCapabilities, protocolVersion);
		context.setSessionId(session.getId());

		Map<String, Object> result = new HashMap<>();
		result.put("protocolVersion", protocolVersion);
		result.put("capabilities", serverCapabilities());
		result.put("serverInfo", serverInfo());
		return result;
	}

	public Object initialized(McpRequestContext context, Object params) {
		sessionService.markReady(context.getSessionId());
		return Collections.emptyMap();
	}

	public Object ping(McpRequestContext context, Object params) {
		return Collections.emptyMap();
	}

	public Object cancelled(McpRequestContext context, Map<String, Object> params) {
		McpSession session = sessionService.requireSession(context.getSessionId());
		if (params != null && params.get("requestId") != null) {
			session.getCancelledRequests().add(params.get("requestId"));
		}
		return Collections.emptyMap();
	}

	public Object progress(McpRequestContext context, Map<String, Object> params) {
		sessionService.requireSession(context.getSessionId());
		return Collections.emptyMap();
	}

	public void requireReady(McpRequestContext context) {
		McpSession session;
		try {
			session = sessionService.requireSession(context == null ? null : context.getSessionId());
		} catch (McpSessionNotFoundException e) {
			throw new McpJsonRpcException(McpJsonRpcError.INVALID_REQUEST, "Session is not ready");
		}
		if (session.getState() != McpSessionState.READY) {
			throw new McpJsonRpcException(McpJsonRpcError.INVALID_REQUEST, "Session is not ready");
		}
	}

	private Map<String, Object> serverCapabilities() {
		Map<String, Object> capabilities = new HashMap<>();
		Map<String, Object> tools = new HashMap<>();
		if (this.capabilities.isToolsListChanged()) {
			tools.put("listChanged", true);
		}
		capabilities.put("tools", tools);
		Map<String, Object> resources = new HashMap<>();
		if (this.capabilities.isResourcesListChanged()) {
			resources.put("listChanged", true);
		}
		if (this.capabilities.isResourceSubscribe()) {
			resources.put("subscribe", true);
		}
		capabilities.put("resources", resources);
		Map<String, Object> prompts = new HashMap<>();
		if (this.capabilities.isPromptsListChanged()) {
			prompts.put("listChanged", true);
		}
		capabilities.put("prompts", prompts);
		if (this.capabilities.isLogging()) {
			capabilities.put("logging", Collections.emptyMap());
		}
		if (this.capabilities.isCompletion()) {
			capabilities.put("completion", Collections.emptyMap());
		}
		return capabilities;
	}

	public static class Capabilities {

		private boolean toolsListChanged;
		private boolean resourcesListChanged;
		private boolean resourceSubscribe;
		private boolean promptsListChanged;
		private boolean logging;
		private boolean completion;

		public boolean isToolsListChanged() { return toolsListChanged; }
		public void setToolsListChanged(boolean toolsListChanged) { this.toolsListChanged = toolsListChanged; }
		public boolean isResourcesListChanged() { return resourcesListChanged; }
		public void setResourcesListChanged(boolean resourcesListChanged) { this.resourcesListChanged = resourcesListChanged; }
		public boolean isResourceSubscribe() { return resourceSubscribe; }
		public void setResourceSubscribe(boolean resourceSubscribe) { this.resourceSubscribe = resourceSubscribe; }
		public boolean isPromptsListChanged() { return promptsListChanged; }
		public void setPromptsListChanged(boolean promptsListChanged) { this.promptsListChanged = promptsListChanged; }
		public boolean isLogging() { return logging; }
		public void setLogging(boolean logging) { this.logging = logging; }
		public boolean isCompletion() { return completion; }
		public void setCompletion(boolean completion) { this.completion = completion; }
	}

	private Map<String, Object> serverInfo() {
		Map<String, Object> serverInfo = new HashMap<>();
		serverInfo.put("name", "fw-plus-magicapi-mcp");
		serverInfo.put("version", "0.1.1-SNAPSHOT");
		return serverInfo;
	}

	private String stringValue(Object value, String defaultValue) {
		return value == null ? defaultValue : String.valueOf(value);
	}
}
