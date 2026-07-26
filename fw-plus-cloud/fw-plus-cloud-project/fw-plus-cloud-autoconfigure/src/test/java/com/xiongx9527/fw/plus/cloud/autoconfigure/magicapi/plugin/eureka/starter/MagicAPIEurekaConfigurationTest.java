package com.xiongx9527.fw.plus.cloud.autoconfigure.magicapi.plugin.eureka.starter;

import org.ssssssss.magicapi.core.model.Plugin;

import java.lang.reflect.Field;

public class MagicAPIEurekaConfigurationTest {

	public void testEurekaUsesAttributesForConfiguredPath() throws Exception {
		MagicAPIEurekaConfiguration configuration = new MagicAPIEurekaConfiguration();
		Field path = MagicAPIEurekaConfiguration.class.getDeclaredField("eurekaDashboardPath");
		path.setAccessible(true);
		path.set(configuration, "/custom-console");

		Plugin plugin = configuration.plugin();

		assertEquals("MagicEureka", plugin.getGlobalName());
		assertEquals("magic-eureka.1.0.0.iife.js", plugin.getJavascriptFilename());
		assertEquals("/custom-console", plugin.getAttributes().get("path"));
	}

	private void assertEquals(Object expected, Object actual) {
		if (expected == null ? actual != null : !expected.equals(actual)) {
			throw new AssertionError("expected <" + expected + "> but was <" + actual + ">");
		}
	}
}
