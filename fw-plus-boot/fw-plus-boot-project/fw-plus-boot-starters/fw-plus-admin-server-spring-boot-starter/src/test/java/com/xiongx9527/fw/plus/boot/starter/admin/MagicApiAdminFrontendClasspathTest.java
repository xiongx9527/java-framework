package com.xiongx9527.fw.plus.boot.starter.admin;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MagicApiAdminFrontendClasspathTest {

	@Test
	void starterClasspathContainsDeclaredAdminFrontend() {
		assertNotNull(getClass().getClassLoader().getResource(
				"magic-editor/plugins/magic-admin.1.0.0.iife.js"));
	}
}
