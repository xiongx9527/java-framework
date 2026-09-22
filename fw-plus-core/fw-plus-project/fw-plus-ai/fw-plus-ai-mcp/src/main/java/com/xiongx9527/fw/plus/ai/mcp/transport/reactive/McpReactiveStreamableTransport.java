package com.xiongx9527.fw.plus.ai.mcp.transport.reactive;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.mcp.transport.AbstractStreamableTransport;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * Spring WebFlux（Reactive）Streamable HTTP 传输
 *
 * <p>会话的服务端 → 客户端通道通过 {@code Flux<ServerSentEvent<String>>} 实现（spec 5.3）：
 * 底层使用 {@link Sinks#many()} 作为可跨线程写入的事件源，事件 ID 与恢复缓冲由基类统一管理。
 *
 * <p>所依赖的 {@code spring-webflux}/{@code reactor} 在 core 中为 optional，
 * 只有响应式应用才会加载本类。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public class McpReactiveStreamableTransport extends AbstractStreamableTransport {

	public McpReactiveStreamableTransport(String name, String path, long sessionTimeoutMillis,
			ObjectMapper objectMapper) {
		super(name, path, sessionTimeoutMillis, objectMapper);
	}

	/**
	 * 建立/恢复会话 SSE 流
	 *
	 * @param sessionId   会话 ID
	 * @param lastEventId 客户端上次收到的事件 ID（可空，用于事件恢复）
	 * @return SSE 事件流；会话不存在时返回 {@code null}
	 */
	public Flux<ServerSentEvent<String>> createEventStream(String sessionId, String lastEventId) {
		Sinks.Many<ServerSentEvent<String>> sink = Sinks.many().unicast().onBackpressureBuffer();
		SseSink handle = new SinkSseSink(this, sink, sessionId);
		if (!attachSink(sessionId, handle, lastEventId)) {
			// 会话不存在：由端点转换为 404
			sink.tryEmitComplete();
			return null;
		}
		return sink.asFlux();
	}

	/**
	 * reactor Sinks 通道适配
	 */
	private static final class SinkSseSink implements SseSink {

		private final McpReactiveStreamableTransport transport;
		private final Sinks.Many<ServerSentEvent<String>> sink;
		private final String sessionId;
		private volatile boolean active = true;

		SinkSseSink(McpReactiveStreamableTransport transport, Sinks.Many<ServerSentEvent<String>> sink,
				String sessionId) {
			this.transport = transport;
			this.sink = sink;
			this.sessionId = sessionId;
		}

		@Override
		public void send(String eventId, String jsonData) {
			if (!active) {
				return;
			}
			ServerSentEvent<String> event = ServerSentEvent.<String>builder()
					.id(eventId)
					.data(jsonData)
					.build();
			Sinks.EmitResult result = sink.tryEmitNext(event);
			if (result.isFailure()) {
				active = false;
				transport.detachSink(sessionId, this);
			}
		}

		@Override
		public void complete() {
			if (!active) {
				return;
			}
			active = false;
			sink.tryEmitComplete();
		}

		@Override
		public void completeWithError(Throwable error) {
			if (!active) {
				return;
			}
			active = false;
			sink.tryEmitError(error);
		}
	}
}
