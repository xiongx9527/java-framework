package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * WS 地基配置读取的验收测试：键名与缺省值须与改造前逐项一致。
 *
 * @author zzy
 */
class WsBaseServiceTest {

    @Test
    void defaultsShouldMatchFrozenContract() {
        WsBaseService service = service(new HashMap<>());

        assertThat(service.getWebsocketPath()).isEqualTo("/ws/api");
        assertThat(service.getTrustedDomain()).isEmpty();
        assertThat(service.getSubProtocols()).isEmpty();
        assertThat(service.getClientIdParamName()).isEqualTo("clientId");
        assertThat(service.getMaxClientIdLength()).isEqualTo(50);
        assertThat(service.getDefaultAppId()).isEqualTo("default");
        assertThat(service.getInitBusinessType()).isEqualTo("init");
        assertThat(service.getCloseBusinessType()).isEqualTo("close");
        assertThat(service.getBroadcastChannelName()).isEqualTo("fw-plus:websocket:broadcast");
        assertThat(service.getMaxClientSessions()).isEqualTo(10000);
        assertThat(service.getSessionCacheExpireMinutes()).isEqualTo(30);
        assertThat(service.getCacheConcurrencyLevel()).isEqualTo(16);
        assertThat(service.getDelayedClosePoolSize()).isEqualTo(2);
        assertThat(service.getDelayMillis()).isEqualTo(1000);
        assertThat(service.getSupportsPartialMessages()).isFalse();
        assertThat(service.getMaxTextMessageSize()).isEqualTo(65536);
        assertThat(service.getMaxBinaryMessageSize()).isEqualTo(65536);
        assertThat(service.getMaxTotalSessions()).isEqualTo(10000);
        assertThat(service.getMaxSessionsPerClient()).isEqualTo(100);
        assertThat(service.isIdleScanEnabled()).isFalse();
        assertThat(service.getIdleScanIntervalSeconds()).isEqualTo(60);
        assertThat(service.isCloseOnCacheEviction()).isFalse();
        assertThat(service.isBinaryEnabled()).isTrue();
        assertThat(service.getBinaryBusinessType()).isEqualTo("binary");
        assertThat(service.isBroadcastAsync()).isFalse();
        assertThat(service.getScriptThreadPoolCoreSize()).isEqualTo(8);
        assertThat(service.getScriptThreadPoolMaxSize()).isEqualTo(32);
        assertThat(service.getScriptThreadPoolQueueCapacity()).isEqualTo(1000);
    }

    @Test
    void subProtocolsShouldBeSplitAndTrimmed() {
        WsBaseService service = service(config("websocket.subProtocols", " chat , super-secret "));

        assertThat(service.getSubProtocols()).containsExactly("chat", "super-secret");
    }

    @Test
    void flatAndNestedConfigShouldBeEquivalent() {
        WsBaseService flat = service(config("session.cache.maxClients", 5));

        // 嵌套 Map 由 WsConfig.flatten() 展开，读取侧只认点号扁平键
        assertThat(flat.getConfigValueAsString("session.cache.maxClients", "fallback")).isEqualTo("5");
        assertThat(flat.getConfigValueAsInt("session.cache.maxClients", 10000)).isEqualTo(5);
        assertThat(flat.getConfigValueAsLong("session.cache.maxClients", 10000L)).isEqualTo(5L);
        assertThat(flat.getConfigValueAsString("missing.key", "fallback")).isEqualTo("fallback");
    }

    @Test
    void paramNameShouldFollowConfig() {
        WsBaseService service = service(config("param.clientId", "cid"));

        assertThat(service.getClientIdParamName()).isEqualTo("cid");
        assertThat(service.getParamNameFromConfig("param.clientId", "clientId")).isEqualTo("cid");
        assertThat(service.getParamNameFromConfig("param.missing", "clientId")).isEqualTo("clientId");
    }

    @Test
    void businessTypeShouldFollowConfig() {
        WsBaseService service = service(config("init.type", "open"));

        assertThat(service.getInitBusinessType()).isEqualTo("open");
        assertThat(service.getCloseBusinessType()).isEqualTo("close");
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
