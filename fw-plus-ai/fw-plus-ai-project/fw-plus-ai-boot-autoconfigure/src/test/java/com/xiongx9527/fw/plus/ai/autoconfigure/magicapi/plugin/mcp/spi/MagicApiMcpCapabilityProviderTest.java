package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.spi;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpPromptInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.model.MagicMcpResourceInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpTool;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MagicApiMcpCapabilityProviderTest {

    @Test
    void shouldDefaultToEmptyCapabilities() {
        MagicApiMcpCapabilityProvider provider = new MagicApiMcpCapabilityProvider() {
        };

        assertThat(provider.mcpTools()).isEmpty();
        assertThat(provider.mcpResources()).isEmpty();
        assertThat(provider.mcpPrompts()).isEmpty();
        assertThat(provider.mcpCompletions()).isEmpty();
    }

    @Test
    void shouldBuildCompletionStoreKey() {
        MagicApiMcpCapabilityProvider.CompletionEntry entry =
                new MagicApiMcpCapabilityProvider.CompletionEntry("prompt", "summarize", "text",
                        Arrays.asList("alpha"));

        assertThat(entry.storeKey()).isEqualTo("prompt:summarize:text");
        assertThat(entry.getValues()).containsExactly("alpha");
    }

    @Test
    void shouldExposeOverriddenCapabilities() {
        MagicApiMcpTool tool = new MagicApiMcpTool("tool-ping", "ping", "探活", "function", "GET", "/ping",
                "group-1", "/public", false, null);

        MagicMcpResourceInfo resource = new MagicMcpResourceInfo();
        resource.setUri("mcp://ping");
        resource.setName("ping");

        MagicMcpPromptInfo prompt = new MagicMcpPromptInfo();
        prompt.setName("greet");

        MagicApiMcpCapabilityProvider provider = new MagicApiMcpCapabilityProvider() {
            @Override
            public List<MagicApiMcpTool> mcpTools() {
                return Collections.singletonList(tool);
            }

            @Override
            public List<MagicMcpResourceInfo> mcpResources() {
                return Collections.singletonList(resource);
            }

            @Override
            public List<MagicMcpPromptInfo> mcpPrompts() {
                return Collections.singletonList(prompt);
            }
        };

        assertThat(provider.mcpTools()).containsExactly(tool);
        assertThat(provider.mcpResources()).containsExactly(resource);
        assertThat(provider.mcpPrompts()).containsExactly(prompt);
    }
}
