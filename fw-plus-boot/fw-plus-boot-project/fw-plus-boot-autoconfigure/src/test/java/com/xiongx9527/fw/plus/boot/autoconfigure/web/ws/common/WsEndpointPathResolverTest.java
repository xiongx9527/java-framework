package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 端点路径解析规则：webmvc 与 webflux 两条栈共用同一套拼接语义（Scenario A1/A5）。
 */
class WsEndpointPathResolverTest {

    @Test
    void normalizeShouldTrimAndPrefixLeadingSlash() {
        assertThat(WsEndpointPathResolver.normalize(" ws/api ")).isEqualTo("/ws/api");
        assertThat(WsEndpointPathResolver.normalize("/ws/api")).isEqualTo("/ws/api");
        assertThat(WsEndpointPathResolver.normalize(null)).isNull();
        assertThat(WsEndpointPathResolver.normalize("   ")).isNull();
    }

    @Test
    void relativePathsShouldKeepPrimaryFirstAndDropDuplicates() {
        List<String> relativePaths = WsEndpointPathResolver.resolveRelativePaths("/ws/api",
                Arrays.asList("/ws/api2", "/ws/api", " ws/api3 ", "  "));

        assertThat(relativePaths).containsExactly("/ws/api", "/ws/api2", "/ws/api3");
    }

    @Test
    void blankPrimaryPathShouldStayBlankWithoutWebsocketApiFallback() {
        assertThat(WsEndpointPathResolver.resolveRelativePaths("", Collections.singletonList("/ws/api2")))
                .containsExactly("", "/ws/api2");
    }

    @Test
    void joinBasePathShouldFixSlashAtJunction() {
        assertThat(WsEndpointPathResolver.joinBasePath("/magic-api", "/ws/api")).isEqualTo("/magic-api/ws/api");
        // 基础路径不带前导斜杠时保持原样，与拆分前 buildFullPath 的行为逐字节一致
        assertThat(WsEndpointPathResolver.joinBasePath("magic-api", "ws/api")).isEqualTo("magic-api/ws/api");
        assertThat(WsEndpointPathResolver.joinBasePath("/magic-api/", "/ws/api")).isEqualTo("/magic-api/ws/api");
        assertThat(WsEndpointPathResolver.joinBasePath("/magic-api/", "ws/api")).isEqualTo("/magic-api/ws/api");
        assertThat(WsEndpointPathResolver.joinBasePath("", "/ws/api")).isEqualTo("/ws/api");
        assertThat(WsEndpointPathResolver.joinBasePath(null, "/ws/api")).isEqualTo("/ws/api");
        // 相对路径为空串时补一个连接斜杠，与拆分前 webmvc 的表现一致
        assertThat(WsEndpointPathResolver.joinBasePath("/magic-api", "")).isEqualTo("/magic-api/");
    }

    @Test
    void fullPathsShouldExpandEveryBasePathAgainstEveryRelativePath() {
        List<String> relativePaths = WsEndpointPathResolver.resolveRelativePaths("/ws/api",
                Collections.singletonList("/ws/api2"));

        List<String> fullPaths = WsEndpointPathResolver.resolveFullPaths(relativePaths,
                Arrays.asList("/magic-api", "/magic-api", "/bff"));

        assertThat(fullPaths).containsExactly(
                "/magic-api/ws/api", "/magic-api/ws/api2", "/bff/ws/api", "/bff/ws/api2");
    }

    @Test
    void fullPathsWithoutBasePathShouldKeepRelativePathsOnly() {
        List<String> relativePaths = WsEndpointPathResolver.resolveRelativePaths("/ws/api", null);

        assertThat(WsEndpointPathResolver.resolveFullPaths(relativePaths, Collections.<String>emptyList()))
                .containsExactly("/ws/api");
        assertThat(WsEndpointPathResolver.resolveFullPaths(relativePaths, null)).containsExactly("/ws/api");
    }

    @Test
    void fullPathsShouldIgnoreBlankBasePath() {
        List<String> relativePaths = Collections.singletonList("/ws/api");

        assertThat(WsEndpointPathResolver.resolveFullPaths(relativePaths, Collections.singletonList("  ")))
                .containsExactly("/ws/api");
        assertThat(WsEndpointPathResolver.normalizeBasePath(" ")).isEmpty();
        assertThat(WsEndpointPathResolver.normalizeBasePath(null)).isEmpty();
    }
}
