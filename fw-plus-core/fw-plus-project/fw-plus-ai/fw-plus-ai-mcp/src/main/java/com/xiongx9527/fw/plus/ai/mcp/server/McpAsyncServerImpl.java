package com.xiongx9527.fw.plus.ai.mcp.server;

import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcError;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionRef;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionRequest;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpCompletionResult;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpContent;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptInfo;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptRequest;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptResult;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpProgressToken;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceContent;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceInfo;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceReadRequest;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceTemplate;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpTool;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolCallRequest;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolResult;
import com.xiongx9527.fw.plus.ai.mcp.transport.McpServerTransportProvider;
import com.xiongx9527.fw.plus.ai.mcp.transport.McpStreamableServerTransportProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP 异步服务器实现
 *
 * <p>对齐 MCP 2025-11-25 协议规范，不依赖外部 MCP SDK。
 * 在 Spring Boot 环境下同步处理请求，通过 {@link McpStreamableServerTransportProvider}
 * 或 {@link McpServerTransportProvider} 与传输层解耦。
 *
 * <p>支持的能力：initialize（版本协商）、notifications/initialized、ping、
 * tools（list/call/list_changed）、resources（list/read/templates/list/subscribe/unsubscribe/list_changed）、
 * prompts（list/get/list_changed）、completion/complete、logging（setLevel/notifications/message）。
 *
 * <p>非目标：sampling、roots、tasks、elicitation、OAuth 不实现。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public class McpAsyncServerImpl {

	private static final Logger logger = LoggerFactory.getLogger(McpAsyncServerImpl.class);

	/** 支持的协议版本（按 spec 3 接受 2025-11-25 与 2025-06-18） */
	private static final Set<String> SUPPORTED_PROTOCOL_VERSIONS;
	static {
		Set<String> versions = new LinkedHashSet<>();
		versions.add("2025-11-25");
		versions.add("2025-06-18");
		SUPPORTED_PROTOCOL_VERSIONS = Collections.unmodifiableSet(versions);
	}

	/** 默认协议版本（当客户端未提供版本时回退） */
	private static final String DEFAULT_PROTOCOL_VERSION = "2025-11-25";

	/** 默认日志级别 */
	private static final String DEFAULT_LOG_LEVEL = "info";

	/** 会话超时时间（毫秒） */
	private static final long SESSION_TIMEOUT_MS = 30L * 60L * 1000L;

	private final Object transportProvider; // McpStreamableServerTransportProvider or McpServerTransportProvider
	private final boolean async;

	private final McpServerInfo serverInfo;
	private final McpServerCapabilities capabilities;
	private final String instructions;

	private final McpToolRegistry toolRegistry;
	private final McpResourceRegistry resourceRegistry;
	private final McpPromptRegistry promptRegistry;
	private final McpCompletionRegistry completionRegistry;

	private final McpResourceSubscriptionManager subscriptionManager;

	private volatile boolean running = false;

	/** 会话表：sessionId -> 会话状态 */
	private final Map<String, McpSessionState> sessions = new ConcurrentHashMap<>();

	/** 当前请求的 sessionId（用于处理工具调用等） */
	private final ThreadLocal<String> currentSessionId = new ThreadLocal<>();

	McpAsyncServerImpl(
			McpStreamableServerTransportProvider transportProvider,
			McpServerInfo serverInfo,
			McpServerCapabilities capabilities,
			String instructions,
			McpToolRegistry toolRegistry,
			McpResourceRegistry resourceRegistry,
			McpPromptRegistry promptRegistry,
			McpCompletionRegistry completionRegistry,
			boolean async) {
		this.transportProvider = transportProvider;
		this.serverInfo = serverInfo;
		this.capabilities = capabilities;
		this.instructions = instructions;
		this.toolRegistry = toolRegistry;
		this.resourceRegistry = resourceRegistry;
		this.promptRegistry = promptRegistry;
		this.completionRegistry = completionRegistry;
		this.async = async;
		this.subscriptionManager = new McpResourceSubscriptionManager();
	}

	McpAsyncServerImpl(
			McpServerTransportProvider transportProvider,
			McpServerInfo serverInfo,
			McpServerCapabilities capabilities,
			String instructions,
			McpToolRegistry toolRegistry,
			McpResourceRegistry resourceRegistry,
			McpPromptRegistry promptRegistry,
			McpCompletionRegistry completionRegistry,
			boolean async) {
		this.transportProvider = transportProvider;
		this.serverInfo = serverInfo;
		this.capabilities = capabilities;
		this.instructions = instructions;
		this.toolRegistry = toolRegistry;
		this.resourceRegistry = resourceRegistry;
		this.promptRegistry = promptRegistry;
		this.completionRegistry = completionRegistry;
		this.async = async;
		this.subscriptionManager = new McpResourceSubscriptionManager();
	}

	public void start() throws Exception {
		if (running) {
			logger.warn("MCP Server [{}] already running", serverInfo.name());
			return;
		}
		logger.info("Starting MCP Server: {} v{}", serverInfo.name(), serverInfo.version());
		running = true;
		startSessionCleanupThread();
	}

	public void stop() throws Exception {
		if (!running) {
			return;
		}
		logger.info("Stopping MCP Server: {}", serverInfo.name());
		running = false;
		Set<String> sessionIds = new HashSet<>(sessions.keySet());
		for (String sid : sessionIds) {
			subscriptionManager.unsubscribeAll(sid);
		}
		sessions.clear();
	}

	public boolean isRunning() {
		return running;
	}

	// ==================== 请求分发 ====================

	/**
	 * 处理 JSON-RPC 请求
	 */
	public McpJsonRpcResponse handleRequest(McpJsonRpcRequest request, String sessionId) {
		if (!running) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.internalError("Server is not running"));
		}
		if (sessionId != null) {
			McpSessionState state = sessions.get(sessionId);
			if (state == null) {
				state = new McpSessionState();
				state.lastAccess = System.currentTimeMillis();
				sessions.put(sessionId, state);
			} else {
				state.lastAccess = System.currentTimeMillis();
			}
		}
		currentSessionId.set(sessionId);
		try {
			String method = request.getMethod();
			logger.debug("Handling MCP request: {}, session: {}", method, sessionId);
			// 路由到对应的方法处理器
			if (method == null) {
				return McpJsonRpcResponse.error(request.getId(), McpJsonRpcError.invalidRequest());
			}
			switch (method) {
				case "initialize":
					return handleInitialize(request);
				case "notifications/initialized":
					return handleNotificationsInitialized(request);
				case "ping":
					return McpJsonRpcResponse.success(request.getId(), Collections.emptyMap());
				case "tools/list":
					return handleToolsList(request);
				case "tools/call":
					return handleToolsCall(request);
				case "resources/list":
					return handleResourcesList(request);
				case "resources/read":
					return handleResourcesRead(request);
				case "resources/templates/list":
					return handleResourceTemplatesList(request);
				case "resources/subscribe":
					return handleResourcesSubscribe(request);
				case "resources/unsubscribe":
					return handleResourcesUnsubscribe(request);
				case "prompts/list":
					return handlePromptsList(request);
				case "prompts/get":
					return handlePromptsGet(request);
				case "completion/complete":
					return handleCompletionComplete(request);
				case "logging/setLevel":
					return handleLoggingSetLevel(request);
				case "notifications/cancelled":
					return handleCancelled(request);
				default:
					return McpJsonRpcResponse.error(request.getId(),
							McpJsonRpcError.methodNotFound(method));
			}
		} catch (Exception e) {
			logger.error("Error handling MCP request: {}", request.getMethod(), e);
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.internalError(e.getMessage() != null ? e.getMessage() : e.toString()));
		} finally {
			currentSessionId.remove();
		}
	}

	// ==================== initialize / notifications/initialized ====================

	/**
	 * 处理 initialize 请求
	 *
	 * <p>spec 3：版本协商。接受 2025-11-25/2025-06-18，回显客户端请求版本；
	 * 其他版本返回协议错误（不创建会话）。
	 */
	private McpJsonRpcResponse handleInitialize(McpJsonRpcRequest request) {
		Map<String, Object> params = request.getParams();
		String clientProtocolVersion = null;
		if (params != null) {
			Object versionObj = params.get("protocolVersion");
			if (versionObj != null) {
				clientProtocolVersion = versionObj.toString();
			}
		}
		// 版本协商
		if (clientProtocolVersion == null
				|| !SUPPORTED_PROTOCOL_VERSIONS.contains(clientProtocolVersion)) {
			return McpJsonRpcResponse.error(request.getId(),
					new McpJsonRpcError(McpJsonRpcError.INVALID_REQUEST,
							"Unsupported protocol version: " + clientProtocolVersion
									+ "; supported versions: " + SUPPORTED_PROTOCOL_VERSIONS));
		}
		// 创建/更新会话状态
		String sessionId = currentSessionId.get();
		if (sessionId != null) {
			McpSessionState state = sessions.get(sessionId);
			if (state == null) {
				state = new McpSessionState();
				sessions.put(sessionId, state);
			}
			state.negotiatedVersion = clientProtocolVersion;
			state.lastAccess = System.currentTimeMillis();
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("protocolVersion", clientProtocolVersion);
		result.put("capabilities", capabilitiesToMap());
		Map<String, Object> serverInfoMap = new LinkedHashMap<>();
		serverInfoMap.put("name", serverInfo.name());
		serverInfoMap.put("version", serverInfo.version());
		if (serverInfo.description() != null && !serverInfo.description().isEmpty()) {
			serverInfoMap.put("description", serverInfo.description());
		}
		result.put("serverInfo", serverInfoMap);
		if (instructions != null && !instructions.isEmpty()) {
			result.put("instructions", instructions);
		}
		return McpJsonRpcResponse.success(request.getId(), result);
	}

	/**
	 * 处理 notifications/initialized 通知
	 *
	 * <p>spec 3：客户端在 initialize 之后发送该通知，标记会话已初始化。
	 * 通知无响应（返回 null）。
	 */
	private McpJsonRpcResponse handleNotificationsInitialized(McpJsonRpcRequest request) {
		String sessionId = currentSessionId.get();
		if (sessionId != null) {
			McpSessionState state = sessions.get(sessionId);
			if (state != null) {
				state.initialized = true;
				state.lastAccess = System.currentTimeMillis();
			}
		}
		return null;
	}

	// ==================== Tools ====================

	private McpJsonRpcResponse handleToolsList(McpJsonRpcRequest request) {
		if (!capabilities.tools()) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.methodNotFound("tools/list"));
		}
		Collection<McpTool> tools = toolRegistry.listTools();
		List<Map<String, Object>> toolList = new ArrayList<>();
		for (McpTool tool : tools) {
			toolList.add(tool.toMap());
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("tools", toolList);
		return McpJsonRpcResponse.success(request.getId(), result);
	}

	private McpJsonRpcResponse handleToolsCall(McpJsonRpcRequest request) {
		if (!capabilities.tools()) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.methodNotFound("tools/call"));
		}
		Map<String, Object> params = request.getParams();
		if (params == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing params"));
		}
		Object nameObj = params.get("name");
		if (nameObj == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing tool name"));
		}
		String toolName = nameObj.toString();
		McpToolProvider provider = toolRegistry.getProvider(toolName);
		if (provider == null) {
			// spec 4.2：工具未注册返回方法不存在或能力未启用错误
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.methodNotFound("Tool not found: " + toolName));
		}
		McpProgressToken progressToken = null;
		Object metaObj = params.get("_meta");
		if (metaObj instanceof Map) {
			Object tokenObj = ((Map<?, ?>) metaObj).get("progressToken");
			if (tokenObj != null) {
				progressToken = new McpProgressToken(tokenObj);
			}
		}
		try {
			McpToolCallRequest callRequest = new McpToolCallRequest();
			callRequest.setName(toolName);
			@SuppressWarnings("unchecked")
			Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
			callRequest.setArguments(arguments);
			callRequest.setProgressToken(progressToken);
			McpProviderContext context = new McpProviderContext(currentSessionId.get());
			McpToolResult result = provider.callTool(context, callRequest);
			return McpJsonRpcResponse.success(request.getId(), result.toMap());
		} catch (Exception e) {
			logger.error("Error calling tool: {}", toolName, e);
			// spec 4.2：工具执行业务失败使用 isError=true 的 tool result，不破坏 JSON-RPC 层
			McpToolResult errorResult = McpToolResult.error(
					e.getMessage() != null ? e.getMessage() : e.toString());
			return McpJsonRpcResponse.success(request.getId(), errorResult.toMap());
		}
	}

	// ==================== Resources ====================

	private McpJsonRpcResponse handleResourcesList(McpJsonRpcRequest request) {
		if (!capabilities.resources()) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.methodNotFound("resources/list"));
		}
		Collection<McpResourceInfo> resources = resourceRegistry.listResources(currentSessionId.get());
		List<Map<String, Object>> resourceList = new ArrayList<>();
		for (McpResourceInfo resource : resources) {
			resourceList.add(resource.toMap());
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("resources", resourceList);
		return McpJsonRpcResponse.success(request.getId(), result);
	}

	private McpJsonRpcResponse handleResourcesRead(McpJsonRpcRequest request) {
		if (!capabilities.resources()) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.methodNotFound("resources/read"));
		}
		Map<String, Object> params = request.getParams();
		if (params == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing params"));
		}
		Object uriObj = params.get("uri");
		if (uriObj == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing uri"));
		}
		String uri = uriObj.toString();
		McpResourceProvider provider = resourceRegistry.getResourceProvider(uri);
		if (provider == null) {
			// 模板展开
			String templateKey = resourceRegistry.findTemplateMatch(uri);
			if (templateKey != null) {
				provider = resourceRegistry.getTemplateProvider(templateKey);
			}
		}
		if (provider == null) {
			// spec 6.2：URI 无对应提供者时返回规范错误
			return McpJsonRpcResponse.error(request.getId(),
					new McpJsonRpcError(McpJsonRpcError.RESOURCE_NOT_FOUND,
							"Resource not found: " + uri));
		}
		try {
			McpResourceReadRequest readRequest = McpResourceReadRequest.of(uri);
			McpProviderContext context = new McpProviderContext(currentSessionId.get());
			McpResourceContent content = provider.readResource(context, readRequest);
			Map<String, Object> result = new LinkedHashMap<>();
			List<Map<String, Object>> contents = new ArrayList<>();
			contents.add(content.toMap());
			result.put("contents", contents);
			return McpJsonRpcResponse.success(request.getId(), result);
		} catch (Exception e) {
			logger.error("Error reading resource: {}", uri, e);
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.internalError(
							e.getMessage() != null ? e.getMessage() : e.toString()));
		}
	}

	private McpJsonRpcResponse handleResourceTemplatesList(McpJsonRpcRequest request) {
		if (!capabilities.resources()) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.methodNotFound("resources/templates/list"));
		}
		Collection<McpResourceTemplate> templates = resourceRegistry.listResourceTemplates();
		List<Map<String, Object>> templateList = new ArrayList<>();
		for (McpResourceTemplate template : templates) {
			templateList.add(template.toMap());
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("resourceTemplates", templateList);
		return McpJsonRpcResponse.success(request.getId(), result);
	}

	private McpJsonRpcResponse handleResourcesSubscribe(McpJsonRpcRequest request) {
		if (!capabilities.resources()) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.methodNotFound("resources/subscribe"));
		}
		Map<String, Object> params = request.getParams();
		if (params == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing params"));
		}
		Object uriObj = params.get("uri");
		if (uriObj == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing uri"));
		}
		String uri = uriObj.toString();
		String sessionId = currentSessionId.get();
		subscriptionManager.subscribe(sessionId, uri);
		logger.debug("Session {} subscribed to resource: {}", sessionId, uri);
		return McpJsonRpcResponse.success(request.getId(), Collections.emptyMap());
	}

	private McpJsonRpcResponse handleResourcesUnsubscribe(McpJsonRpcRequest request) {
		if (!capabilities.resources()) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.methodNotFound("resources/unsubscribe"));
		}
		Map<String, Object> params = request.getParams();
		if (params == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing params"));
		}
		Object uriObj = params.get("uri");
		if (uriObj == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing uri"));
		}
		String uri = uriObj.toString();
		String sessionId = currentSessionId.get();
		subscriptionManager.unsubscribe(sessionId, uri);
		logger.debug("Session {} unsubscribed from resource: {}", sessionId, uri);
		return McpJsonRpcResponse.success(request.getId(), Collections.emptyMap());
	}

	// ==================== Prompts ====================

	private McpJsonRpcResponse handlePromptsList(McpJsonRpcRequest request) {
		if (!capabilities.prompts()) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.methodNotFound("prompts/list"));
		}
		Collection<McpPromptInfo> prompts = promptRegistry.listPrompts();
		List<Map<String, Object>> promptList = new ArrayList<>();
		for (McpPromptInfo prompt : prompts) {
			promptList.add(prompt.toMap());
		}
		Map<String, Object> result = new LinkedHashMap<>();
		result.put("prompts", promptList);
		return McpJsonRpcResponse.success(request.getId(), result);
	}

	private McpJsonRpcResponse handlePromptsGet(McpJsonRpcRequest request) {
		if (!capabilities.prompts()) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.methodNotFound("prompts/get"));
		}
		Map<String, Object> params = request.getParams();
		if (params == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing params"));
		}
		Object nameObj = params.get("name");
		if (nameObj == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing prompt name"));
		}
		String name = nameObj.toString();
		McpPromptProvider provider = promptRegistry.getProvider(name);
		if (provider == null) {
			return McpJsonRpcResponse.error(request.getId(),
					new McpJsonRpcError(McpJsonRpcError.RESOURCE_NOT_FOUND,
							"Prompt not found: " + name));
		}
		// spec 6.3：缺失必填参数返回参数无效错误
		McpPromptInfo info = promptRegistry.getPrompt(name);
		if (info != null && info.arguments() != null) {
			@SuppressWarnings("unchecked")
			Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
			if (arguments == null) {
				arguments = Collections.emptyMap();
			}
			for (com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptArgument arg : info.arguments()) {
				if (arg.required() && !arguments.containsKey(arg.name())) {
					return McpJsonRpcResponse.error(request.getId(),
							McpJsonRpcError.invalidParams("Missing required argument: " + arg.name()));
				}
			}
		}
		try {
			McpPromptRequest promptRequest = new McpPromptRequest();
			promptRequest.setName(name);
			@SuppressWarnings("unchecked")
			Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
			promptRequest.setArguments(arguments);
			McpProviderContext context = new McpProviderContext(currentSessionId.get());
			McpPromptResult result = provider.getPrompt(context, promptRequest);
			return McpJsonRpcResponse.success(request.getId(), result.toMap());
		} catch (Exception e) {
			logger.error("Error getting prompt: {}", name, e);
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.internalError(
							e.getMessage() != null ? e.getMessage() : e.toString()));
		}
	}

	// ==================== Completion ====================

	/**
	 * 处理 completion/complete 请求
	 *
	 * <p>spec 6.4：ref 为对象 {type, name/uri}，argument 为 {name, value}。
	 */
	private McpJsonRpcResponse handleCompletionComplete(McpJsonRpcRequest request) {
		if (!capabilities.completions()) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.methodNotFound("completion/complete"));
		}
		Map<String, Object> params = request.getParams();
		if (params == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing params"));
		}
		Object refObj = params.get("ref");
		if (!(refObj instanceof Map)) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing or invalid ref"));
		}
		@SuppressWarnings("unchecked")
		Map<String, Object> refMap = (Map<String, Object>) refObj;
		Object typeObj = refMap.get("type");
		if (typeObj == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing ref.type"));
		}
		String refType = typeObj.toString();
		McpCompletionRef ref;
		if (McpCompletionRef.TYPE_PROMPT.equals(refType)) {
			Object nameVal = refMap.get("name");
			ref = McpCompletionRef.prompt(nameVal != null ? nameVal.toString() : "");
		} else if (McpCompletionRef.TYPE_RESOURCE.equals(refType)) {
			Object uriVal = refMap.get("uri");
			ref = McpCompletionRef.resource(uriVal != null ? uriVal.toString() : "");
		} else {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Unsupported ref.type: " + refType));
		}
		String argumentName = null;
		String argumentValue = null;
		Object argumentObj = params.get("argument");
		if (argumentObj instanceof Map) {
			@SuppressWarnings("unchecked")
			Map<String, Object> argument = (Map<String, Object>) argumentObj;
			Object nameVal = argument.get("name");
			Object valueVal = argument.get("value");
			argumentName = nameVal != null ? nameVal.toString() : null;
			argumentValue = valueVal != null ? valueVal.toString() : null;
		}
		try {
			McpCompletionRequest completionRequest = new McpCompletionRequest(
					ref, argumentName, argumentValue);
			McpProviderContext context = new McpProviderContext(currentSessionId.get());
			McpCompletionResult result = completionRegistry.complete(context, completionRequest);
			return McpJsonRpcResponse.success(request.getId(), result.toMap());
		} catch (Exception e) {
			logger.error("Error completing: {}", refType, e);
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.internalError(
							e.getMessage() != null ? e.getMessage() : e.toString()));
		}
	}

	// ==================== Logging ====================

	private McpJsonRpcResponse handleLoggingSetLevel(McpJsonRpcRequest request) {
		Map<String, Object> params = request.getParams();
		if (params == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing params"));
		}
		Object levelObj = params.get("level");
		if (levelObj == null) {
			return McpJsonRpcResponse.error(request.getId(),
					McpJsonRpcError.invalidParams("Missing level"));
		}
		String level = levelObj.toString();
		// 按会话存储日志级别
		String sessionId = currentSessionId.get();
		if (sessionId != null) {
			McpSessionState state = sessions.get(sessionId);
			if (state != null) {
				state.logLevel = level;
			}
		}
		logger.debug("Session {} set log level: {}", sessionId, level);
		return McpJsonRpcResponse.success(request.getId(), Collections.emptyMap());
	}

	/**
	 * 发送日志消息通知给客户端
	 *
	 * <p>spec 6.5：按会话级别通过 notifications/message 下发日志（logger、data）；
	 * 未设置时按默认级别。
	 */
	public void sendLoggingMessageNotification(String loggerName, String level, Object data) {
		if (!capabilities.logging()) {
			return;
		}
		for (Map.Entry<String, McpSessionState> entry : sessions.entrySet()) {
			String sessionId = entry.getKey();
			McpSessionState state = entry.getValue();
			String sessionLevel = state.logLevel != null ? state.logLevel : DEFAULT_LOG_LEVEL;
			if (!shouldLog(sessionLevel, level)) {
				continue;
			}
			Map<String, Object> notification = new LinkedHashMap<>();
			notification.put("level", level);
			if (loggerName != null) {
				notification.put("logger", loggerName);
			}
			notification.put("data", data);
			sendNotification(sessionId, "notifications/message", notification);
		}
	}

	private boolean shouldLog(String sessionLevel, String messageLevel) {
		int session = logLevelOrder(sessionLevel);
		int message = logLevelOrder(messageLevel);
		return message >= session;
	}

	private int logLevelOrder(String level) {
		if (level == null) return 2; // info
		switch (level.toLowerCase()) {
			case "debug": return 0;
			case "info": return 1;
			case "notice": return 2;
			case "warning": return 3;
			case "error": return 4;
			case "critical": return 5;
			case "alert": return 6;
			case "emergency": return 7;
			default: return 2;
		}
	}

	// ==================== Cancelled ====================

	/**
	 * 处理 notifications/cancelled 通知
	 *
	 * <p>spec：客户端通知服务器取消正在运行的工具调用。
	 * 当前为最小实现，仅记录日志；后续可扩展为通过 Thread.interrupt 中断。
	 */
	private McpJsonRpcResponse handleCancelled(McpJsonRpcRequest request) {
		Map<String, Object> params = request.getParams();
		String reason = null;
		if (params != null) {
			Object reasonObj = params.get("reason");
			reason = reasonObj != null ? reasonObj.toString() : "Cancelled by client";
		} else {
			reason = "Cancelled by client";
		}
		logger.info("Tool call cancelled by client: {}", reason);
		return null; // 通知无响应
	}

	// ==================== 服务端通知 ====================

	/**
	 * 发送通知到指定会话
	 */
	private void sendNotification(String sessionId, String method, Object params) {
		if (transportProvider instanceof McpStreamableServerTransportProvider) {
			try {
				if (sessionId != null) {
					((McpStreamableServerTransportProvider) transportProvider)
							.notifyClient(sessionId, method, params);
				} else {
					((McpStreamableServerTransportProvider) transportProvider)
							.notifyClients(method, params);
				}
			} catch (Exception e) {
				logger.error("Failed to send notification [{}] to session {}", method, sessionId, e);
			}
		} else if (transportProvider instanceof McpServerTransportProvider) {
			// Classic 单连接 transport 由具体实现负责发送
			logger.debug("Notification [{}] queued via McpServerTransportProvider", method);
		}
	}

	/**
	 * 通知所有会话工具列表已变更
	 *
	 * <p>spec 6.1：notifications/tools/list_changed
	 */
	public void notifyToolsListChanged() {
		if (!capabilities.tools()) {
			return;
		}
		for (String sessionId : sessions.keySet()) {
			sendNotification(sessionId, "notifications/tools/list_changed", Collections.emptyMap());
		}
		logger.info("Notified sessions: tools list changed");
	}

	/**
	 * 通知所有会话资源列表已变更
	 *
	 * <p>spec 6.2：notifications/resources/list_changed
	 */
	public void notifyResourcesListChanged() {
		if (!capabilities.resources()) {
			return;
		}
		for (String sessionId : sessions.keySet()) {
			sendNotification(sessionId, "notifications/resources/list_changed", Collections.emptyMap());
		}
		logger.info("Notified sessions: resources list changed");
	}

	/**
	 * 通知所有会话提示词列表已变更
	 *
	 * <p>spec 6.3：notifications/prompts/list_changed
	 */
	public void notifyPromptsListChanged() {
		if (!capabilities.prompts()) {
			return;
		}
		for (String sessionId : sessions.keySet()) {
			sendNotification(sessionId, "notifications/prompts/list_changed", Collections.emptyMap());
		}
		logger.info("Notified sessions: prompts list changed");
	}

	/**
	 * 通知订阅会话指定资源已更新
	 *
	 * <p>spec 6.2：notifications/resources/updated
	 */
	public void notifyResourceUpdated(String uri) {
		if (!capabilities.resources()) {
			return;
		}
		Set<String> subscribers = subscriptionManager.getSubscribers(uri);
		if (subscribers == null || subscribers.isEmpty()) {
			return;
		}
		Map<String, Object> params = new LinkedHashMap<>();
		params.put("uri", uri);
		for (String sessionId : subscribers) {
			sendNotification(sessionId, "notifications/resources/updated", params);
		}
		logger.debug("Notified {} subscribers: resource updated {}", subscribers.size(), uri);
	}

	// ==================== 辅助方法 ====================

	/**
	 * 启动会话清理线程
	 */
	private void startSessionCleanupThread() {
		Thread cleanupThread = new Thread(() -> {
			while (running) {
				try {
					Thread.sleep(5L * 60L * 1000L);
					cleanupExpiredSessions();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (Exception e) {
					logger.error("Error in session cleanup thread", e);
				}
			}
		}, "mcp-session-cleanup");
		cleanupThread.setDaemon(true);
		cleanupThread.start();
	}

	/**
	 * 清理过期会话
	 */
	private void cleanupExpiredSessions() {
		long now = System.currentTimeMillis();
		List<String> expired = new ArrayList<>();
		for (Map.Entry<String, McpSessionState> entry : sessions.entrySet()) {
			if (now - entry.getValue().lastAccess > SESSION_TIMEOUT_MS) {
				expired.add(entry.getKey());
			}
		}
		for (String sessionId : expired) {
			sessions.remove(sessionId);
			subscriptionManager.unsubscribeAll(sessionId);
			logger.info("Cleaned up expired session: {}", sessionId);
		}
	}

	/**
	 * 将服务器能力转换为 JSON-RPC 响应 Map
	 *
	 * <p>spec 3.2：仅声明实例实际开启的能力。
	 */
	private Map<String, Object> capabilitiesToMap() {
		Map<String, Object> map = new LinkedHashMap<>();
		if (capabilities.logging()) {
			map.put("logging", Collections.emptyMap());
		}
		if (capabilities.tools()) {
			Map<String, Object> tools = new LinkedHashMap<>();
			tools.put("listChanged", true);
			map.put("tools", tools);
		}
		if (capabilities.resources()) {
			Map<String, Object> resources = new LinkedHashMap<>();
			resources.put("subscribe", true);
			resources.put("listChanged", true);
			map.put("resources", resources);
		}
		if (capabilities.prompts()) {
			Map<String, Object> prompts = new LinkedHashMap<>();
			prompts.put("listChanged", true);
			map.put("prompts", prompts);
		}
		// spec 9.4：completions 能力声明可选，此处省略
		return map;
	}

	/**
	 * 会话状态
	 */
	private static final class McpSessionState {
		/** 协商的协议版本 */
		String negotiatedVersion;
		/** 是否完成 notifications/initialized */
		boolean initialized;
		/** 当前日志级别 */
		String logLevel;
		/** 最后访问时间（毫秒） */
		long lastAccess;
	}
}
