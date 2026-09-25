package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter.WsAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter.WsRedisConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.support.TestWebSocketSession;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 非 magic 应用「Redis 可用时跨实例广播生效」的**真实端到端**验收测试。
 *
 * <p>{@code WsRedisConfigurationTest} 覆盖的是装配语义（mock 掉 Redis 模板与连接工厂、
 * 反射断言监听器绑定到广播频道）。本测试补上它无法覆盖的部分：接一个**真实 Redis**，
 * 让消息真的经过 Redis 的 pub/sub 往返一圈，验证「另一实例发布的消息能被本实例收到并
 * 投递到本地会话」以及「排除自身实例时不会被回声重复投递」。</p>
 *
 * <p>真实 Redis 由环境变量提供：{@code FW_WS_TEST_REDIS_HOST} / {@code FW_WS_TEST_REDIS_PORT}。
 * 未设置时整类测试自动跳过（本地无 Redis 的机器上不会失败）；一旦设置却连不上，则直接失败，
 * 避免把环境问题伪装成通过。</p>
 *
 * @author zzy
 */
class WsRedisBroadcastEndToEndTest {

    private static final String REDIS_HOST = System.getenv("FW_WS_TEST_REDIS_HOST");

    private static final String REDIS_PORT = System.getenv("FW_WS_TEST_REDIS_PORT");

    private static final String APP_ID = "app-e2e";

    /**
     * 代表"另一个应用实例"的服务器 ID。同一 JVM 内所有处理器实例的 serverId 都是
     * {@code hostname-pid}（见 {@code AbstractWebSocketHandler#generateWebSocketServerId}），
     * 因此跨实例语义只能通过显式传入一个不同的 excludedServerId 来构造。
     */
    private static final String OTHER_INSTANCE_SERVER_ID = "another-instance-server";

    @BeforeAll
    static void requireRealRedis() {
        Assumptions.assumeTrue(REDIS_HOST != null && !REDIS_HOST.isEmpty()
                        && REDIS_PORT != null && !REDIS_PORT.isEmpty(),
                "未设置 FW_WS_TEST_REDIS_HOST / FW_WS_TEST_REDIS_PORT，跳过真实 Redis 端到端验证");

        LettuceConnectionFactory probe = newConnectionFactory();
        try (RedisConnection connection = probe.getConnection()) {
            assertThat(connection.ping()).as("真实 Redis 可达性探测").isEqualTo("PONG");
        } finally {
            probe.destroy();
        }
    }

    /**
     * 每个测试各自拿一个新的连接工厂：上下文关闭时会销毁连接工厂，复用同一个实例会让后续测试断连。
     *
     * @return 指向真实 Redis 的连接工厂
     */
    private static LettuceConnectionFactory newConnectionFactory() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS_HOST, Integer.parseInt(REDIS_PORT)));
        factory.afterPropertiesSet();
        return factory;
    }

    private ApplicationContextRunner runner() {
        // 模板与连接工厂共用同一个工厂实例：由容器统一销毁，避免连接工厂泄漏；
        // Lettuce 默认共享原生连接，订阅与命令各自复用，不会额外放大连接数。
        LettuceConnectionFactory connectionFactory = newConnectionFactory();
        return new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class,
                        WsAutoConfiguration.class, WsRedisConfiguration.class))
                .withBean(RedisConnectionFactory.class, () -> connectionFactory)
                .withBean(StringRedisTemplate.class, () -> new StringRedisTemplate(connectionFactory));
    }

    /**
     * 另一实例发布的消息经真实 Redis 抵达本实例，并投递到本地会话。
     */
    @Test
    void messageFromAnotherInstanceShouldReachLocalSessionThroughRealRedis() {
        runner().run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            assertThat(handler).isInstanceOf(RedisWebSocketHandler.class);

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
        runner().run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            TestWebSocketSession session = registerSession(handler, "s-e2e-once");

            ((RedisWebSocketHandler) handler).broadcastMessageToClient(APP_ID, "client-e2e", "exactly-once");

            assertThat(session.textMessages()).contains("exactly-once");

            // 给足 Redis 往返时间：若回声未被排除，此处会出现第二次投递
            sleepQuietly(1500);
            long delivered = session.textMessages().stream().filter("exactly-once"::equals).count();
            assertThat(delivered).isEqualTo(1L);
        });
    }

    /**
     * 排除自身实例时，消息经真实 Redis 到达却不会被本实例投递。
     *
     * <p>先用一个不排除自身的消息证明链路是通的，再断言被排除的消息未到 —— 否则无法区分
     * "被正确排除"与"链路根本没通"。</p>
     */
    @Test
    void messageExcludedForCurrentInstanceShouldNotBeDelivered() {
        runner().run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
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
     * 广播给应用下所有客户端（clientId 为空的分支）也经真实 Redis 生效。
     */
    @Test
    void broadcastToAllClientsFromAnotherInstanceShouldReachLocalSession() {
        runner().run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            TestWebSocketSession session = registerSession(handler, "s-e2e-all");

            handler.broadcastMessageToAllClient(OTHER_INSTANCE_SERVER_ID, APP_ID, "broadcast-to-all");

            awaitMessage(session, "broadcast-to-all");
            assertThat(session.textMessages()).contains("broadcast-to-all");
        });
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
                .handshakeUri("ws://localhost/ws/api?clientId=client-e2e");

        handler.afterConnectionEstablished(session);

        assertThat(session.closeStatus()).as("握手应被放行").isNull();
        assertThat(session.attributesMap()).containsKey("sessionInfo");
        return session;
    }

    /**
     * 等待真实 Redis 完成异步投递；超时后不抛异常，交由调用处的断言给出明确失败信息。
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
