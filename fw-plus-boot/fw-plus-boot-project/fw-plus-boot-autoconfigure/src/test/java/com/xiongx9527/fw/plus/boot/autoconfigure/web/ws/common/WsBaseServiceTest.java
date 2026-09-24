package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * WS 地基配置读取与 JWT 语义的验收测试（键名与缺省值须与拆分前逐项一致）。
 *
 * @author zzy
 */
class WsBaseServiceTest {

    @Test
    void defaultsShouldMatchFrozenContract() {
        WsBaseService service = service(new HashMap<>());

        assertThat(service.getWebsocketPath()).isEqualTo("/ws/api");
        assertThat(service.getTrustedDomain()).isEmpty();
        assertThat(service.getTokenExpireHours()).isEqualTo(2L);
        assertThat(service.getMaxClientIdLength()).isEqualTo(50);
        assertThat(service.getTokenParamName()).isEqualTo("token");
        assertThat(service.getClientIdParamName()).isEqualTo("clientId");
        assertThat(service.getDefaultAppId()).isEqualTo("default");
        assertThat(service.getInitBusinessType()).isEqualTo("init");
        assertThat(service.getCloseBusinessType()).isEqualTo("close");
        assertThat(service.getBroadcastChannelName()).isEqualTo("websocket:broadcast");
        assertThat(service.getMaxClientSessions()).isEqualTo(10000);
        assertThat(service.getSessionCacheExpireMinutes()).isEqualTo(30);
        assertThat(service.getCacheConcurrencyLevel()).isEqualTo(16);
        assertThat(service.getDelayedClosePoolSize()).isEqualTo(2);
        assertThat(service.getDelayMillis()).isEqualTo(1000);
        assertThat(service.getSupportsPartialMessages()).isFalse();
        assertThat(service.getSubProtocols()).isEmpty();
    }

    @Test
    void flatAndNestedConfigShouldBeEquivalent() {
        WsBaseService flat = service(config("token.secret", "s3cr3t-value"));
        assertThat(flat.getTokenSecret()).isEqualTo("s3cr3t-value");

        // 嵌套 Map 由 WsConfig.flatten() 展开，读取侧只认点号扁平键
        assertThat(flat.getConfigValueAsString("token.secret", "fallback")).isEqualTo("s3cr3t-value");
        assertThat(flat.getConfigValueAsString("missing.key", "fallback")).isEqualTo("fallback");
    }

    @Test
    void tokenSecretIsMandatory() {
        WsBaseService service = service(new HashMap<>());

        assertThatThrownBy(service::getTokenSecret)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("fw.plus.ws.token.secret must be configured");
    }

    @Test
    void tokenShouldRoundTripSubjectWithConfiguredSecret() {
        WsBaseService service = service(config("token.secret", "01234567890123456789012345678901"));
        WsBaseService.SecretKeyAndSignatureAlgorithm key = service.getSecretKeyAndSignatureAlgorithm();
        String token = Jwts.builder()
                .subject("default")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key.getSecretKey())
                .compact();

        assertThat(service.validateAndExtractAppIdFromToken(token)).isEqualTo("default");
    }

    @Test
    void tokenWithForeignSecretShouldBeRejected() {
        WsBaseService signer = service(config("token.secret", "01234567890123456789012345678901"));
        WsBaseService verifier = service(config("token.secret", "99999999999999999999999999999999"));
        WsBaseService.SecretKeyAndSignatureAlgorithm key = signer.getSecretKeyAndSignatureAlgorithm();
        String token = Jwts.builder()
                .subject("default")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key.getSecretKey())
                .compact();

        assertThatThrownBy(() -> verifier.validateAndExtractAppIdFromToken(token))
                .isInstanceOf(RuntimeException.class);
    }

    /**
     * jjwt 迁移前的冻结证据：该 token 由已废弃的旧 API（{@code setSubject} / {@code setIssuedAt}
     * / {@code setExpiration} / {@code signWith(SignatureAlgorithm, SecretKey)}）签发，
     * 密钥 {@code 0123456789abcdef0123456789abcdef}（32 字节 → HS256），subject {@code app-frozen}，
     * iat 1735689600000、exp 2051222400000（远期，保证长期可解析）。
     *
     * 迁移后的 {@link WsBaseService#validateAndExtractAppIdFromToken(String)} 必须仍能解析出同一 appId。
     */
    private static final String FROZEN_TOKEN_ISSUED_BEFORE_MIGRATION =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcHAtZnJvemVuIiwiaWF0IjoxNzM1Njg5NjAwLCJleHAiOjIwNTEyMjI0MDB9.JnlkWZtYZXlDgb61pHNoSKHfQXZ6MfCBeRPwVenIWEU";

    /**
     * 与 {@link #FROZEN_TOKEN_ISSUED_BEFORE_MIGRATION} 同密钥、同 subject、同 iat，
     * 仅 exp 取 1735761600000（已过期）的冻结 token，用于覆盖校验链路的拒绝路径。
     */
    private static final String FROZEN_EXPIRED_TOKEN_ISSUED_BEFORE_MIGRATION =
            "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhcHAtZnJvemVuIiwiaWF0IjoxNzM1Njg5NjAwLCJleHAiOjE3MzU3NjE2MDB9.3-hiC8JUmCK0jW-Ze7tFKNllxXDv99ymEtKRHcWvaN0";

    @Test
    void tokenIssuedBeforeMigrationShouldStillResolveToSameAppId() {
        WsBaseService service = service(config("token.secret", "0123456789abcdef0123456789abcdef"));

        assertThat(service.validateAndExtractAppIdFromToken(FROZEN_TOKEN_ISSUED_BEFORE_MIGRATION))
                .isEqualTo("app-frozen");
    }

    @Test
    void expiredTokenShouldBeRejected() {
        WsBaseService service = service(config("token.secret", "0123456789abcdef0123456789abcdef"));

        assertThatThrownBy(() -> service.validateAndExtractAppIdFromToken(FROZEN_EXPIRED_TOKEN_ISSUED_BEFORE_MIGRATION))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void headerAlgorithmShouldFollowKeyLength() {
        // 32 / 48 / 64 字节依次走 HS256 / HS384 / HS512 分支，不足 32 字节时回落到 SHA-512 派生的 HS256
        assertThat(headerAlgorithmOf("0123456789abcdef0123456789abcdef")).isEqualTo("HS256");
        assertThat(headerAlgorithmOf("0123456789abcdef0123456789abcdef0123456789abcdef")).isEqualTo("HS384");
        assertThat(headerAlgorithmOf("0123456789abcdef0123456789abcdef0123456789abcdef"
                + "0123456789abcdef0123456789abcdef")).isEqualTo("HS512");
        assertThat(headerAlgorithmOf("too-short")).isEqualTo("HS512");
    }

    @Test
    void selectedAlgorithmShouldMatchTokenHeader() {
        WsBaseService service = service(config("token.secret",
                "0123456789abcdef0123456789abcdef0123456789abcdef"));
        String token = sign(service, "app-alg");

        assertThat(service.getSecretKeyAndSignatureAlgorithm().getAlgorithm().getValue())
                .isEqualTo(headerAlgorithmOfToken(service, token));
        assertThat(service.validateAndExtractAppIdFromToken(token)).isEqualTo("app-alg");
    }

    @Test
    void expireHoursShouldFollowConfig() {
        Map<String, Object> config = config("token.secret", "0123456789abcdef0123456789abcdef");
        config.put("token.expireHours", 12L);

        assertThat(service(config).getTokenExpireHours()).isEqualTo(12L);
    }

    private static String sign(WsBaseService service, String appId) {
        return Jwts.builder()
                .subject(appId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(service.getSecretKeyAndSignatureAlgorithm().getSecretKey())
                .compact();
    }

    private static String headerAlgorithmOf(String secret) {
        WsBaseService service = service(config("token.secret", secret));
        return headerAlgorithmOfToken(service, sign(service, "app-len"));
    }

    private static String headerAlgorithmOfToken(WsBaseService service, String token) {
        return Jwts.parser()
                .verifyWith(service.getSecretKeyAndSignatureAlgorithm().getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getHeader()
                .getAlgorithm();
    }

    private static Map<String, Object> config(String key, Object value) {
        Map<String, Object> config = new HashMap<>();
        config.put(key, value);
        return config;
    }

    private static WsBaseService service(Map<String, Object> config) {
        return new WsBaseService() {
            @Override
            public Map<String, Object> getConfig() {
                return config;
            }
        };
    }
}
