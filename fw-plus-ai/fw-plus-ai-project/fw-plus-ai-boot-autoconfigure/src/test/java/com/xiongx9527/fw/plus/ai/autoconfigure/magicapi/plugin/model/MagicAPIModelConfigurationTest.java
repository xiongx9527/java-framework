package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.starter.MagicAPIModelConfiguration;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.Plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MagicAPIModelConfigurationTest {

    @Test
    void exposesModelPluginMetadataAndControllerRegister() {
        MagicAPIModelConfiguration configuration = new MagicAPIModelConfiguration();

        Plugin plugin = configuration.plugin();

        assertEquals("model", plugin.getName());
        assertEquals("MagicModel", plugin.getGlobalName());
        assertEquals("magic-model.1.0.0.iife.js", plugin.getJavascriptFilename());
        assertNotNull(configuration.controllerRegister());
    }
}
