package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify;

import com.ajaxjs.mcp.server.McpServer;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicMcpNotifierTest {

    private McpServer server;

    private MagicMcpNotifier notifier;

    @BeforeEach
    void setUp() {
        server = mock(McpServer.class);
        when(server.getTransport()).thenReturn(mock(com.ajaxjs.mcp.transport.McpTransportSync.class));

        McpServerRuntime runtime = mock(McpServerRuntime.class);
        when(runtime.getServer()).thenReturn(server);
        notifier = new MagicMcpNotifier(Collections.singletonList(runtime), null,
				org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
				org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class),
				mock(org.springframework.context.ApplicationEventPublisher.class));
    }

    @Test
    void shouldBroadcastListChangedPerCapability() {
        notifier.publishToolsChanged();
        notifier.publishResourcesChanged();
        notifier.publishPromptsChanged();

        verify(server).publishToolsChanged();
        verify(server).publishResourcesChanged();
        verify(server).publishPromptsChanged();
    }

    @Test
    void shouldBroadcastResourceUpdatedByUri() {
        notifier.publishResourceUpdated("mcp://readme");

        verify(server).publishResourceUpdated("mcp://readme");
    }

    @Test
    void shouldIgnoreBlankUriOnResourceUpdated() {
        notifier.publishResourceUpdated("  ");

        verify(server, never()).publishResourceUpdated(any());
    }

    @Test
    void shouldSkipServersWithoutTransport() {
        when(server.getTransport()).thenReturn(null);

        notifier.publishToolsChanged();
        notifier.publishResourcesChanged();

        verify(server, never()).publishToolsChanged();
        verify(server, never()).publishResourcesChanged();
    }
}
