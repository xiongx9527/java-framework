package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 非 magic 应用「仅引入 WS 地基模块即可用」的**真实 HTTP Upgrade** 验收测试。
 *
 * <p>{@code WsAutoConfigurationTest} 用 mock 注册表断言处理器被挂到配置指定的路径上，
 * 属于装配级证据。本测试补最后一环：起一个真实 Servlet 容器（随机端口），让一个真实
 * WebSocket 客户端发起 HTTP Upgrade，验证握手按 {@code token.secret} 放行、端点确实挂在
 * {@code fw.plus.ws.websocket.path} 指定的路径上，并在连接上完成一次 ping/pong 往返。</p>
 *
 * @author zzy
 */
@SpringBootTest(
        classes = WsEndpointHttpUpgradeTest.WsTestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "fw.plus.ws.token.secret=" + WsEndpointHttpUpgradeTest.SECRET,
                "fw.plus.ws.websocket.path=" + WsEndpointHttpUpgradeTest.ENDPOINT_PATH,
                "fw.plus.ws.websocket.trustedDomain=*"
        })
class WsEndpointHttpUpgradeTest {

    /**
     * 64 字节密钥，足以触发 HS512 分支，与生产环境的算法选择路径一致。
     */
    static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    /**
     * 刻意取一个非默认值，使"端点确实按配置注册"在真实握手层面也被验证。
     */
    static final String ENDPOINT_PATH = "/ws/e2e-http";

    private static final String APP_ID = "app-http";

    @LocalServerPort
    private int port;

    @Autowired
    private AbstractWebSocketHandler handler;

    /**
     * 只启用 WS 自动配置的最小应用（无 magic-api、无 Redis、无本模块其余自动配置）。
     *
     * <p>本测试与 WS 地基同处 {@code fw-plus-boot-autoconfigure} 模块，而该模块的
     * {@code META-INF/spring.factories} 另外注册了 14 条与 WS 无关的自动配置（magic-api、
     * jimu、mqtt、seata 等）。若此处用 {@code @EnableAutoConfiguration}，这些自动配置会被一并加载：
     * 其中 magic-api 的 vue3 插件在 {@code magic-api.web} 缺省时会把过滤器 url-pattern 拼成
     * {@code null/vue3/component/*}，令 Tomcat 启动失败 —— 这与被测能力（WS 握手）无关。
     * 故改为显式导入：只应用 WS 自动配置，外加内嵌 Servlet 容器起来所必需的三条 Spring Boot 标准自动配置
     * （{@code DispatcherServletAutoConfiguration} 让 Upgrade 请求经过 HandlerMapping、
     * {@code ServletWebServerFactoryAutoConfiguration} 提供内嵌 Tomcat、
     * {@code WebSocketServletAutoConfiguration} 装上 JSR-356 的 {@code WsSci}，
     * 缺任一条则握手分别表现为 404 或 500）。于是本用例证明的是「仅启用 WS 自动配置即可提供 WS 能力」，
     * 既不经由本模块其它能力，也不受本模块自动配置集合日后增减的影响。</p>
     *
     * <p>刻意不导入 {@code WsRedisConfiguration}，用于覆盖「无 Redis 时降级为单机广播」这条路径。</p>
     */
    @SpringBootConfiguration
    @ImportAutoConfiguration({
            WsAutoConfiguration.class,
            WebSocketServletAutoConfiguration.class,
            ServletWebServerFactoryAutoConfiguration.class,
            DispatcherServletAutoConfiguration.class
    })
    @EnableWebSocket
    static class WsTestApplication {
    }

    /**
     * 预热 Servlet 容器，隔离与本用例无关的惰性初始化耗时。
     *
     * <p>首个进入容器的请求会触发 DispatcherServlet 与 HandlerMapping 的惰性初始化；在负载较高的
     * 机器上这段耗时可能达到数秒。JSR-356 客户端在握手时对 HTTP 升级响应有约 5 秒读超时，
     * 若把这笔初始化开销算进握手窗口，用例会随机报
     * {@code The HTTP upgrade to WebSocket failed but partial data may have been received}。
     * 这里先用一次普通 HTTP GET 把容器预热，使后续断言只测量 WebSocket 升级本身；
     * 不断言该预热请求的状态码，也不改变本用例对握手鉴权与 ping/pong 往返的检查强度。</p>
     */
    @BeforeEach
    void warmUpServletContainer() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(
                    "http://localhost:" + port + ENDPOINT_PATH).openConnection();
            connection.setConnectTimeout(30_000);
            connection.setReadTimeout(30_000);
            connection.getResponseCode();
        } catch (Exception ignored) {
            // 预热失败不影响断言：真实握手仍会独立失败并给出明确原因
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Test
    void realHttpUpgradeOnConfiguredPathShouldHandshakeAndRoundTripMessage() throws Exception {
        List<String> received = new ArrayList<>();

        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session = client.doHandshake(new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession session, TextMessage message) {
                synchronized (received) {
                    received.add(message.getPayload());
                    received.notifyAll();
                }
            }
        }, "ws://localhost:" + port + ENDPOINT_PATH + "?token=" + issueToken() + "&clientId=client-http")
                .get(10, TimeUnit.SECONDS);
        try {
            assertThat(session.isOpen()).as("HTTP Upgrade 后连接应处于打开状态").isTrue();

            session.sendMessage(new TextMessage("ping"));

            String payload = awaitPayload(received, 5000L);
            assertThat(payload).as("连接应完成 ping/pong 往返").isEqualTo("pong");
        } finally {
            session.close();
        }
    }

    /**
     * 未携带 token 的真实握手应被拒绝：证明鉴权是在真实 Upgrade 链路上生效，而非仅存在于单元测试里。
     */
    @Test
    void realHttpUpgradeWithoutTokenShouldBeRejected() throws Exception {
        StandardWebSocketClient client = new StandardWebSocketClient();
        WebSocketSession session = client.doHandshake(new TextWebSocketHandler() {
        }, "ws://localhost:" + port + ENDPOINT_PATH + "?clientId=client-http").get(10, TimeUnit.SECONDS);

        try {
            // 握手本身按 HTTP 协议完成，随后被处理器以策略违规关闭
            awaitClosed(session, 5000L);
            assertThat(session.isOpen()).as("缺少 token 的连接最终应被关闭").isFalse();
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
    }

    private String issueToken() {
        WsBaseService.SecretKeyAndSignatureAlgorithm keyAndAlgorithm = handler.getSecretKeyAndSignatureAlgorithm();
        return Jwts.builder()
                .setSubject(APP_ID)
                .signWith(keyAndAlgorithm.getSecretKey(), keyAndAlgorithm.getAlgorithm())
                .compact();
    }

    private static String awaitPayload(List<String> received, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        synchronized (received) {
            while (received.isEmpty() && System.currentTimeMillis() < deadline) {
                received.wait(Math.max(1L, deadline - System.currentTimeMillis()));
            }
            return received.isEmpty() ? null : received.get(0);
        }
    }

    private static void awaitClosed(WebSocketSession session, long timeoutMillis) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        while (session.isOpen() && System.currentTimeMillis() < deadline) {
            Thread.sleep(50L);
        }
    }
}
