package com.xiongx9527.fw.plus.boot.starter.magicapi.javaentity;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.starter.MagicAPIJavaEntityConfiguration;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaEntityPluginRegistrationTest {

	@Test
	void registersConsolePluginWhenFrontendResourceExists() {
		MagicAPIJavaEntityConfiguration configuration = new MagicAPIJavaEntityConfiguration();

		assertTrue(configuration instanceof MagicPluginConfiguration);
		Plugin plugin = ((MagicPluginConfiguration) configuration).plugin();

		assertEquals("java-entity", plugin.getName());
		assertEquals("MagicJavaEntity", plugin.getGlobalName());
		assertEquals("magic-java-entity.1.0.0.iife.js", plugin.getJavascriptFilename());
	}
}
