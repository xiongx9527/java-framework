package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.notify.MagicMcpNotifier;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptMessage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpResourceInfo;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collections;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class MagicMcpRegistryNotificationTest {

    private MagicMcpNotifier notifier;

    private MagicMcpResourceMagicDynamicRegistry resourceRegistry;

    private MagicMcpPromptMagicDynamicRegistry promptRegistry;

    @BeforeEach
    void setUp() {
        notifier = mock(MagicMcpNotifier.class);
        MagicApiMcpCapabilityRegistrar capabilityRegistrar = mock(MagicApiMcpCapabilityRegistrar.class);
        resourceRegistry = new MagicMcpResourceMagicDynamicRegistry(
                new MagicMcpResourceInfoMagicResourceStorage(), notifier, capabilityRegistrar);
        promptRegistry = new MagicMcpPromptMagicDynamicRegistry(
                new MagicMcpPromptInfoMagicResourceStorage(), notifier, capabilityRegistrar);
    }

    @Test
    void newResourceShouldNotifyListChanged() {
        resourceRegistry.register(resource("mcp://readme", "readme"));

        verify(notifier).publishResourcesChanged();
        verify(notifier, never()).publishResourceUpdated("mcp://readme");
    }

    @Test
    void updatedResourceShouldNotifyResourceUpdated() {
        MagicMcpResourceInfo first = resource("mcp://readme", "readme");
        first.setUpdateTime(1000L);
        MagicMcpResourceInfo second = resource("mcp://readme", "readme updated");
        second.setUpdateTime(2000L);
        resourceRegistry.register(first);
        resourceRegistry.register(second);

        verify(notifier).publishResourcesChanged();
        verify(notifier).publishResourceUpdated("mcp://readme");
    }

    @Test
    void removedResourceShouldNotifyListChanged() {
        MagicMcpResourceInfo info = resource("mcp://readme", "readme");
        resourceRegistry.register(info);

        resourceRegistry.unregister(info);

        verify(notifier, times(2)).publishResourcesChanged();
    }

    @Test
    void promptChangesShouldNotifyPromptsChanged() {
        promptRegistry.register(prompt("summarize"));

        verify(notifier).publishPromptsChanged();
    }

    @Test
    void removedPromptShouldNotifyPromptsChanged() {
        MagicMcpPromptInfo info = prompt("summarize");
        promptRegistry.register(info);

        promptRegistry.unregister(info);

        verify(notifier, times(2)).publishPromptsChanged();
    }

    private MagicMcpResourceInfo resource(String uri, String name) {
        MagicMcpResourceInfo info = new MagicMcpResourceInfo();
        info.setId("resource-" + uri.hashCode() + "-" + name.hashCode());
        info.setUri(uri);
        info.setName(name);
        return info;
    }

    private MagicMcpPromptInfo prompt(String name) {
        MagicMcpPromptInfo info = new MagicMcpPromptInfo();
        info.setId("prompt-" + name);
        info.setName(name);
        MagicMcpPromptMessage message = new MagicMcpPromptMessage("user", "hello");
        info.setMessages(Collections.singletonList(message));
        return info;
    }
}
