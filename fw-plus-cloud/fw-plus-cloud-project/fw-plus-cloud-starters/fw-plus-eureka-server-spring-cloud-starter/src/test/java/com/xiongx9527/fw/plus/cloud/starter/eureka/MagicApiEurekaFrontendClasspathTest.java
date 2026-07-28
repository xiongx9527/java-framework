package com.xiongx9527.fw.plus.cloud.starter.eureka;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class MagicApiEurekaFrontendClasspathTest {

	@Test
	void starterClasspathContainsDeclaredEurekaFrontend() {
		assertNotNull(getClass().getClassLoader().getResource(
				"magic-editor/plugins/magic-eureka.1.0.0.iife.js"));
	}
}
