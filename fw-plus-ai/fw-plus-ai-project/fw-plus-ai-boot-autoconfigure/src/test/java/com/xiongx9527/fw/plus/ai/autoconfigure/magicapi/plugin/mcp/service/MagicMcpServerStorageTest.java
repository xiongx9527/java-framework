package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.service;

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
        byte[] bytes = storage.write(info);

        MagicMcpServerInfo restored = storage.read(bytes);

        assertThat(restored.getKey()).isEqualTo("report");
        assertThat(restored.getEndpoint()).isEqualTo("/mcp/report");
        assertThat(restored.getTransport()).isEqualTo("streamable-http");
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
