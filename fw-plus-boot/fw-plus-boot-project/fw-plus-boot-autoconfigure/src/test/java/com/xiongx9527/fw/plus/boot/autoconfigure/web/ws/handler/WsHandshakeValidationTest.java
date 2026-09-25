package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter.WsAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.support.TestWebSocketSession;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.web.socket.CloseStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 非 magic 应用的握手参数验收测试：仅引入 WS 地基模块时，clientId 校验与按端点路径
 * 解析实例 appId 的行为在真实 Spring 上下文装配出的处理器上生效。
 *
 * <p>地基自身不做 token / JWT 鉴权，鉴权由宿主应用在握手拦截器里实现，因此这里只覆盖
 * 地基负责的那一层：必要参数、长度限制、实例 appId 归属。</p>
 *
 * @author zzy
 */
class WsHandshakeValidationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class, WsAutoConfiguration.class));

    @Test
    void clientIdShouldEstablishSession() {
        runner.run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            TestWebSocketSession session = new TestWebSocketSession("s-ok")
                    .handshakeUri("ws://localhost/ws/api?clientId=c1");

            handler.afterConnectionEstablished(session);

            assertThat(session.closeStatus()).isNull();
            assertThat(session.isOpen()).isTrue();
            assertThat(session.attributesMap()).containsKey("sessionInfo");
            assertThat(appIdOf(session)).isEqualTo("default");
        });
    }

    @Test
    void missingClientIdShouldCloseWithPolicyViolation() {
        runner.run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            TestWebSocketSession session = new TestWebSocketSession("s-no-client")
                    .handshakeUri("ws://localhost/ws/api");

            handler.afterConnectionEstablished(session);

            assertThat(session.closeStatus().getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
            assertThat(session.attributesMap()).doesNotContainKey("sessionInfo");
        });
    }

    @Test
    void oversizedClientIdShouldCloseWithPolicyViolation() {
        runner.withPropertyValues("fw.plus.ws.param.maxClientIdLength=4").run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            TestWebSocketSession session = new TestWebSocketSession("s-long-client")
                    .handshakeUri("ws://localhost/ws/api?clientId=abcdefg");

            handler.afterConnectionEstablished(session);

            assertThat(session.closeStatus().getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
            assertThat(session.attributesMap()).doesNotContainKey("sessionInfo");
        });
    }

    @Test
    void customClientIdParamNameShouldBeHonored() {
        runner.withPropertyValues("fw.plus.ws.param.clientId=cid").run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            TestWebSocketSession session = new TestWebSocketSession("s-custom-param")
                    .handshakeUri("ws://localhost/ws/api?cid=c9");

            handler.afterConnectionEstablished(session);

            assertThat(session.closeStatus()).isNull();
            assertThat(session.attributesMap()).containsKey("sessionInfo");
        });
    }

    @Test
    void defaultAppIdShouldFollowConfig() {
        runner.withPropertyValues("fw.plus.ws.appId=foundation").run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            TestWebSocketSession session = new TestWebSocketSession("s-default-app")
                    .handshakeUri("ws://localhost/ws/api?clientId=c1");

            handler.afterConnectionEstablished(session);

            assertThat(appIdOf(session)).isEqualTo("foundation");
        });
    }

    @Test
    void instanceEndpointShouldResolveInstanceAppId() {
        runner.withPropertyValues(
                        "fw.plus.ws.servers[0].key=report",
                        "fw.plus.ws.servers[0].path=/ws/api2",
                        "fw.plus.ws.servers[0].appId=report")
                .run(context -> {
                    AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
                    TestWebSocketSession session = new TestWebSocketSession("s-instance")
                            .handshakeUri("ws://localhost/ws/api2?clientId=c1");

                    handler.afterConnectionEstablished(session);

                    assertThat(session.closeStatus()).isNull();
                    assertThat(appIdOf(session)).isEqualTo("report");
                });
    }

    /**
     * 实例追加端点同样归属该实例：请求路径带宿主基础路径，按后缀边界命中。
     */
    @Test
    void additionalInstanceEndpointShouldResolveInstanceAppId() {
        runner.withPropertyValues(
                        "fw.plus.ws.servers[0].key=ops",
                        "fw.plus.ws.servers[0].path=/ws/api2",
                        "fw.plus.ws.servers[0].paths=/ws/api3",
                        "fw.plus.ws.servers[0].appId=ops")
                .run(context -> {
                    AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
                    TestWebSocketSession session = new TestWebSocketSession("s-additional")
                            .handshakeUri("ws://localhost/magic-api/web/ws/api3?clientId=c1");

                    handler.afterConnectionEstablished(session);

                    assertThat(session.closeStatus()).isNull();
                    assertThat(appIdOf(session)).isEqualTo("ops");
                });
    }

    @Test
    void unmatchedEndpointShouldFallBackToDefaultAppId() {
        runner.withPropertyValues(
                        "fw.plus.ws.servers[0].key=report",
                        "fw.plus.ws.servers[0].path=/ws/api2",
                        "fw.plus.ws.servers[0].appId=report")
                .run(context -> {
                    AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
                    TestWebSocketSession session = new TestWebSocketSession("s-unmatched")
                            .handshakeUri("ws://localhost/ws/api?clientId=c1");

                    handler.afterConnectionEstablished(session);

                    assertThat(appIdOf(session)).isEqualTo("default");
                });
    }

    @Test
    void instanceWithoutAppIdShouldFallBackToDefaultAppId() {
        runner.withPropertyValues(
                        "fw.plus.ws.appId=foundation",
                        "fw.plus.ws.servers[0].key=report",
                        "fw.plus.ws.servers[0].path=/ws/api2",
                        "fw.plus.ws.servers[0].appId=")
                .run(context -> {
                    AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
                    TestWebSocketSession session = new TestWebSocketSession("s-no-app")
                            .handshakeUri("ws://localhost/ws/api2?clientId=c1");

                    handler.afterConnectionEstablished(session);

                    assertThat(appIdOf(session)).isEqualTo("foundation");
                });
    }

    /**
     * 从握手期写入的会话属性里取 appId，避免为了断言一个字段而在上下文里塞桩 dispatcher。
     */
    private static String appIdOf(TestWebSocketSession session) throws Exception {
        Object sessionInfo = session.attributesMap().get("sessionInfo");
        assertThat(sessionInfo).isNotNull();
        Object appId = sessionInfo.getClass().getMethod("getAppId").invoke(sessionInfo);
        return appId == null ? null : appId.toString();
    }
}
