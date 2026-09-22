package com.xiongx9527.fw.plus.ai.mcp.transport.reactive;

import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcError;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;
import com.xiongx9527.fw.plus.ai.mcp.transport.AbstractStreamableTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * Spring WebFlux（Reactive）Streamable HTTP 端点
 *
 * <p>与 {@code McpServletEndpoint} 协议行为完全一致，差别只在响应式类型：
 * {@code POST} 返回 {@code Mono<ResponseEntity<McpJsonRpcResponse>>}，
 * {@code GET} 返回 {@code Flux<ServerSentEvent<String>>}（spec 5.3）。
 *
 * <p>一个实例对应一个端点对象与一条端点路径，映射由
 * {@link McpReactiveEndpointRegistrar} 在上下文就绪后按实例路径注册。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
@RestController
public class McpReactiveEndpoint {

	private static final Logger logger = LoggerFactory.getLogger(McpReactiveEndpoint.class);

	private static final String INITIALIZE_METHOD = "initialize";

	private final McpReactiveStreamableTransport transport;

	public McpReactiveEndpoint(McpReactiveStreamableTransport transport) {
		this.transport = transport;
	}

	public McpReactiveStreamableTransport getTransport() {
		return transport;
	}

	public String getPath() {
		return transport.getPath();
	}

	/**
	 * POST：接收 JSON-RPC 请求并返回结果
	 */
	public Mono<ResponseEntity<McpJsonRpcResponse>> handlePost(ServerWebExchange exchange) {
		if (!isOriginAllowed(exchange)) {
			return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
		}
		return readBody(exchange).map(body -> dispatch(exchange, body));
	}

	/**
	 * GET：建立/恢复会话 SSE 流
	 */
	public Flux<ServerSentEvent<String>> handleGet(ServerWebExchange exchange) {
		if (!isOriginAllowed(exchange)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		String sessionId = sessionId(exchange);
		if (sessionId == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
		}
		Flux<ServerSentEvent<String>> stream =
				transport.createEventStream(sessionId, exchange.getRequest().getHeaders().getFirst("Last-Event-ID"));
		if (stream == null) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND);
		}
		return stream;
	}

	/**
	 * DELETE：显式结束会话
	 */
	public Mono<ResponseEntity<Void>> handleDelete(ServerWebExchange exchange) {
		if (!isOriginAllowed(exchange)) {
			return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN).build());
		}
		String sessionId = sessionId(exchange);
		if (sessionId == null) {
			return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).build());
		}
		if (!transport.hasSession(sessionId)) {
			return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
		}
		transport.handleDelete(sessionId);
		return Mono.just(ResponseEntity.noContent().build());
	}

	// ==================== 内部方法 ====================

	private ResponseEntity<McpJsonRpcResponse> dispatch(ServerWebExchange exchange, String body) {
		McpJsonRpcRequest rpcRequest = transport.parseRequest(body);
		if (rpcRequest == null) {
			return ResponseEntity.ok(McpJsonRpcResponse.error(null, McpJsonRpcError.parseError()));
		}
		if (rpcRequest.getMethod() == null) {
			return ResponseEntity.ok(McpJsonRpcResponse.error(rpcRequest.getId(), McpJsonRpcError.invalidRequest()));
		}
		String sessionId = sessionId(exchange);
		if (sessionId == null) {
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
			return ResponseEntity.accepted().build();
		}
		return ResponseEntity.ok(response);
	}

	private ResponseEntity<McpJsonRpcResponse> handleInitialize(McpJsonRpcRequest rpcRequest) {
		String sessionId = transport.createSession();
		McpJsonRpcResponse response = transport.handlePost(sessionId, rpcRequest);
		if (response == null || response.getError() != null) {
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

	/**
	 * 读取请求体原始字符串（UTF-8），与 MVC 版端点行为对齐
	 */
	private Mono<String> readBody(ServerWebExchange exchange) {
		return DataBufferUtils.join(exchange.getRequest().getBody())
				.map(dataBuffer -> {
					byte[] bytes = new byte[dataBuffer.readableByteCount()];
					dataBuffer.read(bytes);
					DataBufferUtils.release(dataBuffer);
					return new String(bytes, StandardCharsets.UTF_8);
				})
				.defaultIfEmpty("");
	}

	private String sessionId(ServerWebExchange exchange) {
		return transport.resolveSessionId(
				exchange.getRequest().getHeaders().getFirst(AbstractStreamableTransport.SESSION_ID_HEADER));
	}

	private boolean isOriginAllowed(ServerWebExchange exchange) {
		String origin = exchange.getRequest().getHeaders().getFirst("Origin");
		boolean allowed = transport.isOriginAllowed(origin, exchange.getRequest().getHeaders().getFirst("Host"));
		if (!allowed) {
			logger.debug("Rejected request from invalid Origin '{}' on MCP instance '{}'", origin, transport.getName());
		}
		return allowed;
	}
}
