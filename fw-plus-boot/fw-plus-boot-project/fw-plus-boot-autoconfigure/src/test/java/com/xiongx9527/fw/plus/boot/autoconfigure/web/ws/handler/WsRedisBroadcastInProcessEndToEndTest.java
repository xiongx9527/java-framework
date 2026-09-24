package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.redis.WsRedisMessageListenerContainer;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter.WsAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter.WsRedisConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.support.InProcessRedisConnectionFactory;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.support.TestWebSocketSession;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.assertj.AssertableApplicationContext;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 跨实例广播的**进程内端到端**验收测试。
 *
 * <p>{@code WsRedisBroadcastEndToEndTest} 接的是真实 Redis，未设置 {@code FW_WS_TEST_REDIS_HOST} /
 * {@code FW_WS_TEST_REDIS_PORT} 时整类跳过，因此本地与 CI 上「发布 → 订阅 → serverId 排除 → 本地投递」
 * 这条链路其实从未被执行过。本测试用一个进程内假 {@link RedisConnectionFactory} 补上这条证据：假工厂只
 * 替换连接与网络层，消息经 {@code StringRedisTemplate.convertAndSend} 发布后被回送给频道订阅者，随后由
 * <b>真实的</b> {@code WsRedisMessageListenerContainer}、其 {@code redis-ws-} 任务执行器与
 * {@code WsBroadcastMessageListener} 完成反序列化、排除与投递。</p>
 *
 * <p>因此不需要网络、外部进程或环境变量，四类行为（跨实例可达、定向恰投一次、排除自身不投递、广播全部可达）
 * 在每次 {@code mvn test} 中都实际执行。</p>
 *
 * @author zzy
 */
class WsRedisBroadcastInProcessEndToEndTest {

    /**
     * 64 字节密钥，足以触发 HS512 分支，与 {@code WsRedisBroadcastEndToEndTest} 保持一致。
     */
    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private static final String APP_ID = "app-e2e";

    /**
     * 代表"另一个应用实例"的服务器 ID。同一 JVM 内所有处理器实例的 serverId 都是
     * {@code hostname-pid}（见 {@code AbstractWebSocketHandler#generateWebSocketServerId}），
     * 因此跨实例语义只能通过显式传入一个不同的 excludedServerId 来构造。
     */
    private static final String OTHER_INSTANCE_SERVER_ID = "another-instance-server";

    /**
     * 默认广播频道（见 {@code WsBaseService#getBroadcastChannelName}）。
     */
    private static final String BROADCAST_CHANNEL = "fw-plus:websocket:broadcast";

    /**
     * 每个测试各自拿一个新的假连接工厂：上下文关闭时会关闭订阅连接，复用同一个实例会让后续测试收不到广播。
     *
     * @return 指向进程内假 Redis 的上下文运行器
     */
    private ApplicationContextRunner runner(InProcessRedisConnectionFactory connectionFactory) {
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class,
                        WsAutoConfiguration.class, WsRedisConfiguration.class))
                .withPropertyValues("fw.plus.ws.token.secret=" + SECRET)
                .withBean(RedisConnectionFactory.class, () -> connectionFactory)
                .withBean(StringRedisTemplate.class, () -> new StringRedisTemplate(connectionFactory));
    }

    /**
     * 另一实例发布的广播经 Redis 链路抵达本实例，并投递到本地会话。
     */
    @Test
    void messageFromAnotherInstanceShouldReachLocalSessionThroughInProcessRedis() {
        InProcessRedisConnectionFactory connectionFactory = new InProcessRedisConnectionFactory();
        runner(connectionFactory).run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            assertThat(handler).isInstanceOf(RedisWebSocketHandler.class);
            assertBroadcastSubscriptionActive(context, connectionFactory);

            TestWebSocketSession session = registerSession(handler, "s-e2e-reach");

            handler.broadcastMessageToClient(OTHER_INSTANCE_SERVER_ID, APP_ID, "client-e2e", "hello-from-other-instance");

            awaitMessage(session, "hello-from-other-instance");
            assertThat(session.textMessages()).contains("hello-from-other-instance");
        });
    }

    /**
     * 定向广播给指定客户端（public 入口）：本地立即投递一次，且 Redis 回声被自身实例排除逻辑挡住，
     * 不会造成二次投递（消息放大循环防护）。
     */
    @Test
    void targetedBroadcastShouldBeDeliveredLocallyExactlyOnce() {
        InProcessRedisConnectionFactory connectionFactory = new InProcessRedisConnectionFactory();
        runner(connectionFactory).run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            assertBroadcastSubscriptionActive(context, connectionFactory);

            TestWebSocketSession session = registerSession(handler, "s-e2e-once");

            ((RedisWebSocketHandler) handler).broadcastMessageToClient(APP_ID, "client-e2e", "exactly-once");

            assertThat(session.textMessages()).contains("exactly-once");

            // 给足回声往返时间：若回声未被排除，此处会出现第二次投递
            sleepQuietly(1500);
            long delivered = session.textMessages().stream().filter("exactly-once"::equals).count();
            assertThat(delivered).isEqualTo(1L);
        });
    }

    /**
     * 排除自身实例时，消息经 Redis 到达却不会被本实例投递。
     *
     * <p>先用一个不排除自身的消息证明链路是通的，再断言被排除的消息未到 —— 否则无法区分
     * "被正确排除"与"链路根本没通"。</p>
     */
    @Test
    void messageExcludedForCurrentInstanceShouldNotBeDelivered() {
        InProcessRedisConnectionFactory connectionFactory = new InProcessRedisConnectionFactory();
        runner(connectionFactory).run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            assertBroadcastSubscriptionActive(context, connectionFactory);

            TestWebSocketSession session = registerSession(handler, "s-e2e-excluded");

            // 对照：链路确实通
            handler.broadcastMessageToClient(OTHER_INSTANCE_SERVER_ID, APP_ID, "client-e2e", "reachable");
            awaitMessage(session, "reachable");
            assertThat(session.textMessages()).contains("reachable");

            // 断言项：排除自身实例后不应投递
            handler.broadcastMessageToClient(handler.getWebSocketServerId(), APP_ID, "client-e2e", "excluded-self");
            sleepQuietly(1500);
            assertThat(session.textMessages()).doesNotContain("excluded-self");
        });
    }

    /**
     * 广播给应用下所有客户端（clientId 为空的分支）也经 Redis 链路生效。
     */
    @Test
    void broadcastToAllClientsFromAnotherInstanceShouldReachLocalSession() {
        InProcessRedisConnectionFactory connectionFactory = new InProcessRedisConnectionFactory();
        runner(connectionFactory).run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            assertBroadcastSubscriptionActive(context, connectionFactory);

            TestWebSocketSession session = registerSession(handler, "s-e2e-all");

            handler.broadcastMessageToAllClient(OTHER_INSTANCE_SERVER_ID, APP_ID, "broadcast-to-all");

            awaitMessage(session, "broadcast-to-all");
            assertThat(session.textMessages()).contains("broadcast-to-all");
        });
    }

    /**
     * 断言监听容器真的在假 Redis 上完成了广播频道订阅。
     *
     * <p>这是进程内证据的关键护栏：没有它，"消息送达"可能只是本地投递，
     * 无法证明消息确实经过了容器链路。</p>
     *
     * @param context          当前上下文
     * @param connectionFactory 进程内假 Redis 连接工厂
     */
    private static void assertBroadcastSubscriptionActive(AssertableApplicationContext context,
            InProcessRedisConnectionFactory connectionFactory) {
        assertThat(context).hasSingleBean(WsRedisMessageListenerContainer.class);
        WsRedisMessageListenerContainer container = context.getBean(WsRedisMessageListenerContainer.class);

        assertThat(container.isRunning()).as("监听容器应随上下文启动").isTrue();
        assertThat(container.isListening()).as("监听容器应在假 Redis 上完成频道订阅").isTrue();
        assertThat(connectionFactory.subscriberCount(BROADCAST_CHANNEL))
                .as("假 Redis 应记录广播频道订阅").isEqualTo(1);
    }

    /**
     * 走一次真实握手把会话注册进处理器，返回可断言收包的会话桩。
     *
     * @param handler   目标处理器
     * @param sessionId 会话 ID
     * @return 已注册的会话
     */
    private static TestWebSocketSession registerSession(AbstractWebSocketHandler handler, String sessionId)
            throws Exception {
        TestWebSocketSession session = new TestWebSocketSession(sessionId)
                .handshakeUri("ws://localhost/ws/api?token=" + issueToken(handler) + "&clientId=client-e2e");

        handler.afterConnectionEstablished(session);

        assertThat(session.closeStatus()).as("握手应被放行").isNull();
        assertThat(session.attributesMap()).containsKey("sessionInfo");
        return session;
    }

    private static String issueToken(AbstractWebSocketHandler handler) {
        WsBaseService.SecretKeyAndSignatureAlgorithm keyAndAlgorithm = handler.getSecretKeyAndSignatureAlgorithm();
        return Jwts.builder()
                .subject(APP_ID)
                .signWith(keyAndAlgorithm.getSecretKey())
                .compact();
    }

    /**
     * 等待容器完成异步投递；超时后不抛异常，交由调用处的断言给出明确失败信息。
     */
    private static void awaitMessage(TestWebSocketSession session, String expected) {
        long deadline = System.currentTimeMillis() + 5000L;
        while (System.currentTimeMillis() < deadline) {
            if (session.textMessages().contains(expected)) {
                return;
            }
            sleepQuietly(50);
        }
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
