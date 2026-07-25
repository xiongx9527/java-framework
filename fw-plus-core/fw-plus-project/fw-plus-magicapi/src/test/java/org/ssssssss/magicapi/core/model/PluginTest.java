package org.ssssssss.magicapi.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PluginTest {

	@Test
	void attributesAreMutableAndEmptyByDefault() {
		Plugin plugin = new Plugin("sample");
		assertNotNull(plugin.getAttributes());
		assertTrue(plugin.getAttributes().isEmpty());
		plugin.getAttributes().put("path", "/admin");
		assertEquals("/admin", plugin.getAttributes().get("path"));
	}
}
