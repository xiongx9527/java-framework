package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBusinessDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsInvocationContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsSubscribeScript;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.lang.reflect.Method;
import java.net.URI;
import java.nio.ByteBuffer;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * WebSocket 地基的会话/消息/错误处理行为测试（与 magic 无关）。
 *
 * @author zzy
 */
class AbstractWebSocketHandlerTest {

    @Test
    void handleMessageShouldReturnTextPongForTextPing() throws Exception {
        TestWebSocketHandler handler = new TestWebSocketHandler();
        TestWebSocketSession session = openSession("s1");
        registerSession(handler, session, "app-a", "client-a");

        handler.handleMessage(session, new TextMessage("ping"));

        assertThat(session.textMessages).containsExactly("pong");
        assertThat(session.closeStatus).isNull();
    }

    @Test
    void handleMessageShouldReturnStructuredErrorForInvalidJson() throws Exception {
        TestWebSocketHandler handler = new TestWebSocketHandler();
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
        TestWebSocketHandler handler = new TestWebSocketHandler();
        TestWebSocketSession session = openSession("s3");
        registerSession(handler, session, "app-a", "client-a");

        handler.handleMessage(session, new TextMessage("{\"payload\":1}"));

        assertThat(errorCode(session)).isEqualTo("MISSING_TYPE");
        assertThat(session.closeStatus).isNull();
    }

    @Test
    void handleMessageShouldReturnUnknownTypeWhenNoBusinessDispatcher() throws Exception {
        TestWebSocketHandler handler = new TestWebSocketHandler();
        TestWebSocketSession session = openSession("s4");
        registerSession(handler, session, "app-a", "client-a");

        handler.handleMessage(session, new TextMessage("{\"type\":\"any\"}"));

        assertThat(errorCode(session)).isEqualTo("UNKNOWN_TYPE");
        assertThat(session.closeStatus).isNull();
    }

    @Test
    void handleMessageShouldDispatchToBusinessAndReplyThroughHandle() throws Exception {
        AtomicReference<WsInvocationContext> captured = new AtomicReference<>();
        WsBusinessDispatcher dispatcher = new WsBusinessDispatcher() {
            @Override
            public List<? extends WsSubscribeScript> findEnabledSubscribes(String appId, String businessType) {
                return Collections.singletonList(subscribe("order-refresh"));
            }

            @Override
            public Object execute(WsSubscribeScript subscribe, WsInvocationContext context) {
                captured.set(context);
                context.getReplyHandle().sendText("handled:" + subscribe.getName());
                return "done";
            }
        };
        TestWebSocketHandler handler = new TestWebSocketHandler().withDispatcher(dispatcher);
        TestWebSocketSession session = openSession("s5");
        registerSession(handler, session, "app-a", "client-a");

        handler.handleMessage(session, new TextMessage("{\"type\":\"order-refresh\",\"id\":7}"));

        assertThat(session.textMessages).containsExactly("handled:order-refresh");
        assertThat(captured.get().getPhase()).isEqualTo(WsInvocationContext.Phase.MESSAGE);
        assertThat(captured.get().getAppId()).isEqualTo("app-a");
        assertThat(captured.get().getClientId()).isEqualTo("client-a");
        assertThat(captured.get().getBusinessType()).isEqualTo("order-refresh");
        assertThat(captured.get().getParams().getInteger("id")).isEqualTo(7);
    }

    @Test
    void handleMessageShouldReturnScriptErrorWhenDispatcherThrows() throws Exception {
        WsBusinessDispatcher dispatcher = new WsBusinessDispatcher() {
            @Override
            public List<? extends WsSubscribeScript> findEnabledSubscribes(String appId, String businessType) {
                return Collections.singletonList(subscribe("boom"));
            }

            @Override
            public Object execute(WsSubscribeScript subscribe, WsInvocationContext context) {
                throw new IllegalStateException("boom");
            }
        };
        TestWebSocketHandler handler = new TestWebSocketHandler().withDispatcher(dispatcher);
        TestWebSocketSession session = openSession("s6");
        registerSession(handler, session, "app-a", "client-a");

        handler.handleMessage(session, new TextMessage("{\"type\":\"boom\"}"));

        assertThat(errorCode(session)).isEqualTo("SCRIPT_ERROR");
        assertThat(session.closeStatus).isNull();
    }

    @Test
    void binaryDisabledShouldCloseBinaryMessageWithUnsupportedDataStatus() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("binary.enabled", "false");
        TestWebSocketHandler handler = TestWebSocketHandler.withConfig(config);
        TestWebSocketSession session = openSession("s7");
        registerSession(handler, session, "app-a", "client-a");

        handler.handleMessage(session, new BinaryMessage(new byte[]{1, 2, 3}));

        assertThat(session.closeStatus.getCode()).isEqualTo(CloseStatus.NOT_ACCEPTABLE.getCode());
        assertThat(session.textMessages).isEmpty();
    }

    @Test
    void binaryEnabledShouldDispatchBinaryMessageToBinarySubscribes() throws Exception {
        WsInvocationContext.MessageKind[] capturedKind = new WsInvocationContext.MessageKind[1];
        byte[][] capturedBody = new byte[1][];
        WsBusinessDispatcher dispatcher = new WsBusinessDispatcher() {
            @Override
            public List<? extends WsSubscribeScript> findEnabledSubscribes(String appId, String businessType) {
                if (!"binary".equals(businessType)) {
                    return Collections.emptyList();
                }
                return Collections.singletonList(subscribe("bin"));
            }

            @Override
            public Object execute(WsSubscribeScript subscribe1, WsInvocationContext context) {
                capturedKind[0] = context.getMessageKind();
                capturedBody[0] = context.getBinaryBody();
                return null;
            }
        };
        TestWebSocketHandler handler = new TestWebSocketHandler().withDispatcher(dispatcher);
        TestWebSocketSession session = openSession("s8");
        registerSession(handler, session, "app-a", "client-a");

        handler.handleMessage(session, new BinaryMessage(new byte[]{1, 2, 3}));

        assertThat(capturedKind[0]).isEqualTo(WsInvocationContext.MessageKind.BINARY);
        assertThat(capturedBody[0]).containsExactly((byte) 1, (byte) 2, (byte) 3);
        assertThat(session.closeStatus).isNull();
    }

    @Test
    void scheduledExecutorPoolSizeShouldUseConfiguredValue() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("scheduler.delayedClosePoolSize", 4);
        TestWebSocketHandler handler = TestWebSocketHandler.withConfig(config);

        java.lang.reflect.Field field = AbstractWebSocketHandler.class.getDeclaredField("scheduledExecutor");
        field.setAccessible(true);
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) field.get(handler);
        assertThat(executor.getCorePoolSize()).isEqualTo(4);
    }

    /**
     * 锁定响应报文契约：字段名、类型与缺省值必须与 magic-api 的
     * {@code JsonBean}（{@code code=1}、{@code message="success"}、{@code timestamp=now}，
     * 且无序列化注解）保持一致，避免拆分后报文结构漂移。
     */
    @Test
    void businessJsonBeanShouldKeepMagicJsonBeanContract() {
        AbstractWebSocketHandler.BusinessJsonBean bean = new AbstractWebSocketHandler.BusinessJsonBean();
        assertThat(bean.getCode()).isEqualTo(1);
        assertThat(bean.getMessage()).isEqualTo("success");
        assertThat(bean.getTimestamp()).isPositive();
        assertThat(bean.getExecuteTime()).isNull();
        assertThat(bean.getData()).isNull();

        bean.setBusinessType("ping");
        bean.setData("pong");
        bean.setExecuteTime(3);

        com.alibaba.fastjson2.JSONObject json =
                com.alibaba.fastjson2.JSONObject.parseObject(
                        com.alibaba.fastjson2.JSON.toJSONString(bean));
        assertThat(json.getIntValue("code")).isEqualTo(1);
        assertThat(json.getString("message")).isEqualTo("success");
        assertThat(json.getString("data")).isEqualTo("pong");
        assertThat(json.getString("businessType")).isEqualTo("ping");
        assertThat(json.getIntValue("executeTime")).isEqualTo(3);
        assertThat(json).containsKey("timestamp");
    }

    @Test
    void afterConnectionEstablishedShouldApplyConfiguredFrameSizeLimits() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("session.maxTextMessageSize", 1234);
        config.put("session.maxBinaryMessageSize", 4321);
        TestWebSocketHandler handler = TestWebSocketHandler.withConfig(config);
        TestWebSocketSession session = openSession("frames1", handshakeQuery("client-a"));

        handler.afterConnectionEstablished(session);

        assertThat(session.closeStatus).isNull();
        assertThat(session.getTextMessageSizeLimit()).isEqualTo(1234);
        assertThat(session.getBinaryMessageSizeLimit()).isEqualTo(4321);
    }

    @Test
    void afterConnectionEstablishedShouldDefaultFrameSizeLimitsTo65536() throws Exception {
        Map<String, Object> config = new HashMap<>();
        TestWebSocketHandler handler = TestWebSocketHandler.withConfig(config);
        TestWebSocketSession session = openSession("frames2", handshakeQuery("client-a"));

        handler.afterConnectionEstablished(session);

        assertThat(session.getTextMessageSizeLimit()).isEqualTo(65536);
        assertThat(session.getBinaryMessageSizeLimit()).isEqualTo(65536);
    }

    @Test
    void afterConnectionEstablishedShouldRejectWhenTotalSessionLimitReached() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("session.maxTotalSessions", 1);
        TestWebSocketHandler handler = TestWebSocketHandler.withConfig(config);

        TestWebSocketSession first = openSession("total1", handshakeQuery("client-a"));
        handler.afterConnectionEstablished(first);
        assertThat(first.closeStatus).isNull();
        assertThat(handler.sessionCount()).isEqualTo(1);

        TestWebSocketSession second = openSession("total2", handshakeQuery("client-b"));
        handler.afterConnectionEstablished(second);

        assertThat(second.closeStatus).isNotNull();
        assertThat(second.closeStatus.getCode()).isEqualTo(CloseStatus.SERVICE_OVERLOAD.getCode());
        assertThat(handler.sessionCount()).isEqualTo(1);
    }

    @Test
    void afterConnectionEstablishedShouldRejectWhenPerClientSessionLimitReached() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("session.maxTotalSessions", 100);
        config.put("session.maxSessionsPerClient", 1);
        TestWebSocketHandler handler = TestWebSocketHandler.withConfig(config);

        TestWebSocketSession first = openSession("per1", handshakeQuery("client-a"));
        handler.afterConnectionEstablished(first);
        assertThat(first.closeStatus).isNull();

        TestWebSocketSession second = openSession("per2", handshakeQuery("client-a"));
        handler.afterConnectionEstablished(second);
        assertThat(second.closeStatus).isNotNull();
        assertThat(second.closeStatus.getCode()).isEqualTo(CloseStatus.SERVICE_OVERLOAD.getCode());

        // 上限只作用于同一 clientId，其它客户端不受影响
        TestWebSocketSession other = openSession("per3", handshakeQuery("client-b"));
        handler.afterConnectionEstablished(other);
        assertThat(other.closeStatus).isNull();
    }

    @Test
    void afterConnectionEstablishedShouldRejectMissingClientIdWithPolicyViolation() throws Exception {
        Map<String, Object> config = new HashMap<>();
        TestWebSocketHandler handler = TestWebSocketHandler.withConfig(config);
        TestWebSocketSession session = openSession("auth1", "");

        handler.afterConnectionEstablished(session);

        assertThat(session.closeStatus).isNotNull();
        assertThat(session.closeStatus.getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
    }

    @Test
    void cacheEvictionShouldNotCloseOpenSessionByDefault() throws Exception {
        TestWebSocketHandler handler = TestWebSocketHandler.withConfig(evictionConfig(false));
        TestWebSocketSession first = openSession("evict1", handshakeQuery("client-a"));
        handler.afterConnectionEstablished(first);

        // 登记第二个 clientId 触发 Guava maximumSize 驱逐 client-a 的缓存条目
        TestWebSocketSession second = openSession("evict2", handshakeQuery("client-b"));
        handler.afterConnectionEstablished(second);
        assertThat(second.closeStatus).isNull();

        Thread.sleep(200L);
        assertThat(first.isOpen()).as("默认不因缓存驱逐而关闭仍打开的长连接").isTrue();
        assertThat(first.closeStatus).isNull();
    }

    @Test
    void cacheEvictionShouldCloseOpenSessionWhenCloseOnEvictionEnabled() throws Exception {
        TestWebSocketHandler handler = TestWebSocketHandler.withConfig(evictionConfig(true));
        TestWebSocketSession first = openSession("evict3", handshakeQuery("client-a"));
        handler.afterConnectionEstablished(first);

        TestWebSocketSession second = openSession("evict4", handshakeQuery("client-b"));
        handler.afterConnectionEstablished(second);

        eventually(() -> first.closeStatus != null, 3000L);
        assertThat(first.closeStatus).as("closeOnCacheEviction=true 时恢复旧行为").isNotNull();
        assertThat(first.closeStatus.getCode()).isEqualTo(CloseStatus.SERVICE_OVERLOAD.getCode());
    }

    @Test
    void localBroadcastShouldDeliverSynchronouslyByDefault() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("appId", "app-a");
        TestWebSocketHandler handler = TestWebSocketHandler.withConfig(config);
        TestWebSocketSession session = openSession("bc1", handshakeQuery("client-a"));
        handler.afterConnectionEstablished(session);

        boolean accepted = handler.broadcastMessageToClientLocalOnly("app-a", "client-a", "sync-push");

        assertThat(accepted).isTrue();
        assertThat(session.textMessages).containsExactly("sync-push");
    }

    @Test
    void asyncBroadcastShouldStillDeliverToTargetSession() throws Exception {
        Map<String, Object> config = new HashMap<>();
        config.put("appId", "app-a");
        config.put("broadcast.async", true);
        TestWebSocketHandler handler = TestWebSocketHandler.withConfig(config);
        TestWebSocketSession session = openSession("bc2", handshakeQuery("client-a"));
        handler.afterConnectionEstablished(session);

        boolean accepted = handler.broadcastMessageToClientLocalOnly("app-a", "client-a", "async-push");

        assertThat(accepted).isTrue();
        eventually(() -> !session.textMessages.isEmpty(), 3000L);
        assertThat(session.textMessages).containsExactly("async-push");
    }

    private static Map<String, Object> evictionConfig(boolean closeOnEviction) {
        Map<String, Object> config = new HashMap<>();
        config.put("session.cache.maxClients", 1);
        config.put("session.cache.concurrencyLevel", 1);
        config.put("session.delayMillis", 0);
        config.put("session.closeOnCacheEviction", closeOnEviction);
        return config;
    }

    private static String handshakeQuery(String clientId) {
        return "?clientId=" + clientId;
    }

    private static void eventually(java.util.function.BooleanSupplier condition, long timeoutMillis)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (!condition.getAsBoolean() && System.currentTimeMillis() < deadline) {
            Thread.sleep(20L);
        }
    }

    private static WsSubscribeScript subscribe(String name) {
        return new WsSubscribeScript() {
            @Override
            public String getName() {
                return name;
            }

            @Override
            public String getBusinessId() {
                return name;
            }
        };
    }

    private static String errorCode(TestWebSocketSession session) {
        assertThat(session.textMessages).hasSize(1);
        return JSONObject.parseObject(session.textMessages.get(0)).getString("code");
    }

    private static TestWebSocketSession openSession(String id) {
        return new TestWebSocketSession(id);
    }

    private static TestWebSocketSession openSession(String id, String query) {
        return new TestWebSocketSession(id, URI.create("ws://localhost/ws/api" + query));
    }

    private static void registerSession(TestWebSocketHandler handler, TestWebSocketSession session,
                                        String appId, String clientId) throws Exception {
        Method addSession = AbstractWebSocketHandler.class.getDeclaredMethod(
                "addSession", String.class, WebSocketSession.class, String.class);
        addSession.setAccessible(true);
        addSession.invoke(handler, appId, session, clientId);
    }

    private static class TestWebSocketHandler extends AbstractWebSocketHandler {

        private static final ThreadLocal<Map<String, Object>> NEXT_CONFIG = new ThreadLocal<>();

        private final Map<String, Object> config;

        static TestWebSocketHandler withConfig(Map<String, Object> config) {
            NEXT_CONFIG.set(config);
            try {
                return new TestWebSocketHandler();
            } finally {
                NEXT_CONFIG.remove();
            }
        }

        TestWebSocketHandler() {
            super(null);
            this.config = NEXT_CONFIG.get();
        }

        TestWebSocketHandler withDispatcher(WsBusinessDispatcher dispatcher) {
            setBusinessDispatcher(dispatcher);
            return this;
        }

        @Override
        public Map<String, Object> getConfig() {
            // 构造期间实例字段尚未赋值，此时仍从 ThreadLocal 取
            Map<String, Object> pending = NEXT_CONFIG.get();
            if (pending != null) {
                return pending;
            }
            return config == null ? Collections.emptyMap() : config;
        }

        @Override
        void broadcastMessageToClient(String excludedServerId, String appId, String clientId, String message) {
        }

        @Override
        void broadcastMessageToAllClient(String excludedServerId, String appId, String message) {
        }

        @Override
        public int getMaxClientSessions() {
            return getConfigValueAsInt("session.cache.maxClients", 10000);
        }

        @Override
        public int getSessionCacheExpireMinutes() {
            return getConfigValueAsInt("session.cache.expireMinutes", 30);
        }

        @Override
        public int getCacheConcurrencyLevel() {
            return getConfigValueAsInt("session.cache.concurrencyLevel", 4);
        }
    }

    private static class TestWebSocketSession implements WebSocketSession {

        private final String id;
        private final Map<String, Object> attributes = new HashMap<>();
        private final List<String> textMessages = new ArrayList<>();
        private final List<byte[]> binaryMessages = new ArrayList<>();
        private final URI uri;
        private int textMessageSizeLimit;
        private int binaryMessageSizeLimit;
        private boolean open = true;
        private CloseStatus closeStatus;

        TestWebSocketSession(String id) {
            this(id, URI.create("ws://localhost/ws/api"));
        }

        TestWebSocketSession(String id, URI uri) {
            this.id = id;
            this.uri = uri;
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
            return uri;
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
            this.textMessageSizeLimit = messageSizeLimit;
        }

        @Override
        public int getTextMessageSizeLimit() {
            return textMessageSizeLimit;
        }

        @Override
        public void setBinaryMessageSizeLimit(int messageSizeLimit) {
            this.binaryMessageSizeLimit = messageSizeLimit;
        }

        @Override
        public int getBinaryMessageSizeLimit() {
            return binaryMessageSizeLimit;
        }

        @Override
        public List<org.springframework.web.socket.WebSocketExtension> getExtensions() {
            return Collections.emptyList();
        }

        @Override
        public void sendMessage(WebSocketMessage<?> message) {
            if (message instanceof TextMessage) {
                textMessages.add(((TextMessage) message).getPayload());
            } else if (message instanceof BinaryMessage) {
                ByteBuffer payload = ((BinaryMessage) message).getPayload();
                byte[] bytes = new byte[payload.remaining()];
                payload.get(bytes);
                binaryMessages.add(bytes);
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
