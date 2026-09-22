package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter.WsAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.support.TestWebSocketSession;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.web.socket.CloseStatus;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 非 magic 应用的握手鉴权验收测试：仅引入 WS 地基模块时，
 * {@code token.secret} 校验在真实 Spring 上下文装配出的处理器上生效。
 *
 * @author zzy
 */
class WsHandshakeAuthenticationTest {

    /**
     * 64 字节密钥，足以触发 HS512 分支，与生产环境的算法选择路径一致。
     */
    private static final String SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private static final String FOREIGN_SECRET =
            "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class, WsAutoConfiguration.class));

    @Test
    void validTokenShouldRegisterSession() {
        runner.withPropertyValues("fw.plus.ws.token.secret=" + SECRET).run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            TestWebSocketSession session = new TestWebSocketSession("s-ok")
                    .handshakeUri("ws://localhost/ws/api?token=" + issue(handler, "app-a") + "&clientId=c1");

            handler.afterConnectionEstablished(session);

            assertThat(session.closeStatus()).isNull();
            assertThat(session.isOpen()).isTrue();
            assertThat(session.attributesMap()).containsKey("sessionInfo");
        });
    }

    @Test
    void missingTokenShouldCloseWithPolicyViolation() {
        runner.withPropertyValues("fw.plus.ws.token.secret=" + SECRET).run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            TestWebSocketSession session = new TestWebSocketSession("s-no-token")
                    .handshakeUri("ws://localhost/ws/api?clientId=c1");

            handler.afterConnectionEstablished(session);

            assertThat(session.closeStatus().getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
            assertThat(session.attributesMap()).doesNotContainKey("sessionInfo");
        });
    }

    @Test
    void missingClientIdShouldCloseWithPolicyViolation() {
        runner.withPropertyValues("fw.plus.ws.token.secret=" + SECRET).run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            TestWebSocketSession session = new TestWebSocketSession("s-no-client")
                    .handshakeUri("ws://localhost/ws/api?token=" + issue(handler, "app-a"));

            handler.afterConnectionEstablished(session);

            assertThat(session.closeStatus().getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
            assertThat(session.attributesMap()).doesNotContainKey("sessionInfo");
        });
    }

    @Test
    void tokenSignedByForeignSecretShouldCloseWithPolicyViolation() {
        runner.withPropertyValues("fw.plus.ws.token.secret=" + SECRET).run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            String foreignToken = Jwts.builder().setSubject("app-a")
                    .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                                    FOREIGN_SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8)),
                            io.jsonwebtoken.SignatureAlgorithm.HS512)
                    .compact();
            TestWebSocketSession session = new TestWebSocketSession("s-foreign")
                    .handshakeUri("ws://localhost/ws/api?token=" + foreignToken + "&clientId=c1");

            handler.afterConnectionEstablished(session);

            assertThat(session.closeStatus().getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
            assertThat(session.attributesMap()).doesNotContainKey("sessionInfo");
        });
    }

    @Test
    void missingTokenSecretShouldRejectHandshake() {
        runner.run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            TestWebSocketSession session = new TestWebSocketSession("s-no-secret")
                    .handshakeUri("ws://localhost/ws/api?token=irrelevant&clientId=c1");

            handler.afterConnectionEstablished(session);

            assertThat(session.closeStatus().getCode()).isEqualTo(CloseStatus.POLICY_VIOLATION.getCode());
            assertThat(session.attributesMap()).doesNotContainKey("sessionInfo");
        });
    }

    @Test
    void customTokenParamNameShouldBeHonored() {
        runner.withPropertyValues("fw.plus.ws.token.secret=" + SECRET,
                        "fw.plus.ws.param.token=jwt",
                        "fw.plus.ws.param.clientId=cid")
                .run(context -> {
                    AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
                    TestWebSocketSession session = new TestWebSocketSession("s-custom-param")
                            .handshakeUri("ws://localhost/ws/api?jwt=" + issue(handler, "app-b") + "&cid=c9");

                    handler.afterConnectionEstablished(session);

                    assertThat(session.closeStatus()).isNull();
                    assertThat(session.attributesMap()).containsKey("sessionInfo");
                });
    }

    private static String issue(AbstractWebSocketHandler handler, String appId) {
        WsBaseService.SecretKeyAndSignatureAlgorithm keyAndAlgorithm =
                handler.getSecretKeyAndSignatureAlgorithm();
        return Jwts.builder()
                .setSubject(appId)
                .signWith(keyAndAlgorithm.getSecretKey(), keyAndAlgorithm.getAlgorithm())
                .compact();
    }
}
