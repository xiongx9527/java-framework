package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicApiMcpPropertiesTest {

    @Test
    void providesDefaultsForPluginOwnKeys() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();

        assertTrue(properties.isEnabled());
        assertTrue(properties.getAuth().isEnabled());
        assertEquals("fw-plus-mcp", properties.getAuth().getResource());
        assertEquals("mcp:tools", properties.getAuth().getToolsScope());
        assertEquals("mcp:resources", properties.getAuth().getResourcesScope());
        assertEquals("mcp:prompts", properties.getAuth().getPromptsScope());

        assertTrue(properties.getTools().getFunctions().isEnabled());
        assertTrue(properties.getTools().getFunctions().getIncludeGroups().isEmpty());
        assertFalse(properties.getTools().getApi().isEnabled());
        assertFalse(properties.getTools().getApi().isRequireOptIn());
        assertEquals("mcpTool", properties.getTools().getApi().getOptInOptionName());

        assertTrue(properties.getServers().isEmpty());
    }

    @Test
    void bindsNestedEntries() {
        Map<String, String> source = new HashMap<>();
        source.put("magic-api.plugin.mcp.enabled", "false");
        source.put("magic-api.plugin.mcp.tools.functions.include-groups[0]", "/system/**");
        source.put("magic-api.plugin.mcp.tools.api.require-opt-in", "true");
        source.put("magic-api.plugin.mcp.tools.api.opt-in-option-name", "exposeToMcp");
        source.put("magic-api.plugin.mcp.auth.enabled", "false");
        source.put("magic-api.plugin.mcp.auth.resource", "custom-resource");
        source.put("magic-api.plugin.mcp.servers[0].key", "report");
        source.put("magic-api.plugin.mcp.servers[0].endpoint", "/mcp/report");
        source.put("magic-api.plugin.mcp.servers[0].transport", "streamable-http");
        source.put("magic-api.plugin.mcp.servers[0].tools.api.require-opt-in", "true");

        MagicApiMcpProperties properties = new Binder(new MapConfigurationPropertySource(source))
                .bind("magic-api.plugin.mcp", Bindable.of(MagicApiMcpProperties.class))
                .orElseThrow(IllegalStateException::new);

        assertFalse(properties.isEnabled());
        assertEquals(Arrays.asList("/system/**"), properties.getTools().getFunctions().getIncludeGroups());
        assertTrue(properties.getTools().getApi().isRequireOptIn());
        assertEquals("exposeToMcp", properties.getTools().getApi().getOptInOptionName());
        assertFalse(properties.getAuth().isEnabled());
        assertEquals("custom-resource", properties.getAuth().getResource());

        assertEquals(1, properties.getServers().size());
        MagicApiMcpProperties.Server server = properties.getServers().get(0);
        assertEquals("report", server.getKey());
        assertEquals("/mcp/report", server.getEndpoint());
        assertEquals("streamable-http", server.getTransport());
        assertTrue(server.getTools().getApi().isRequireOptIn());
    }

    @Test
    void transportDefaultsToHttpSseWhenNotConfigured() {
        MagicApiMcpProperties.Server server = new MagicApiMcpProperties.Server();

        assertEquals("http-sse", server.getTransport());
        assertNull(server.getEndpoint());
        assertTrue(server.getTools() == null);
    }
}
