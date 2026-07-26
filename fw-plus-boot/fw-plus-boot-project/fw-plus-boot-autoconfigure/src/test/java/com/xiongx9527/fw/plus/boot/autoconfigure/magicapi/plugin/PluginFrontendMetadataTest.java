package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.admin.server.MagicAPIAdminServerConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.starter.MagicAPIDruidConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.MagicSwaggerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.ssssssss.magicapi.core.model.Plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluginFrontendMetadataTest {

	@Test
	void druidExposesFrontendFactoryMetadata() {
		Plugin plugin = new MagicAPIDruidConfiguration().plugin();

		assertFrontendMetadata(plugin, "MagicDruid", "magic-druid.1.0.0.iife.js");
	}

	@Test
	void swaggerExposesFrontendFactoryMetadata() {
		Plugin plugin = new MagicSwaggerConfiguration(null, null, null, null, null).plugin();

		assertFrontendMetadata(plugin, "MagicSwagger", "magic-swagger.1.0.0.iife.js");
	}

	@Test
	void adminUsesAttributesForConfiguredPath() {
		MagicAPIAdminServerConfiguration configuration = new MagicAPIAdminServerConfiguration();
		ReflectionTestUtils.setField(configuration, "springbootAdminContextPath", "/custom-console");

		Plugin plugin = configuration.plugin();

		assertFrontendMetadata(plugin, "MagicAdmin", "magic-admin.1.0.0.iife.js");
		assertEquals("/custom-console", plugin.getAttributes().get("path"));
	}

	private void assertFrontendMetadata(Plugin plugin, String globalName, String javascriptFilename) {
		assertEquals(globalName, plugin.getGlobalName());
		assertEquals(javascriptFilename, plugin.getJavascriptFilename());
	}
}
