package com.xiongx9527.fw.plus.ai.mcp.transport.servlet;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.mcp.transport.AbstractStreamableTransport;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Spring MVC（Servlet）Streamable HTTP 传输
 *
 * <p>会话的服务端 → 客户端通道通过 {@link SseEmitter} 实现（spec 5.2）：
 * {@code GET} 建立/恢复该会话 SSE 流，服务端通知与事件 ID 由基类统一管理。
 *
 * <p>所依赖的 {@code spring-webmvc} 在 core 中为 optional，只有 Servlet 应用才会加载本类。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public class McpServletStreamableTransport extends AbstractStreamableTransport {

	public McpServletStreamableTransport(String name, String path, long sessionTimeoutMillis,
			ObjectMapper objectMapper) {
		super(name, path, sessionTimeoutMillis, objectMapper);
	}

	/**
	 * 建立/恢复会话 SSE 流
	 *
	 * @param sessionId   会话 ID
	 * @param lastEventId 客户端上次收到的事件 ID（可空，用于事件恢复）
	 * @return SSE 发射器；会话不存在时返回 {@code null}
	 */
	public SseEmitter createEmitter(String sessionId, String lastEventId) {
		final SseEmitter emitter = new SseEmitter(sessionTimeoutMillis > 0L ? sessionTimeoutMillis : 0L);
		final SseEmitterSink sink = new SseEmitterSink(this, emitter, sessionId);
		emitter.onCompletion(() -> detachSink(sessionId, sink));
		emitter.onTimeout(() -> {
			detachSink(sessionId, sink);
			emitter.complete();
		});
		emitter.onError(error -> detachSink(sessionId, sink));
		if (!sink.isActive()) {
			return null;
		}
		if (!attachSink(sessionId, sink, lastEventId)) {
			// 会话不存在：关闭刚建立的通道，由端点返回 404
			emitter.complete();
			return null;
		}
		sink.markAttached();
		return emitter;
	}

	/**
	 * SseEmitter 通道适配
	 */
	private static final class SseEmitterSink implements SseSink {

		private final McpServletStreamableTransport transport;
		private final SseEmitter emitter;
		private final String sessionId;
		private volatile boolean active = true;
		private volatile boolean attached;

		SseEmitterSink(McpServletStreamableTransport transport, SseEmitter emitter, String sessionId) {
			this.transport = transport;
			this.emitter = emitter;
			this.sessionId = sessionId;
		}

		void markAttached() {
			this.attached = true;
		}

		boolean isActive() {
			return active;
		}

		@Override
		public void send(String eventId, String jsonData) {
			if (!active) {
				return;
			}
			try {
				SseEmitter.SseEventBuilder event = SseEmitter.event().data(jsonData);
				if (eventId != null) {
					event = event.id(eventId);
				}
				emitter.send(event);
			} catch (Exception e) {
				active = false;
				if (attached) {
					transport.detachSink(sessionId, this);
				}
				emitter.completeWithError(e);
				throw new IllegalStateException("Failed to send SSE event to session " + sessionId, e);
			}
		}

		@Override
		public void complete() {
			if (!active) {
				return;
			}
			active = false;
			emitter.complete();
		}

		@Override
		public void completeWithError(Throwable error) {
			if (!active) {
				return;
			}
			active = false;
			emitter.completeWithError(error);
		}
	}
}
