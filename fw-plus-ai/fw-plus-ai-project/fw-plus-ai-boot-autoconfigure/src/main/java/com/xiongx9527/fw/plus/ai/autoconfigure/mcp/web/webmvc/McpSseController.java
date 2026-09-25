package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web.webmvc;

import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerSseRegistry;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

/**
 * HTTP/SSE 传输控制器。
 * <p>
 * 路径由 {@link McpRoutingRegistrar} 编程式注册：
 * {@code GET {endpoint}/sse} 建立事件流并下发消息端点；{@code POST {endpoint}/messages/{uuid}}
 * 投递 JSON-RPC 消息（uuid 为动态路径段，会话归属先经本 runtime 会话表校验）。
 * </p>
 */
public class McpSseController extends McpTransportController {

	public static final String SSE_PATH = "/sse";

	public static final String MESSAGES_PATH = "/messages/{uuid}";

	public McpSseController(McpServerRuntime runtime) {
		super(runtime);
	}

	/**
	 * 建立 SSE 事件流，并下发客户端后续 POST 消息使用的端点路径。
	 */
	public void streamSse(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ServerSseRegistry sse = sseRegistry();
		if (sse == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if (!sse.isOriginAllowed(request.getHeader("Origin"))) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		response.setContentType(MediaType.TEXT_EVENT_STREAM_VALUE);
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Connection", "keep-alive");

		String uuid = UUID.randomUUID().toString();
		PrintWriter writer = response.getWriter();

		try {
			sse.openSession(uuid, writer, endpoint() + "/messages/" + uuid);

			// 连接生命周期由内核维护，这里只负责持有写通道直至对端断开。
			while (sse.isSessionOpen(uuid) && !Thread.currentThread().isInterrupted()) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
		} finally {
			sse.removeConnection(uuid);
		}
	}

	/**
	 * 接收 SSE 客户端的 JSON-RPC 消息，响应经事件流回送给该会话。
	 */
	public void handleMessage(@RequestBody String json, @PathVariable("uuid") String uuid,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ServerSseRegistry sse = sseRegistry();
		if (sse == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		if (!sse.isOriginAllowed(request.getHeader("Origin"))) {
			response.sendError(HttpServletResponse.SC_FORBIDDEN);
			return;
		}
		// 会话归属校验：uuid 必须属于本 runtime，否则存在跨 runtime 串台，直接失效。
		if (!belongsToRuntime(uuid)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (!isAuthenticated(uuid, "http-sse", endpoint(), json, request, response)) {
			return;
		}

		try {
			sse.handle(uuid, json);
		} catch (JsonRpcErrorException e) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType(MediaType.APPLICATION_JSON_VALUE);
			response.getWriter().write(e.toJson());
		}
	}
}
