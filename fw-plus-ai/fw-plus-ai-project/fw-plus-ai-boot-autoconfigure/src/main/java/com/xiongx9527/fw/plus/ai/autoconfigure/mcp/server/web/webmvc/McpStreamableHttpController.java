package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.web.webmvc;

import com.ajaxjs.mcp.server.model.HttpResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerStreamableHttpRegistry;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Streamable HTTP 传输控制器。
 * <p>
 * 所有 JSON-RPC 消息走同一个端点 {@code {endpoint}}：POST 投递消息，DELETE 终止会话，
 * GET 建立服务器主动推送的事件流。请求经会话归属校验（{@code Mcp-Session-Id} 必须属于
 * 本 runtime）后才进内核。
 * </p>
 */
public class McpStreamableHttpController extends McpTransportController {

	public McpStreamableHttpController(McpServerRuntime runtime) {
		super(runtime);
	}

	/**
	 * Streamable HTTP 消息入口。
	 */
	public void post(@RequestBody(required = false) String body, HttpServletRequest request,
			HttpServletResponse response) throws IOException {
		ServerStreamableHttpRegistry transport = streamableRegistry();
		if (transport == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String session = request.getHeader(sessionIdHeaderName());
		// 初始化请求没有会话头；已持会话的后续消息必须归属本 runtime。
		if (session != null && !belongsToRuntime(session)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String payload = body == null ? "" : body;
		if (!isAuthenticated(session, "streamable-http", endpoint(), payload, request, response)) {
			return;
		}
		write(transport.post(payload, requestHeaders(request)), response);
	}

	/**
	 * 终止一个 Streamable HTTP 会话。
	 */
	public void delete(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ServerStreamableHttpRegistry transport = streamableRegistry();
		if (transport == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String session = request.getHeader(sessionIdHeaderName());
		if (session != null && !belongsToRuntime(session)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (!isAuthenticated(session, "streamable-http", endpoint(), null, request, response)) {
			return;
		}
		write(transport.delete(session, requestHeaders(request)), response);
	}

	/**
	 * 建立 Streamable HTTP 的 GET 事件流，用于服务器主动推送。
	 */
	public void get(HttpServletRequest request, HttpServletResponse response) throws IOException {
		ServerStreamableHttpRegistry transport = streamableRegistry();
		if (transport == null) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		String session = request.getHeader(sessionIdHeaderName());
		if (!belongsToRuntime(session)) {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}

		if (!isAuthenticated(session, "streamable-http", endpoint(), null, request, response)) {
			return;
		}

		response.setCharacterEncoding("UTF-8");
		javax.servlet.AsyncContext async = request.startAsync();
		// 会话过期与心跳由内核负责，适配层不设置异步超时。
		async.setTimeout(0);
		AtomicBoolean ended = new AtomicBoolean();
		PrintWriter writer = new PrintWriter(response.getWriter()) {
			@Override
			public void close() {
				if (ended.compareAndSet(false, true)) {
					super.close();
					try {
						async.complete();
					} catch (IllegalStateException ignored) {
						// 异步上下文已由其它路径结束
					}
				}
			}
		};

		async.addListener(new javax.servlet.AsyncListener() {
			private void cleanup() {
				if (session != null)
					transport.closeEventStream(session, writer);
				writer.close();
			}

			@Override
			public void onComplete(javax.servlet.AsyncEvent event) {
				cleanup();
			}

			@Override
			public void onTimeout(javax.servlet.AsyncEvent event) {
				cleanup();
			}

			@Override
			public void onError(javax.servlet.AsyncEvent event) {
				cleanup();
			}

			@Override
			public void onStartAsync(javax.servlet.AsyncEvent event) {
				// 无需处理
			}
		});

		try {
			// 心跳与适配层并发提交响应头时会互相覆盖，需要与内核同步。
			synchronized (transport) {
				HttpResult result = transport.openEventStream(session, writer, requestHeaders(request));
				response.setHeader("Cache-Control", "no-cache");
				response.setHeader("X-Accel-Buffering", "no");
				write(result, response);
				response.flushBuffer();

				if (result.getStatus() != HttpServletResponse.SC_OK)
					writer.close();
			}
		} catch (IOException | RuntimeException failure) {
			if (session != null)
				transport.closeEventStream(session, writer);

			writer.close();
			throw failure;
		}
	}
}
