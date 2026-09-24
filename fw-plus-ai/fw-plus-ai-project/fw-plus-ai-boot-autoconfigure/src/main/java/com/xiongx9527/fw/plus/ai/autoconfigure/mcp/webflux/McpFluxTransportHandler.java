package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.webflux;

import com.ajaxjs.mcp.server.ServerStreamableHttp;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.server.model.HttpResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerStreamableHttpRegistry;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * WebFlux 传输处理器：SSE 与 Streamable HTTP 两套路径布局，语义与 MVC 控制器一致。
 *
 * <p>同步内核调用一律 {@link Mono#fromCallable} 后 {@code subscribeOn} 专用池，不阻塞
 * event loop；SSE 通过 {@link SinkWriter} 把内核帧桥接到 {@code Flux<String>}。</p>
 */
class McpFluxTransportHandler {

	private static final String STREAMABLE_TRANSPORT = "streamable-http";

	private static final int OVERFLOW_BUFFER_SIZE = 4096;

	private final McpServerRuntime runtime;

	private final Scheduler pool;

	McpFluxTransportHandler(McpServerRuntime runtime, Scheduler pool) {
		this.runtime = runtime;
		this.pool = pool;
	}

	RouterFunction<ServerResponse> router() {
		String endpoint = runtime.getEndpoint();
		if (endpoint == null || endpoint.isEmpty()) {
			throw new IllegalStateException(
					"MCP runtime '" + runtime.getKey() + "' has an empty endpoint, cannot register routes.");
		}

		if (STREAMABLE_TRANSPORT.equals(runtime.getTransport())) {
			return RouterFunctions.route(org.springframework.web.reactive.function.server.RequestPredicates
					.POST(endpoint), this::post)
					.and(RouterFunctions.route(org.springframework.web.reactive.function.server.RequestPredicates
							.DELETE(endpoint), this::delete))
					.and(RouterFunctions.route(org.springframework.web.reactive.function.server.RequestPredicates
							.GET(endpoint), this::get));
		}

		return RouterFunctions.route(org.springframework.web.reactive.function.server.RequestPredicates
				.GET(endpoint + McpWebfluxWebBinding.SSE_SUFFIX), this::streamSse)
				.and(RouterFunctions.route(org.springframework.web.reactive.function.server.RequestPredicates
						.POST(endpoint + McpWebfluxWebBinding.MESSAGES_SUFFIX), this::handleMessage));
	}

	private Mono<ServerResponse> streamSse(ServerRequest request) {
		ServerSseRegistry sse = sseRegistry();
		if (sse == null) {
			return ServerResponse.notFound().build();
		}
		if (!sse.isOriginAllowed(request.headers().firstHeader("Origin"))) {
			return ServerResponse.status(HttpStatus.FORBIDDEN).build();
		}

		String uuid = UUID.randomUUID().toString();
		AtomicReference<String> writerHolder = new AtomicReference<>();
		Flux<String> frames = Flux.<String>create(sink -> {
			writerHolder.set(endpoint() + McpWebfluxWebBinding.MESSAGES_SUFFIX.replace("{uuid}", uuid));
			sse.openSession(uuid, new PrintWriter(new SinkWriter(sink)), writerHolder.get());
			sink.onDispose(() -> sse.removeConnection(uuid));
		}, FluxSink.OverflowStrategy.BUFFER).subscribeOn(pool);

		return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(frames, String.class);
	}

	private Mono<ServerResponse> handleMessage(ServerRequest request) {
		ServerSseRegistry sse = sseRegistry();
		if (sse == null) {
			return ServerResponse.notFound().build();
		}
		if (!sse.isOriginAllowed(request.headers().firstHeader("Origin"))) {
			return ServerResponse.status(HttpStatus.FORBIDDEN).build();
		}

		String uuid = request.pathVariable("uuid");
		// 会话归属校验：uuid 必须属于本 runtime，否则存在跨 runtime 串台，直接失效。
		if (!runtime.hasSession(uuid)) {
			return ServerResponse.notFound().build();
		}
		authenticate(uuid, request);

		return request.bodyToMono(String.class).defaultIfEmpty("")
				.flatMap(json -> Mono.fromCallable(() -> callSseHandle(sse, uuid, json)).subscribeOn(pool)
						.flatMap(this::toResponse));
	}

	private Mono<ServerResponse> post(ServerRequest request) {
		ServerStreamableHttpRegistry transport = streamableRegistry();
		if (transport == null) {
			return ServerResponse.notFound().build();
		}

		String session = request.headers().firstHeader(ServerStreamableHttp.SESSION_ID_HEADER);
		// 初始化请求没有会话头；已持会话的后续消息必须归属本 runtime。
		if (session != null && !runtime.hasSession(session)) {
			return ServerResponse.notFound().build();
		}
		authenticate(session, request);

		return request.bodyToMono(String.class).defaultIfEmpty("")
				.flatMap(body -> handlePost(transport, body, request));
	}

	private Mono<ServerResponse> handlePost(ServerStreamableHttpRegistry transport, String body,
			ServerRequest request) {
		java.util.concurrent.Callable<HttpResult> callable = () -> transport.post(body, headers(request));
		Mono<HttpResult> source = Mono.fromCallable(callable);
		return source.subscribeOn(pool).flatMap(result -> httpResultResponse(result));
	}

	private Mono<ServerResponse> delete(ServerRequest request) {
		ServerStreamableHttpRegistry transport = streamableRegistry();
		if (transport == null) {
			return ServerResponse.notFound().build();
		}

		String session = request.headers().firstHeader(ServerStreamableHttp.SESSION_ID_HEADER);
		if (session != null && !runtime.hasSession(session)) {
			return ServerResponse.notFound().build();
		}
		authenticate(session, request);

		java.util.concurrent.Callable<HttpResult> callable = () -> transport.delete(session, headers(request));
		return Mono.fromCallable(callable).subscribeOn(pool).flatMap(result -> httpResultResponse(result));
	}

	private Mono<ServerResponse> get(ServerRequest request) {
		ServerStreamableHttpRegistry transport = streamableRegistry();
		if (transport == null) {
			return ServerResponse.notFound().build();
		}

		String session = request.headers().firstHeader(ServerStreamableHttp.SESSION_ID_HEADER);
		if (!runtime.hasSession(session)) {
			return ServerResponse.notFound().build();
		}
		authenticate(session, request);

		Flux<String> frames = Flux.<String>create(sink -> {
			HttpResult result;
			synchronized (transport) {
				result = transport.openEventStream(session, new PrintWriter(new SinkWriter(sink)), headers(request));
			}
			sink.onDispose(() -> transport.closeEventStream(session, new PrintWriter(new SinkWriter(sink))));
			if (result.getStatus() != HttpStatus.OK.value()) {
				sink.complete();
			}
		}, FluxSink.OverflowStrategy.BUFFER).subscribeOn(pool);

		return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM)
				.header("Cache-Control", "no-cache")
				.header("X-Accel-Buffering", "no")
				.body(frames, String.class);
	}

	private SyncResult callSseHandle(ServerSseRegistry sse, String uuid, String json) {
		try {
			sse.handle(uuid, json);
			return SyncResult.ok();
		} catch (JsonRpcErrorException e) {
			return new SyncResult(HttpStatus.BAD_REQUEST.value(), MediaType.APPLICATION_JSON_VALUE, e.toJson());
		}
	}

	private Mono<ServerResponse> toResponse(SyncResult result) {
		ServerResponse.BodyBuilder builder = ServerResponse.status(result.status);
		if (result.contentType != null) {
			builder.contentType(MediaType.parseMediaType(result.contentType));
		}
		return result.body == null ? builder.build() : builder.bodyValue(result.body);
	}

	private Mono<ServerResponse> httpResultResponse(HttpResult result) {
		ServerResponse.BodyBuilder builder = ServerResponse.status(result.getStatus());
		result.getHeaders().forEach(builder::header);
		if (result.getContentType() != null) {
			builder.contentType(MediaType.parseMediaType(result.getContentType()));
		}
		return result.getBody() == null ? builder.build() : builder.bodyValue(result.getBody());
	}

	private Map<String, String> headers(ServerRequest request) {
		Map<String, String> values = new LinkedHashMap<>();
		for (Map.Entry<String, java.util.List<String>> entry : request.headers().asHttpHeaders().entrySet()) {
			java.util.List<String> headerValues = entry.getValue();
			if (headerValues != null && !headerValues.isEmpty()) {
				values.put(entry.getKey(), headerValues.get(0));
			}
		}
		return values;
	}

	private void authenticate(String sessionId, ServerRequest request) {
		McpAuthService authService = runtime.getAuthService();
		if (authService == null) {
			return;
		}
		// 认证 SPI 占位调用点：默认放行，后续认证 change 只替换实现。
		authService.authenticate(new McpRequest(sessionId, runtime.getTransport(), endpoint(), null,
				headers(request)));
	}

	private ServerSseRegistry sseRegistry() {
		if (runtime.getRegistry() instanceof ServerSseRegistry) {
			return (ServerSseRegistry) runtime.getRegistry();
		}
		return null;
	}

	private ServerStreamableHttpRegistry streamableRegistry() {
		if (runtime.getRegistry() instanceof ServerStreamableHttpRegistry) {
			return (ServerStreamableHttpRegistry) runtime.getRegistry();
		}
		return null;
	}

	private String endpoint() {
		return runtime.getEndpoint();
	}

	/**
	 * 同步处理结果，供响应式管道转换。
	 */
	private static final class SyncResult {

		static SyncResult ok() {
			return new SyncResult(HttpStatus.OK.value(), null, null);
		}

		private final int status;

		private final String contentType;

		private final String body;

		private SyncResult(int status, String contentType, String body) {
			this.status = status;
			this.contentType = contentType;
			this.body = body;
		}
	}

	/**
	 * Writer 桥：把内核 {@code PrintWriter} 写出的 SSE 帧原文转发到 {@link FluxSink}。
	 *
	 * <p>底层 Writer 才是真正转发通道，{@code PrintWriter} 的所有 print/println/write
	 * 最终都汇到 {@code out.write}，因此分隔符不会丢失。</p>
	 */
	private static final class SinkWriter extends Writer {

		private final FluxSink<String> sink;

		private SinkWriter(FluxSink<String> sink) {
			this.sink = sink;
		}

		@Override
		public void write(char[] cbuf, int off, int len) {
			if (sink.isCancelled()) {
				return;
			}
			sink.next(new String(cbuf, off, len));
		}

		@Override
		public void write(String str, int off, int len) {
			if (sink.isCancelled()) {
				return;
			}
			sink.next(str.substring(off, off + len));
		}

		@Override
		public void write(int c) {
			if (sink.isCancelled()) {
				return;
			}
			sink.next(String.valueOf((char) c));
		}

		@Override
		public void flush() {
			// 帧由内核统一提交，桥层不额外 flush
		}

		@Override
		public void close() {
			sink.complete();
		}
	}
}
