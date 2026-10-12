package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig.WsInstance;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 按端点路径解析命名实例：「一个实例 = 一个 key + 一组端点 + 一个 appId」的匹配语义。
 */
class WsInstanceResolverTest {

    @Test
    void relativeInstancePathShouldMatchSamePath() {
        WsConfig config = config(instance("report", "/ws/api2", "app2"));

        assertThat(WsInstanceResolver.resolveAppId(config, "/ws/api2")).isEqualTo("app2");
        assertThat(WsInstanceResolver.resolve(config, "/ws/api2").getKey()).isEqualTo("report");
    }

    @Test
    void fullInstancePathShouldMatchRequestWithBasePath() {
        WsConfig config = config(instance("report", "/magic-api/web/ws/api2", "app2"));

        assertThat(WsInstanceResolver.resolveAppId(config, "/magic-api/web/ws/api2")).isEqualTo("app2");
        // 实例写完整路径时，不同基础路径下的同名端点不应命中
        assertThat(WsInstanceResolver.resolveAppId(config, "/other/web/ws/api2")).isNull();
    }

    @Test
    void instancePathShouldMatchRequestPrefixedByBasePath() {
        WsConfig config = config(instance("report", "/ws/api2", "app2"));

        assertThat(WsInstanceResolver.resolveAppId(config, "/magic-api/web/ws/api2")).isEqualTo("app2");
    }

    @Test
    void additionalInstancePathShouldResolveToSameAppId() {
        WsConfig config = config(instance("report", "/ws/api2", "app2", "/ws/api3"));

        assertThat(WsInstanceResolver.resolveAppId(config, "/ws/api2")).isEqualTo("app2");
        assertThat(WsInstanceResolver.resolveAppId(config, "/ws/api3")).isEqualTo("app2");
    }

    @Test
    void suffixMatchShouldRequireSlashSegmentBoundary() {
        WsConfig config = config(instance("report", "/api", "app2"));

        // /my-api 的尾段是 -api，并非以 /api 结尾，不得命中
        assertThat(WsInstanceResolver.resolveAppId(config, "/my-api")).isNull();
        assertThat(WsInstanceResolver.resolveAppId(config, "/web/ws/api")).isEqualTo("app2");
    }

    @Test
    void exactMatchShouldWinOverSuffixMatch() {
        WsConfig config = config(
                instance("suffix", "/api", "app-suffix"),
                instance("exact", "/web/api", "app-exact"));

        // 精确匹配即使排在后面，也应优先于先配置的后缀匹配命中
        assertThat(WsInstanceResolver.resolveAppId(config, "/web/api")).isEqualTo("app-exact");
    }

    @Test
    void unmatchedPathShouldResolveToNullSoCallerFallsBackToDefaultAppId() {
        WsConfig config = config(instance("report", "/ws/api2", "app2"));

        assertThat(WsInstanceResolver.resolveAppId(config, "/ws/api")).isNull();
        assertThat(WsInstanceResolver.resolveAppId(config, null)).isNull();
        assertThat(WsInstanceResolver.resolveAppId(config, "  ")).isNull();
        assertThat(WsInstanceResolver.resolve(config, null)).isNull();
        assertThat(WsInstanceResolver.resolve(null, "/ws/api2")).isNull();
    }

    @Test
    void blankInstanceAppIdShouldResolveToNull() {
        WsConfig config = config(instance("report", "/ws/api2", "   "));

        // 实例本身仍被命中，只是 appId 留空表示回落全局 appId
        assertThat(WsInstanceResolver.resolve(config, "/ws/api2")).isNotNull();
        assertThat(WsInstanceResolver.resolveAppId(config, "/ws/api2")).isNull();
    }

    @Test
    void trailingSlashAndQueryShouldHitSameInstance() {
        WsConfig config = config(instance("report", "/ws/api2", "app2"));

        assertThat(WsInstanceResolver.resolveAppId(config, "/ws/api2/")).isEqualTo("app2");
        assertThat(WsInstanceResolver.resolveAppId(config, "/ws/api2?clientId=1")).isEqualTo("app2");
    }

    @Test
    void noInstancesShouldResolveToNull() {
        WsConfig config = new WsConfig();

        assertThat(WsInstanceResolver.resolveAppId(config, "/ws/api2")).isNull();
        assertThat(WsInstanceResolver.resolve(null, "/ws/api2")).isNull();
    }

    @Test
    void firstConfiguredInstanceShouldWinForAmbiguousSuffix() {
        WsConfig config = config(
                instance("first", "/ws/api", "app-first"),
                instance("second", "/api", "app-second"));

        assertThat(WsInstanceResolver.resolveAppId(config, "/bff/ws/api")).isEqualTo("app-first");
    }

    private static WsConfig config(WsInstance... servers) {
        WsConfig config = new WsConfig();
        if (servers != null && servers.length > 0) {
            config.setServers(Arrays.asList(servers));
        } else {
            config.setServers(Collections.<WsInstance>emptyList());
        }
        return config;
    }

    private static WsInstance instance(String key, String path, String appId, String... additionalPaths) {
        WsInstance instance = new WsInstance();
        instance.setKey(key);
        instance.setPath(path);
        instance.setAppId(appId);
        instance.setPaths(new ArrayList<>(Arrays.asList(additionalPaths)));
        return instance;
    }
}
