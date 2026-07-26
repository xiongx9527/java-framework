package com.xiongx9527.fw.plus.boot.starter.magicapi.swagger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MagicApiSwaggerFrontendClasspathTest {

	@Test
	void pluginClasspathContainsDeclaredSwaggerFrontend() {
		assertNotNull(getClass().getClassLoader().getResource(
				"magic-editor/plugins/magic-swagger.1.0.0.iife.js"));
	}
}
