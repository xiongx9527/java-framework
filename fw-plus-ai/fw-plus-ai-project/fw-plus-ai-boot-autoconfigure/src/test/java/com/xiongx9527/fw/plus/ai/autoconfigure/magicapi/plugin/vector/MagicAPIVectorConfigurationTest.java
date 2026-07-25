package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.starter.MagicAPIVectorConfiguration;
import org.junit.Test;
import org.ssssssss.magicapi.core.model.Plugin;

import static org.junit.Assert.assertEquals;

public class MagicAPIVectorConfigurationTest {

    @Test
    public void shouldExposeSingleVectorPluginMetadata() {
        MagicAPIVectorConfiguration configuration = new MagicAPIVectorConfiguration();

        Plugin plugin = configuration.plugin();

        assertEquals("vector", plugin.getName());
        assertEquals("MagicVector", plugin.getGlobalName());
        assertEquals("magic-vector.1.0.0.iife.js", plugin.getJavascriptFilename());
    }
}
