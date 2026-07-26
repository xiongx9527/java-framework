package com.xiongx9527.fw.plus.boot.starter.jdbc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MagicApiDruidFrontendClasspathTest {

	@Test
	void starterClasspathContainsDeclaredDruidFrontend() {
		assertNotNull(getClass().getClassLoader().getResource(
				"magic-editor/plugins/magic-druid.1.0.0.iife.js"));
	}
}
