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
                .setSubject("default")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key.getAlgorithm(), key.getSecretKey())
                .compact();

        assertThat(service.validateAndExtractAppIdFromToken(token)).isEqualTo("default");
    }

    @Test
    void tokenWithForeignSecretShouldBeRejected() {
        WsBaseService signer = service(config("token.secret", "01234567890123456789012345678901"));
        WsBaseService verifier = service(config("token.secret", "99999999999999999999999999999999"));
        WsBaseService.SecretKeyAndSignatureAlgorithm key = signer.getSecretKeyAndSignatureAlgorithm();
        String token = Jwts.builder()
                .setSubject("default")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key.getAlgorithm(), key.getSecretKey())
                .compact();

        assertThatThrownBy(() -> verifier.validateAndExtractAppIdFromToken(token))
                .isInstanceOf(RuntimeException.class);
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
