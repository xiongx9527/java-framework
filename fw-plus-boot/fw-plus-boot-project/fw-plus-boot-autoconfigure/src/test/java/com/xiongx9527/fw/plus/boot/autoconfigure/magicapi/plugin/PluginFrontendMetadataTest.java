package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.admin.server.MagicAPIAdminServerConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.starter.MagicAPIDruidConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.MagicSwaggerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.ssssssss.magicapi.core.model.Plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PluginFrontendMetadataTest {

	@Test
	void druidDoesNotExposeMissingFrontendResource() {
		Plugin plugin = new MagicAPIDruidConfiguration().plugin();

		assertBackendOnlyMetadata(plugin, "druid");
	}

	@Test
	void swaggerDoesNotExposeMissingFrontendResource() {
		Plugin plugin = new MagicSwaggerConfiguration(null, null, null, null, null).plugin();

		assertBackendOnlyMetadata(plugin, "swagger");
	}

	@Test
	void adminUsesAttributesForConfiguredPath() {
		MagicAPIAdminServerConfiguration configuration = new MagicAPIAdminServerConfiguration();
		ReflectionTestUtils.setField(configuration, "springbootAdminContextPath", "/custom-console");

		Plugin plugin = configuration.plugin();

		assertBackendOnlyMetadata(plugin, "admin");
		assertEquals("/custom-console", plugin.getAttributes().get("path"));
	}

	private void assertBackendOnlyMetadata(Plugin plugin, String name) {
		assertEquals(name, plugin.getName());
		assertNull(plugin.getGlobalName());
		assertNull(plugin.getJavascriptFilename());
	}
}
