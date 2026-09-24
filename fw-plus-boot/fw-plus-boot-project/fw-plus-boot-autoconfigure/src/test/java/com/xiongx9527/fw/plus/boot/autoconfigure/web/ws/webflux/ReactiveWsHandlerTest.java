package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBusinessDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsInvocationContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsSubscribeScript;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 响应式栈处理器行为验收（Scenario A11/A12/A13）：
 * 握手鉴权、文本与二进制派发、定向发送与广播，以及脚本不占用 Reactor event loop。
 *
 * @author zzy
 */
class ReactiveWsHandlerTest {

	private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

	private static final DefaultDataBufferFactory FACTORY = new DefaultDataBufferFactory();

	@Test
	void handshakeWithoutTokenShouldBeRejectedWithPolicyViolation() {
		ReactiveWsHandler handler = new ReactiveWsHandler(wsConfig());
		TestReactiveSession session = new TestReactiveSession("s1", uri("", "c1"), Flux.empty());

		handler.handle(session).block(Duration.ofSeconds(5));

		assertThat(session.closedStatus.get()).isNotNull();
		assertThat(session.closedStatus.get().getCode()).isEqualTo(1008);
		assertThat(session.sentText).isEmpty();
	}

	@Test
	void handshakeWithForeignSecretShouldBeRejected() {
		ReactiveWsHandler handler = new ReactiveWsHandler(wsConfig());
		String foreignToken = token("app-a", "9999999999999999999999999999999999999999999999999999999999999999");
		TestReactiveSession session =
				new TestReactiveSession("s2", uri(foreignToken, "c1"), Flux.empty());

		handler.handle(session).block(Duration.ofSeconds(5));

		assertThat(session.closedStatus.get()).isNotNull();
		assertThat(session.closedStatus.get().getCode()).isEqualTo(1008);
	}

	@Test
	void textPingShouldBeAnsweredWithPong() {
		ReactiveWsHandler handler = new ReactiveWsHandler(wsConfig());
		TestReactiveSession session = new TestReactiveSession("s3", uri(token("app-a"), "c1"),
				Flux.just(text("ping")));

		handler.handle(session).block(Duration.ofSeconds(5));

		assertThat(session.closedStatus.get()).isNull();
		assertThat(session.sentText).anyMatch(payload -> payload.contains("pong"));
	}

	@Test
	void binaryFrameShouldDispatchWithBinaryContextAndEchoBinaryReply() {
		ReactiveWsHandler handler = new ReactiveWsHandler(wsConfig());
		RecordingDispatcher dispatcher = new RecordingDispatcher("binary");
		handler.setBusinessDispatcher(dispatcher);

		TestReactiveSession session = new TestReactiveSession("s4", uri(token("app-a"), "c1"),
				Flux.just(new WebSocketMessage(WebSocketMessage.Type.BINARY,
						FACTORY.wrap(new byte[]{1, 2, 3}))));

		handler.handle(session).block(Duration.ofSeconds(5));

		assertThat(dispatcher.calls).hasSize(1);
		WsInvocationContext context = dispatcher.calls.get(0);
		assertThat(context.getMessageKind()).isEqualTo(WsInvocationContext.MessageKind.BINARY);
		assertThat(context.getBinaryBody()).containsExactly((byte) 1, (byte) 2, (byte) 3);
		assertThat(context.getParams()).isNull();
		assertThat(context.getBusinessType()).isEqualTo("binary");

		assertThat(session.sentBinary).hasSize(1);
		assertThat(session.sentBinary.get(0)).containsExactly((byte) 1, (byte) 2, (byte) 3);
	}

	@Test
	void binaryFrameShouldBeIgnoredWhenBinaryDisabled() {
		WsConfig config = wsConfig();
		config.getBinary().setEnabled(false);
		ReactiveWsHandler handler = new ReactiveWsHandler(config);
		RecordingDispatcher dispatcher = new RecordingDispatcher("binary");
		handler.setBusinessDispatcher(dispatcher);

		TestReactiveSession session = new TestReactiveSession("s5", uri(token("app-a"), "c1"),
				Flux.just(new WebSocketMessage(WebSocketMessage.Type.BINARY,
						FACTORY.wrap(new byte[]{9}))));

		handler.handle(session).block(Duration.ofSeconds(5));

		assertThat(dispatcher.calls).isEmpty();
		assertThat(session.closedStatus.get()).isNotNull();
		assertThat(session.closedStatus.get().getCode()).isEqualTo(1003);
	}

	@Test
	void textSubscribesShouldRunOffReactorEventLoop() {
		ReactiveWsHandler handler = new ReactiveWsHandler(wsConfig());
		RecordingDispatcher dispatcher = new RecordingDispatcher("echo");
		handler.setBusinessDispatcher(dispatcher);

		TestReactiveSession session = new TestReactiveSession("s6", uri(token("app-a"), "c1"),
				Flux.just(text("{\"type\":\"echo\"}")));

		handler.handle(session).block(Duration.ofSeconds(5));

		assertThat(dispatcher.calls).hasSize(1);
		assertThat(dispatcher.threadNames).isNotEmpty();
		assertThat(dispatcher.threadNames).allMatch(name -> name.startsWith("ws-script-"));
	}

	@Test
	void broadcastShouldReachRegisteredLocalSessions() {
		ReactiveWsHandler handler = new ReactiveWsHandler(wsConfig());
		TestReactiveSession session = new TestReactiveSession("s7", uri(token("app-a"), "c1"), Flux.never());

		// handle 在返回前同步完成鉴权与登记；这里不订阅入站流，使会话保留在注册表中
		handler.handle(session);

		handler.broadcastToApp("app-a", "hello-app");
		handler.broadcastToClient("app-a", "c1", "hello-client");

		assertThat(session.sentText).containsExactly("hello-app", "hello-client");
	}

	@Test
	void broadcastShouldSkipOtherApps() {
		ReactiveWsHandler handler = new ReactiveWsHandler(wsConfig());
		TestReactiveSession session = new TestReactiveSession("s8", uri(token("app-a"), "c1"), Flux.never());
		handler.handle(session);

		handler.broadcastToClient("other-app", "c1", "should-not-arrive");
		handler.broadcastToApp("other-app", "should-not-arrive");

		assertThat(session.sentText).isEmpty();
	}

	@Test
	void totalSessionLimitShouldBeEnforced() {
		WsConfig config = wsConfig();
		config.getSession().setMaxTotalSessions(1);
		ReactiveWsHandler handler = new ReactiveWsHandler(config);

		TestReactiveSession first = new TestReactiveSession("t1", uri(token("app-a"), "c1"), Flux.never());
		handler.handle(first);
		assertThat(first.closedStatus.get()).isNull();

		TestReactiveSession second = new TestReactiveSession("t2", uri(token("app-a"), "c2"), Flux.never());
		handler.handle(second);

		assertThat(second.closedStatus.get()).isNotNull();
		assertThat(second.closedStatus.get().getCode()).isEqualTo(1013);
	}

	@Test
	void perClientSessionLimitShouldBeEnforced() {
		WsConfig config = wsConfig();
		config.getSession().setMaxSessionsPerClient(1);
		ReactiveWsHandler handler = new ReactiveWsHandler(config);

		TestReactiveSession first = new TestReactiveSession("l1", uri(token("app-a"), "c1"), Flux.never());
		handler.handle(first);
		assertThat(first.closedStatus.get()).isNull();

		TestReactiveSession second = new TestReactiveSession("l2", uri(token("app-a"), "c1"), Flux.never());
		handler.handle(second);

		assertThat(second.closedStatus.get()).isNotNull();
		assertThat(second.closedStatus.get().getCode()).isEqualTo(1013);
	}

	private static WsConfig wsConfig() {
		WsConfig config = new WsConfig();
		config.getToken().setSecret(SECRET);
		config.getWebsocket().setPath("/ws/api");
		return config;
	}

	private static URI uri(String token, String clientId) {
		String query = token.isEmpty()
				? "?clientId=" + clientId
				: "?token=" + token + "&clientId=" + clientId;
		return URI.create("ws://localhost:8080/ws/api" + query);
	}

	private static String token(String appId) {
		return token(appId, SECRET);
	}

	private static String token(String appId, String secret) {
		WsConfig config = wsConfig();
		config.getToken().setSecret(secret);
		WsBaseService service = new WsBaseService() {
			@Override
			public Map<String, Object> getConfig() {
				return config.flatten();
			}
		};
		WsBaseService.SecretKeyAndSignatureAlgorithm key = service.getSecretKeyAndSignatureAlgorithm();
		return Jwts.builder()
				.setSubject(appId)
				.signWith(key.getSecretKey(), key.getAlgorithm())
				.compact();
	}

	private static WebSocketMessage text(String payload) {
		return new WebSocketMessage(WebSocketMessage.Type.TEXT,
				FACTORY.wrap(payload.getBytes(StandardCharsets.UTF_8)));
	}

	private static byte[] readBytes(WebSocketMessage message) {
		ByteBuffer buffer = message.getPayload().asByteBuffer();
		byte[] bytes = new byte[buffer.remaining()];
		buffer.get(bytes);
		return bytes;
	}

	/**
	 * 记录调用上下文与执行线程的订阅调度桩件。
	 */
	private static final class RecordingDispatcher implements WsBusinessDispatcher {

		private final Set<String> enabledTypes;

		private final List<WsInvocationContext> calls = new CopyOnWriteArrayList<>();

		private final List<String> threadNames = new CopyOnWriteArrayList<>();

		private RecordingDispatcher(String... enabledTypes) {
			this.enabledTypes = new HashSet<>(Arrays.asList(enabledTypes));
		}

		@Override
		public List<? extends WsSubscribeScript> findEnabledSubscribes(String appId, String businessType) {
			if (!enabledTypes.contains(businessType)) {
				return Collections.emptyList();
			}
			return Collections.singletonList(new WsSubscribeScript() {
				@Override
				public String getName() {
					return businessType;
				}

				@Override
				public String getBusinessId() {
					return businessType;
				}
			});
		}

		@Override
		public Object execute(WsSubscribeScript subscribe, WsInvocationContext context) {
			calls.add(context);
			threadNames.add(Thread.currentThread().getName());
			if (context.getMessageKind() == WsInvocationContext.MessageKind.BINARY
					&& context.getBinaryBody() != null && context.getReplyHandle() != null) {
				context.getReplyHandle().sendBinary(context.getBinaryBody());
			}
			return "ok";
		}
	}

	/**
	 * 手写响应式会话桩件：完整实现 {@link WebSocketSession}，记录发送与关闭，便于断言非阻塞派发结果。
	 */
	private static final class TestReactiveSession implements WebSocketSession {

		private final String id;

		private final HandshakeInfo handshakeInfo;

		private final Map<String, Object> attributes = new ConcurrentHashMap<>();

		private final List<String> sentText = new CopyOnWriteArrayList<>();

		private final List<byte[]> sentBinary = new CopyOnWriteArrayList<>();

		private final Flux<WebSocketMessage> inbound;

		private final AtomicReference<CloseStatus> closedStatus = new AtomicReference<>();

		private volatile boolean open = true;

		private TestReactiveSession(String id, URI uri, Flux<WebSocketMessage> inbound) {
			this.id = id;
			this.handshakeInfo = new HandshakeInfo(uri, new HttpHeaders(), Mono.empty(), null);
			this.inbound = inbound;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public HandshakeInfo getHandshakeInfo() {
			return handshakeInfo;
		}

		@Override
		public DataBufferFactory bufferFactory() {
			return FACTORY;
		}

		@Override
		public Map<String, Object> getAttributes() {
			return attributes;
		}

		@Override
		public Flux<WebSocketMessage> receive() {
			return inbound;
		}

		@Override
		public Mono<Void> send(Publisher<WebSocketMessage> messages) {
			Flux.from(messages).subscribe(this::record);
			return Mono.empty();
		}

		@Override
		public Mono<Void> close() {
			return close(CloseStatus.NORMAL);
		}

		@Override
		public Mono<Void> close(CloseStatus status) {
			closedStatus.set(status);
			open = false;
			return Mono.empty();
		}

		@Override
		public WebSocketMessage textMessage(String payload) {
			return text(payload);
		}

		@Override
		public WebSocketMessage binaryMessage(Function<DataBufferFactory, DataBuffer> factory) {
			return new WebSocketMessage(WebSocketMessage.Type.BINARY, factory.apply(FACTORY));
		}

		@Override
		public WebSocketMessage pingMessage(Function<DataBufferFactory, DataBuffer> factory) {
			return new WebSocketMessage(WebSocketMessage.Type.PING, factory.apply(FACTORY));
		}

		@Override
		public WebSocketMessage pongMessage(Function<DataBufferFactory, DataBuffer> factory) {
			return new WebSocketMessage(WebSocketMessage.Type.PONG, factory.apply(FACTORY));
		}

		@Override
		public boolean isOpen() {
			return open;
		}

		@Override
		public Mono<CloseStatus> closeStatus() {
			return Mono.justOrEmpty(closedStatus.get());
		}

		private void record(WebSocketMessage message) {
			if (message.getType() == WebSocketMessage.Type.BINARY) {
				sentBinary.add(readBytes(message));
			} else {
				sentText.add(message.getPayloadAsText());
			}
		}
	}
}
