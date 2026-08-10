package com.xiongx9527.fw.plus.ai.tinyflow.starter;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class TinyflowUiClasspathTest {

	@Test
	public void shouldPackagePinnedUiAssetsAndLicense() {
		assertNotNull(getClass().getResource("/META-INF/resources/fw-plus/tinyflow-ui/1.3.6/tinyflow-ui.js"));
		assertNotNull(getClass().getResource("/META-INF/resources/fw-plus/tinyflow-ui/1.3.6/tinyflow-ui.css"));
		assertNotNull(getClass().getResource("/META-INF/resources/fw-plus/tinyflow-ui/NOTICE"));
	}
}
