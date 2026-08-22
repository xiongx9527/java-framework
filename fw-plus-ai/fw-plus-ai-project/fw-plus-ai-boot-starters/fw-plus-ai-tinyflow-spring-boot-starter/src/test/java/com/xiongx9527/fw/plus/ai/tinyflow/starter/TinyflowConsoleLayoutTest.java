package com.xiongx9527.fw.plus.ai.tinyflow.starter;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TinyflowConsoleLayoutTest {

	@Test
	public void shouldFollowMagicConsoleDirectoryLayout() {
		File console = new File("src/console");
		File legacyFrontend = new File("src/main/frontend/tinyflow-ui");

		assertTrue("Tinyflow frontend must live under src/console", console.isDirectory());
		assertTrue("Magic console layout must include package.json", new File(console, "package.json").isFile());
		assertTrue("Magic console layout must include src", new File(console, "src").isDirectory());
		assertFalse("Legacy frontend path must be removed", legacyFrontend.exists());
	}
}
