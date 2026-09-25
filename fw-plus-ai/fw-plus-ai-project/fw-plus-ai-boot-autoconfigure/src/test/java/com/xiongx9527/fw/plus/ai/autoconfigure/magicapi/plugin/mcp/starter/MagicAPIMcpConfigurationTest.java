package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MagicAPIMcpConfigurationTest {

    @Test
    void shouldExposeSingleMcpPluginMetadata() {
        MagicAPIMcpConfiguration configuration = new MagicAPIMcpConfiguration(new MagicApiMcpProperties());

        Plugin plugin = configuration.plugin();

        assertEquals("mcp", plugin.getName());
        assertEquals("MagicMcp", plugin.getGlobalName());
        assertEquals("magic-mcp.1.0.0.iife.js", plugin.getJavascriptFilename());
    }

    @Test
    void shouldExposeMcpControllerRegister() {
        MagicAPIMcpConfiguration configuration = new MagicAPIMcpConfiguration(new MagicApiMcpProperties());

        MagicControllerRegister controllerRegister = configuration.controllerRegister();

        assertNotNull(controllerRegister);
    }
}