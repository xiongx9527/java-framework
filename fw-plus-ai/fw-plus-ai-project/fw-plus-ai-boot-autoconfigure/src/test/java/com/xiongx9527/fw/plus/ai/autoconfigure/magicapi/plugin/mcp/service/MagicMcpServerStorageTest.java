package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model.MagicMcpServerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MagicMcpServerStorageTest {

    private MagicResourceService magicResourceService;

    private MagicMcpServerInfoMagicResourceStorage storage;

    @BeforeEach
    void setUp() {
        magicResourceService = mock(MagicResourceService.class);
        when(magicResourceService.listFiles(anyString())).thenReturn(Collections.emptyList());
        storage = new MagicMcpServerInfoMagicResourceStorage();
        storage.setMagicResourceService(magicResourceService);
    }

    @Test
    void shouldExposeFolderAndType() {
        assertThat(storage.folder()).isEqualTo("mcp-server");
        assertThat(storage.suffix()).isEqualTo(".json");
        assertThat(storage.magicClass()).isEqualTo(MagicMcpServerInfo.class);
        assertThat(storage.requirePath()).isFalse();
        assertThat(storage.requiredScript()).isFalse();
    }

    @Test
    void shouldAcceptValidServerDefinition() {
        MagicMcpServerInfo info = new MagicMcpServerInfo();
        info.setKey("report");
        info.setName("报表服务");
        info.setTransport("streamable-http");
        info.setEndpoint("/mcp/report");

        storage.validate(info);
    }

    @Test
    void shouldRejectBlankKey() {
        MagicMcpServerInfo info = valid();
        info.setKey(null);

        assertThatThrownBy(() -> storage.validate(info)).hasMessageContaining("key不能为空");
    }

    @Test
    void shouldRejectKeyWithIllegalCharacters() {
        MagicMcpServerInfo info = valid();
        info.setKey("report mcp");

        assertThatThrownBy(() -> storage.validate(info)).hasMessageContaining("key");
    }

    @Test
    void shouldRejectBlankEndpoint() {
        MagicMcpServerInfo info = valid();
        info.setEndpoint(" ");

        assertThatThrownBy(() -> storage.validate(info)).hasMessageContaining("endpoint不能为空");
    }

    @Test
    void shouldRejectEndpointNotStartingWithSlash() {
        MagicMcpServerInfo info = valid();
        info.setEndpoint("mcp/report");

        assertThatThrownBy(() -> storage.validate(info)).hasMessageContaining("以 / 开头");
    }

    @Test
    void shouldRejectUnsupportedTransport() {
        MagicMcpServerInfo info = valid();
        info.setTransport("stdio");

        assertThatThrownBy(() -> storage.validate(info)).hasMessageContaining("transport");
    }

    @Test
    void shouldAcceptClasspathAvailableAuthProvider() {
        MagicMcpServerInfo info = valid();
        MagicApiMcpProperties.Auth auth = new MagicApiMcpProperties.Auth();
        auth.setProvider("permit-all");
        info.setAuth(auth);

        storage.validate(info);
    }

    @Test
    void shouldRejectLegacyCustomAuthProvider() {
        MagicMcpServerInfo info = valid();
        MagicApiMcpProperties.Auth auth = new MagicApiMcpProperties.Auth();
        auth.setProvider("custom");
        info.setAuth(auth);

        // 历史 custom 已移除，不允许保存
        assertThatThrownBy(() -> storage.validate(info)).hasMessageContaining("auth.provider");
    }

    @Test
    void shouldRejectUnknownAuthProvider() {
        MagicMcpServerInfo info = valid();
        MagicApiMcpProperties.Auth auth = new MagicApiMcpProperties.Auth();
        auth.setProvider("unknown");
        info.setAuth(auth);

        assertThatThrownBy(() -> storage.validate(info)).hasMessageContaining("auth.provider");
    }

    @Test
    void shouldAllowMissingAuthProvider() {
        MagicMcpServerInfo info = valid();

        // 未配置 provider：允许保存，但运行时该实例未认证（401）
        storage.validate(info);
    }

    @Test
    void shouldRejectDuplicatedKey() {
        MagicMcpServerInfo existing = valid();
        existing.setId("other");
        when(magicResourceService.listFiles(anyString()))
                .thenReturn(Collections.singletonList(existing));

        MagicMcpServerInfo info = valid();
        info.setKey("report");

        assertThatThrownBy(() -> storage.validate(info)).hasMessageContaining("已存在");
    }

    @Test
    void shouldRoundTripThroughJson() {
        MagicMcpServerInfo info = valid();
        MagicApiMcpProperties.Auth auth = new MagicApiMcpProperties.Auth();
        auth.setProvider("sa-token-login");
        info.setAuth(auth);
        byte[] bytes = storage.write(info);

        MagicMcpServerInfo restored = storage.read(bytes);

        assertThat(restored.getKey()).isEqualTo("report");
        assertThat(restored.getEndpoint()).isEqualTo("/mcp/report");
        assertThat(restored.getTransport()).isEqualTo("streamable-http");
        assertThat(restored.getAuth()).isNotNull();
        assertThat(restored.getAuth().getProvider()).isEqualTo("sa-token-login");
    }

    private MagicMcpServerInfo valid() {
        MagicMcpServerInfo info = new MagicMcpServerInfo();
        info.setKey("report");
        info.setTransport("streamable-http");
        info.setEndpoint("/mcp/report");
        info.setEnabled(true);
        return info;
    }
}
