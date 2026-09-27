package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web.webflux;

import com.ajaxjs.mcp.server.ServerStreamableHttp;
import com.ajaxjs.mcp.server.error.JsonRpcErrorException;
import com.ajaxjs.mcp.server.model.HttpResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpCapabilities;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpPrincipal;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpHeaderNames;
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
import reactor.core.publisher.UnicastProcessor;
import reactor.core.scheduler.Scheduler;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

	private final McpHeaderNames headerNames;

	private final Scheduler pool;

	McpFluxTransportHandler(McpServerRuntime runtime, Scheduler pool) {
		this.runtime = runtime;
		this.headerNames = new McpHeaderNames(runtime.getProperties() == null ? null
				: runtime.getProperties().getHeaders());
		this.pool = pool;
	}

	RouterFunction<ServerResponse> router() {
		String endpoint = runtime.getEndpoint();
		if (endpoint == null || endpoint.isEmpty()) {
			throw new IllegalStateException(
					"MCP runtime '" + runtime.getKey() + "' has an empty endpoint, cannot register routes.");
		}

		if (STREAMABLE_TRANSPORT.equalsIgnoreCase(runtime.getTransport())) {
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
		if (!sse.isOriginAllowed(request.headers().firstHeader(headerNames.origin()))) {
			return ServerResponse.status(HttpStatus.FORBIDDEN).build();
		}
		if (!isAuthorized(null, request)) {
			return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
		}

		String uuid = UUID.randomUUID().toString();
		AtomicReference<String> writerHolder = new AtomicReference<>();
		Flux<String> frames = Flux.<String>create(sink -> {
			writerHolder.set(endpoint() + McpWebfluxWebBinding.MESSAGES_SUFFIX.replace("{uuid}", uuid));
			sse.openSession(uuid, new PrintWriter(new FrameWriter(sink::next, () -> !sink.isCancelled(), sink::complete)),
					writerHolder.get());
			sink.onDispose(() -> sse.removeConnection(uuid));
		}, FluxSink.OverflowStrategy.BUFFER).subscribeOn(pool);

		return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM).body(frames, String.class);
	}

	private Mono<ServerResponse> handleMessage(ServerRequest request) {
		ServerSseRegistry sse = sseRegistry();
		if (sse == null) {
			return ServerResponse.notFound().build();
		}
		if (!sse.isOriginAllowed(request.headers().firstHeader(headerNames.origin()))) {
			return ServerResponse.status(HttpStatus.FORBIDDEN).build();
		}

		String uuid = request.pathVariable("uuid");
		// 会话归属校验：uuid 必须属于本 runtime，否则存在跨 runtime 串台，直接失效。
		if (!runtime.hasSession(uuid)) {
			return ServerResponse.notFound().build();
		}

		// scope 裁剪需要请求体中的 JSON-RPC method，故授权后移到 body 读取之后（与 post() 一致）。
		return request.bodyToMono(String.class).defaultIfEmpty("")
				.flatMap(body -> {
					if (!isAuthorized(uuid, request, body)) {
						return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
					}
					return Mono.<SyncResult>fromCallable(() -> callSseHandle(sse, uuid, body)).subscribeOn(pool)
							.flatMap(this::toResponse);
				});
	}

	private Mono<ServerResponse> post(ServerRequest request) {
		ServerStreamableHttpRegistry transport = streamableRegistry();
		if (transport == null) {
			return ServerResponse.notFound().build();
		}

		String session = request.headers().firstHeader(headerNames.sessionId());
		// 初始化请求没有会话头；已持会话的后续消息必须归属本 runtime。
		if (session != null && !runtime.hasSession(session)) {
			return ServerResponse.notFound().build();
		}

		// scope 裁剪需要请求体中的 JSON-RPC method，故授权后移到 body 读取之后。
		return request.bodyToMono(String.class).defaultIfEmpty("")
				.flatMap(body -> {
					if (!isAuthorized(session, request, body)) {
						return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
					}
					return handlePost(transport, body, request);
				});
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

		String session = request.headers().firstHeader(headerNames.sessionId());
		if (session != null && !runtime.hasSession(session)) {
			return ServerResponse.notFound().build();
		}
		if (!isAuthorized(session, request)) {
			return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
		}

		java.util.concurrent.Callable<HttpResult> callable = () -> transport.delete(session, headers(request));
		return Mono.fromCallable(callable).subscribeOn(pool).flatMap(result -> httpResultResponse(result));
	}

	private Mono<ServerResponse> get(ServerRequest request) {
		final ServerStreamableHttpRegistry transport = streamableRegistry();
		if (transport == null) {
			return ServerResponse.notFound().build();
		}

		String session = request.headers().firstHeader(headerNames.sessionId());
		if (session == null || !runtime.hasSession(session)) {
			return ServerResponse.notFound().build();
		}
		if (!isAuthorized(session, request)) {
			return ServerResponse.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 响应状态必须在事件流之前确定：先与内核同步打开事件流拿到结果，
		// 非 200（403/400/503 等）直接透出真实状态码，不再伪装成空的 200 SSE（与 MVC 一致）。
		// UnicastProcessor 在响应提交前即可缓冲内核帧，早到的帧不丢失。
		return Mono.defer(() -> {
			final UnicastProcessor<String> eventStream = UnicastProcessor.create();
			final FrameWriter bridge = new FrameWriter(eventStream::onNext, () -> !eventStream.isDisposed(),
					eventStream::onComplete);
			final PrintWriter eventWriter = new PrintWriter(bridge);
			HttpResult result;
			try {
				synchronized (transport) {
					result = transport.openEventStream(session, eventWriter, headers(request));
				}
			} catch (RuntimeException failure) {
				// 会话在归属预检与内核打开之间的窗口过期等异常时，事件流可能已注册；
				// 必须先解除注册再上抛，否则心跳会持续写入无订阅者的缓冲队列（内存泄漏）。
				transport.closeEventStream(session, eventWriter);
				throw failure;
			}
			if (result.getStatus() != HttpStatus.OK.value()) {
				transport.closeEventStream(session, eventWriter);
				return httpResultResponse(result);
			}
			return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM)
					.header("Cache-Control", "no-cache")
					.header("X-Accel-Buffering", "no")
					.body(eventStream.doFinally(signal -> transport.closeEventStream(session, eventWriter)),
							String.class);
		}).subscribeOn(pool);
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
		headerNames.applyCustomNames(values);
		return values;
	}

	/**
	 * 认证并处理"匿名即拒绝"：未配置认证服务或认证关闭时为放行主体，
	 * 开了认证但 token 无效时是匿名主体。
	 *
	 * <p>开启 scope 裁剪时，认证通过后再按请求体对应的 capability 校验主体 scopes；
	 * 未开启、非能力操作或主体未携带 scope 信息时一律放行。</p>
	 *
	 * @return 通过返回 {@code true}
	 */
	private boolean isAuthorized(String sessionId, ServerRequest request) {
		return isAuthorized(sessionId, request, null);
	}

	private boolean isAuthorized(String sessionId, ServerRequest request, String body) {
		McpAuthService authService = runtime.getAuthService();
		if (authService == null) {
			return true;
		}
		McpPrincipal principal = authService.authenticate(new McpRequest(sessionId, runtime.getTransport(),
				endpoint(), body, headers(request)));
		if (principal == McpPrincipal.ANONYMOUS) {
			return false;
		}
		if (isScopeCheckEnabled() && !McpCapabilities.covers(principal, McpCapabilities.ofBody(body))) {
			return false;
		}
		return true;
	}

	private boolean isScopeCheckEnabled() {
		return runtime.getProperties() != null && runtime.getProperties().isScopeCheckEnabled();
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
	 * Writer 桥：把内核 {@code PrintWriter} 写出的 SSE 帧原文转发到下游生产者。
	 *
	 * <p>FluxSink 与 UnicastProcessor 两种生产者通过统一触点接入，避免桥接逻辑重复。
	 * 底层 Writer 才是真正转发通道，{@code PrintWriter} 的所有 print/println/write
	 * 最终都汇到 {@code out.write}，因此分隔符不会丢失。</p>
	 */
	private static final class FrameWriter extends Writer {

		private final Consumer<String> emitter;

		private final Supplier<Boolean> active;

		private final Runnable onClose;

		private FrameWriter(Consumer<String> emitter, Supplier<Boolean> active, Runnable onClose) {
			this.emitter = emitter;
			this.active = active;
			this.onClose = onClose;
		}

		@Override
		public void write(char[] cbuf, int off, int len) {
			if (active.get()) {
				emitter.accept(new String(cbuf, off, len));
			}
		}

		@Override
		public void write(String str, int off, int len) {
			if (active.get()) {
				emitter.accept(str.substring(off, off + len));
			}
		}

		@Override
		public void write(int c) {
			if (active.get()) {
				emitter.accept(String.valueOf((char) c));
			}
		}

		@Override
		public void flush() {
			// 帧由内核统一提交，桥层不额外 flush
		}

		@Override
		public void close() {
			onClose.run();
		}
	}
}
