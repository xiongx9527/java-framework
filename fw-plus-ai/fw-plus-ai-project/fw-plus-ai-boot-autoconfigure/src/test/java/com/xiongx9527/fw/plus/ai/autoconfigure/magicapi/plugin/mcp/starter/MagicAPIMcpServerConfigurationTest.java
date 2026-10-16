package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MagicAPIMcpServerConfigurationTest {

    @Test
    void shouldExposeSingleMcpPluginMetadata() {
        MagicAPIMcpServerConfiguration configuration = new MagicAPIMcpServerConfiguration(new MagicApiMcpProperties());

        Plugin plugin = configuration.plugin();

        assertEquals("mcpserver", plugin.getName());
        assertEquals("MagicMcpServer", plugin.getGlobalName());
        assertEquals("magic-mcp-server.1.0.0.iife.js", plugin.getJavascriptFilename());
    }

    @Test
    void shouldExposeMcpControllerRegister() {
        MagicAPIMcpServerConfiguration configuration = new MagicAPIMcpServerConfiguration(new MagicApiMcpProperties());

        MagicControllerRegister controllerRegister = configuration.controllerRegister();

        assertNotNull(controllerRegister);
    }
}