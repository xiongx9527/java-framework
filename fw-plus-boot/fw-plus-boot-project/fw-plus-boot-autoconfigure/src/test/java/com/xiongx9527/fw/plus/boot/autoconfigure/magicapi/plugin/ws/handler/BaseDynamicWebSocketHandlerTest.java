package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.ssssssss.script.MagicScriptContext;

import java.lang.reflect.Method;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BaseDynamicWebSocketHandlerTest {

	@Test
	void putBusinessTypeShouldExposeTypeAndBusinessType() {
		TestDynamicWebSocketHandler handler = new TestDynamicWebSocketHandler();
		MagicScriptContext context = new MagicScriptContext();

		handler.putBusinessType(context, "init");

		assertThat(context.get("type")).isEqualTo("init");
		assertThat(context.get("businessType")).isEqualTo("init");
	}

	@Test
	void putBusinessMessageContextShouldKeepParams() {
		TestDynamicWebSocketHandler handler = new TestDynamicWebSocketHandler();
		MagicScriptContext context = new MagicScriptContext();
		JSONObject params = new JSONObject();
		params.put("type", "order-refresh");

		handler.putBusinessMessageContext(context, "order-refresh", params);

		assertThat(context.get("type")).isEqualTo("order-refresh");
		assertThat(context.get("businessType")).isEqualTo("order-refresh");
		assertThat(context.get("params")).isSameAs(params);
	}

	@Test
	void handleMessageShouldReturnTextPongForTextPing() throws Exception {
		TestDynamicWebSocketHandler handler = new TestDynamicWebSocketHandler();
		TestWebSocketSession session = openSession("s1");
		registerSession(handler, session, "app-a", "client-a");

		handler.handleMessage(session, new TextMessage("ping"));

		assertThat(session.textMessages).containsExactly("pong");
		assertThat(session.closeStatus).isNull();
	}

	@Test
	void handleMessageShouldReturnStructuredErrorForInvalidJson() throws Exception {
		TestDynamicWebSocketHandler handler = new TestDynamicWebSocketHandler();
		TestWebSocketSession session = openSession("s2");
		registerSession(handler, session, "app-a", "client-a");

		handler.handleMessage(session, new TextMessage("not-json"));

		assertThat(session.textMessages).hasSize(1);
		JSONObject response = JSONObject.parseObject(session.textMessages.get(0));
		assertThat(response.getBoolean("success")).isFalse();
		assertThat(response.getString("code")).isEqualTo("INVALID_JSON");
		assertThat(session.closeStatus).isNull();
	}

	@Test
	void handleMessageShouldReturnStructuredErrorForMissingType() throws Exception {
		TestDynamicWebSocketHandler handler = new TestDynamicWebSocketHandler();
		TestWebSocketSession session = openSession("s3");
		registerSession(handler, session, "app-a", "client-a");

		handler.handleMessage(session, new TextMessage("{\"payload\":1}"));

		assertThat(errorCode(session)).isEqualTo("MISSING_TYPE");
		assertThat(session.closeStatus).isNull();
	}

	@Test
	void handleMessageShouldReturnStructuredErrorForUnknownType() throws Exception {
		TestDynamicWebSocketHandler handler = new TestDynamicWebSocketHandler();
		TestWebSocketSession session = openSession("s4");
		registerSession(handler, session, "app-a", "client-a");

		handler.handleMessage(session, new TextMessage("{\"type\":\"unknown\"}"));

		assertThat(errorCode(session)).isEqualTo("UNKNOWN_TYPE");
		assertThat(session.closeStatus).isNull();
	}

	@Test
	void handleMessageShouldCloseBinaryMessageWithUnsupportedDataStatus() throws Exception {
		TestDynamicWebSocketHandler handler = new TestDynamicWebSocketHandler();
		TestWebSocketSession session = openSession("s5");
		registerSession(handler, session, "app-a", "client-a");

		handler.handleMessage(session, new BinaryMessage(new byte[]{1, 2, 3}));

		assertThat(session.closeStatus.getCode()).isEqualTo(CloseStatus.NOT_ACCEPTABLE.getCode());
		assertThat(session.textMessages).isEmpty();
	}

	@Test
	void handleMessageShouldReturnScriptErrorWhenSubscribeScriptThrows() throws Exception {
		WsSubscribeInfo subscribeInfo = new WsSubscribeInfo();
		subscribeInfo.setKey("app-a");
		subscribeInfo.setBusinessId("boom");
		subscribeInfo.setEnabled(true);
		subscribeInfo.setName("boom-script");
		subscribeInfo.setScript("throw 'boom';");
		WsSubscribeInfoMagicResourceStorage storage = mock(WsSubscribeInfoMagicResourceStorage.class);
		when(storage.findAll()).thenReturn(Collections.singletonList(subscribeInfo));
		TestDynamicWebSocketHandler handler = new TestDynamicWebSocketHandler(storage);
		TestWebSocketSession session = openSession("s6");
		registerSession(handler, session, "app-a", "client-a");

		handler.handleMessage(session, new TextMessage("{\"type\":\"boom\"}"));

		assertThat(errorCode(session)).isEqualTo("SCRIPT_ERROR");
		assertThat(session.closeStatus).isNull();
	}

	private static String errorCode(TestWebSocketSession session) {
		assertThat(session.textMessages).hasSize(1);
		return JSONObject.parseObject(session.textMessages.get(0)).getString("code");
	}

	private static TestWebSocketSession openSession(String id) {
		return new TestWebSocketSession(id, true);
	}

	private static void registerSession(TestDynamicWebSocketHandler handler, TestWebSocketSession session, String appId, String clientId) throws Exception {
		Method addSession = BaseDynamicWebSocketHandler.class.getDeclaredMethod("addSession", String.class, WebSocketSession.class, String.class);
		addSession.setAccessible(true);
		addSession.invoke(handler, appId, session, clientId);
	}

	private static class TestDynamicWebSocketHandler extends BaseDynamicWebSocketHandler {

		TestDynamicWebSocketHandler() {
			this(mock(WsSubscribeInfoMagicResourceStorage.class));
		}

		TestDynamicWebSocketHandler(WsSubscribeInfoMagicResourceStorage subscribeStorage) {
			super(mock(WsDataSourceInfoMagicResourceStorage.class), subscribeStorage);
		}

		@Override
		public Map<String, Object> getConfig() {
			return Collections.emptyMap();
		}

		@Override
		void broadcastMessageToClient(String excludedServerId, String appId, String clientId, String message) {
		}

		@Override
		void broadcastMessageToAllClient(String excludedServerId, String appId, String message) {
		}

		@Override
		public int getMaxClientSessions() {
			return 100;
		}

		@Override
		public int getSessionCacheExpireMinutes() {
			return 30;
		}

		@Override
		public int getCacheConcurrencyLevel() {
			return 4;
		}
	}

	private static class TestWebSocketSession implements WebSocketSession {

		private final String id;
		private final Map<String, Object> attributes = new HashMap<>();
		private final List<String> textMessages = new ArrayList<>();
		private boolean open;
		private CloseStatus closeStatus;

		TestWebSocketSession(String id, boolean open) {
			this.id = id;
			this.open = open;
		}

		@Override
		public String getId() {
			return id;
		}

		@Override
		public HttpHeaders getHandshakeHeaders() {
			return new HttpHeaders();
		}

		@Override
		public URI getUri() {
			return URI.create("ws://localhost/ws/api");
		}

		@Override
		public Map<String, Object> getAttributes() {
			return attributes;
		}

		@Override
		public Principal getPrincipal() {
			return null;
		}

		@Override
		public java.net.InetSocketAddress getLocalAddress() {
			return null;
		}

		@Override
		public java.net.InetSocketAddress getRemoteAddress() {
			return null;
		}

		@Override
		public String getAcceptedProtocol() {
			return null;
		}

		@Override
		public void setTextMessageSizeLimit(int messageSizeLimit) {
		}

		@Override
		public int getTextMessageSizeLimit() {
			return 8192;
		}

		@Override
		public void setBinaryMessageSizeLimit(int messageSizeLimit) {
		}

		@Override
		public int getBinaryMessageSizeLimit() {
			return 8192;
		}

		@Override
		public List<org.springframework.web.socket.WebSocketExtension> getExtensions() {
			return Collections.emptyList();
		}

		@Override
		public void sendMessage(WebSocketMessage<?> message) {
			if (message instanceof TextMessage) {
				textMessages.add(((TextMessage) message).getPayload());
			}
		}

		@Override
		public boolean isOpen() {
			return open;
		}

		@Override
		public void close() {
			close(CloseStatus.NORMAL);
		}

		@Override
		public void close(CloseStatus status) {
			this.closeStatus = status;
			this.open = false;
		}
	}
}
