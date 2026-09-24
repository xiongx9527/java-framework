package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web;

import com.ajaxjs.mcp.server.ServerStreamableHttp;
import com.ajaxjs.mcp.server.model.HttpResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpPrincipal;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerStreamableHttpRegistry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * MCP 传输控制器公共基类，只负责 HTTP 适配，JSON-RPC 分发与会话策略仍由 {@code fw-plus-ai-mcp} 内核承担。
 * <p>
 * 具体端点由 {@link McpRoutingRegistrar} 按 runtime 编程式注册（无注解路径），因此这里持有单个
 * runtime 实例而非从上下文查找全局单例：SSE 走 {@link McpSseController}，Streamable 走
 * {@link McpStreamableHttpController}。
 * </p>
 */
public abstract class McpTransportController {
	private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(McpTransportController.class);

	private final McpServerRuntime runtime;

	protected McpTransportController(McpServerRuntime runtime) {
		this.runtime = runtime;
	}

	protected McpServerRuntime runtime() {
		return runtime;
	}

	/**
	 * 返回配置的 MCP 端点基础路径。
	 */
	protected String endpoint() {
		return runtime.getEndpoint();
	}

	/**
	 * 当前 runtime 的 SSE 注册表，传输未启用时返回 {@code null}。
	 */
	protected ServerSseRegistry sseRegistry() {
		if (runtime.getRegistry() instanceof ServerSseRegistry) {
			return (ServerSseRegistry) runtime.getRegistry();
		}
		return null;
	}

	/**
	 * 当前 runtime 的 Streamable HTTP 注册表，传输未启用时返回 {@code null}。
	 */
	protected ServerStreamableHttpRegistry streamableRegistry() {
		if (runtime.getRegistry() instanceof ServerStreamableHttpRegistry) {
			return (ServerStreamableHttpRegistry) runtime.getRegistry();
		}
		return null;
	}

	protected static Map<String, String> headers(HttpServletRequest request) {
		Map<String, String> values = new LinkedHashMap<>();
		Collections.list(request.getHeaderNames()).forEach(name -> values.put(name, request.getHeader(name)));
		return values;
	}

	protected static void write(HttpResult result, HttpServletResponse response) throws IOException {
		response.setStatus(result.getStatus());
		response.setCharacterEncoding("UTF-8");
		result.getHeaders().forEach(response::setHeader);

		if (result.getContentType() != null)
			response.setContentType(result.getContentType());

		// 通知没有 JSON 响应体，不能写成字符串 "null"。
		if (result.getBody() != null)
			response.getWriter().write(result.getBody());
	}

	static org.slf4j.Logger logger() {
		return log;
	}

	/**
	 * Streamable HTTP 会话头名，供子控制器读取。
	 */
	protected static String sessionIdHeader() {
		return ServerStreamableHttp.SESSION_ID_HEADER;
	}

	/**
	 * 会话归属校验：uuid / {@code Mcp-Session-Id} 必须属于本 runtime 的会话表，
	 * 否则视为伪造/过期/已移除的跨 runtime 串台调用，直接失效不进内核。
	 *
	 * @param sessionId 会话 id
	 * @return 是否合法
	 */
	protected boolean belongsToRuntime(String sessionId) {
		return sessionId != null && runtime.hasSession(sessionId);
	}

	/**
	 * 认证 SPI 占位调用点：默认放行，后续认证 change 只替换实现不改签名。
	 *
	 * @param sessionId 会话 id
	 * @param transport 传输类型
	 * @param endpoint  端点
	 * @param body      请求体
	 * @param request   HTTP 请求，用于提取头部
	 * @return 认证结果主体
	 */
	protected McpPrincipal authenticate(String sessionId, String transport, String endpoint, String body,
			HttpServletRequest request) {
		McpAuthService authService = runtime.getAuthService();
		if (authService == null) {
			return McpPrincipal.ANONYMOUS;
		}
		return authService.authenticate(new McpRequest(sessionId, transport, endpoint, body, headers(request)));
	}
}
