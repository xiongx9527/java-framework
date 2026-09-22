package com.xiongx9527.fw.plus.ai.mcp.transport.servlet;

import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcError;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;
import com.xiongx9527.fw.plus.ai.mcp.transport.AbstractStreamableTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Spring MVC（Servlet）Streamable HTTP 端点
 *
 * <p>一个实例对应一个端点对象与一条端点路径（spec 5：支持多实例，每个实例独立名称与路径）。
 * 映射由 {@link McpServletEndpointRegistrar} 在上下文就绪后按实例路径注册，因此本类
 * <b>不带</b> {@code @RequestMapping} 注解，避免固定路径限制多实例能力。
 *
 * <p>协议行为（spec 5.1）：
 * <ul>
 *   <li>{@code POST}：JSON-RPC 请求 → JSON 响应；{@code initialize} 且无会话头时新建会话并回写
 *       {@code Mcp-Session-Id}；通知类请求返回 {@code 202}；解析失败返回 {@code -32700}；</li>
 *   <li>{@code GET}：建立/恢复该会话的 SSE 流（{@link SseEmitter}），支持 {@code Last-Event-ID} 恢复；</li>
 *   <li>{@code DELETE}：显式结束会话并关闭 SSE 流；</li>
 *   <li>缺失/失效会话头分别返回 {@code 400}/{@code 404}，非法 {@code Origin} 返回 {@code 403}。</li>
 * </ul>
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
@RestController
public class McpServletEndpoint {

	private static final Logger logger = LoggerFactory.getLogger(McpServletEndpoint.class);

	private static final String INITIALIZE_METHOD = "initialize";

	private final McpServletStreamableTransport transport;

	public McpServletEndpoint(McpServletStreamableTransport transport) {
		this.transport = transport;
	}

	public McpServletStreamableTransport getTransport() {
		return transport;
	}

	public String getPath() {
		return transport.getPath();
	}

	/**
	 * POST：接收 JSON-RPC 请求并同步返回结果
	 */
	public ResponseEntity<McpJsonRpcResponse> handlePost(HttpServletRequest request) throws IOException {
		if (!isOriginAllowed(request)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		String body = readBody(request);
		McpJsonRpcRequest rpcRequest = transport.parseRequest(body);
		if (rpcRequest == null) {
			return ResponseEntity.ok(McpJsonRpcResponse.error(null, McpJsonRpcError.parseError()));
		}
		if (rpcRequest.getMethod() == null) {
			return ResponseEntity.ok(McpJsonRpcResponse.error(rpcRequest.getId(), McpJsonRpcError.invalidRequest()));
		}

		String sessionId = transport.resolveSessionId(request.getHeader(AbstractStreamableTransport.SESSION_ID_HEADER));
		if (sessionId == null) {
			// 未携带会话头的请求：只有 initialize 允许创建新会话
			if (!INITIALIZE_METHOD.equals(rpcRequest.getMethod())) {
				return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
			}
			return handleInitialize(rpcRequest);
		}
		if (!transport.hasSession(sessionId)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		McpJsonRpcResponse response = transport.handlePost(sessionId, rpcRequest);
		if (response == null) {
			// 通知/响应类请求：接受处理，无 JSON-RPC 响应体
			return ResponseEntity.accepted().build();
		}
		return ResponseEntity.ok(response);
	}

	/**
	 * GET：建立/恢复会话 SSE 流
	 */
	public SseEmitter handleGet(HttpServletRequest request) {
		if (!isOriginAllowed(request)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		String sessionId = transport.resolveSessionId(request.getHeader(AbstractStreamableTransport.SESSION_ID_HEADER));
		if (sessionId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		String lastEventId = request.getHeader("Last-Event-ID");
		SseEmitter emitter = transport.createEmitter(sessionId, lastEventId);
		if (emitter == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return emitter;
	}

	/**
	 * DELETE：显式结束会话
	 */
	public void handleDelete(HttpServletRequest request, HttpServletResponse response) {
		if (!isOriginAllowed(request)) {
			response.setStatus(HttpStatus.FORBIDDEN.value());
			return;
		}
		String sessionId = transport.resolveSessionId(request.getHeader(AbstractStreamableTransport.SESSION_ID_HEADER));
		if (sessionId == null) {
			response.setStatus(HttpStatus.BAD_REQUEST.value());
			return;
		}
		if (!transport.hasSession(sessionId)) {
			response.setStatus(HttpStatus.NOT_FOUND.value());
			return;
		}
		transport.handleDelete(sessionId);
		response.setStatus(HttpStatus.NO_CONTENT.value());
	}

	// ==================== 内部方法 ====================

	private ResponseEntity<McpJsonRpcResponse> handleInitialize(McpJsonRpcRequest rpcRequest) {
		String sessionId = transport.createSession();
		McpJsonRpcResponse response = transport.handlePost(sessionId, rpcRequest);
		if (response == null || response.getError() != null) {
			// 初始化失败（如版本协商失败）不保留会话
			transport.closeSession(sessionId);
			return ResponseEntity.ok(response == null
					? McpJsonRpcResponse.error(rpcRequest.getId(), McpJsonRpcError.internalError("Initialize failed"))
					: response);
		}
		logger.debug("MCP instance '{}' initialized session {}", transport.getName(), sessionId);
		return ResponseEntity.ok()
				.header(AbstractStreamableTransport.SESSION_ID_HEADER, sessionId)
				.body(response);
	}

	private boolean isOriginAllowed(HttpServletRequest request) {
		String origin = request.getHeader("Origin");
		boolean allowed = transport.isOriginAllowed(origin, request.getHeader("Host"));
		if (!allowed) {
			logger.debug("Rejected request from invalid Origin '{}' on MCP instance '{}'", origin, transport.getName());
		}
		return allowed;
	}

	private String readBody(HttpServletRequest request) throws IOException {
		StringBuilder body = new StringBuilder();
		try (BufferedReader reader = request.getReader()) {
			char[] buffer = new char[1024];
			int read;
			while ((read = reader.read(buffer)) != -1) {
				body.append(buffer, 0, read);
			}
		}
		return body.toString();
	}
}
