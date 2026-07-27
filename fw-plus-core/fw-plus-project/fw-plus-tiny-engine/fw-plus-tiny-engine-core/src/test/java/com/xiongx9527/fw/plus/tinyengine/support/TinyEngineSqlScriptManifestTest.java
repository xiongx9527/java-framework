package com.xiongx9527.fw.plus.tinyengine.support;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TinyEngineSqlScriptManifestTest {

	private static final String MANIFEST_RESOURCE = "META-INF/tiny-engine/sql/manifest.properties";
	private static final String MANIFEST_CLASS = "com.xiongx9527.fw.plus.tinyengine.support.sql.TinyEngineSqlScriptManifest";
	private static final String[] DATABASES = {"mysql", "h2", "postgresql"};

	@Test
	public void shouldPackageSqlScriptManifest() {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(MANIFEST_RESOURCE);
		assertNotNull(inputStream, "SQL manifest 资源不存在");
	}

	@Test
	public void shouldExposeOrderedClasspathScriptsForSupportedDatabases() throws Exception {
		Object manifest = newManifest();
		Method getScripts = manifest.getClass().getMethod("getScripts", String.class);
		for (String database : DATABASES) {
			@SuppressWarnings("unchecked")
			List<String> scripts = (List<String>) getScripts.invoke(manifest, database);
			assertFalse(scripts.isEmpty(), database + " 至少应包含一个 SQL 脚本");
			assertScriptsAreOrdered(database, scripts);
			for (String script : scripts) {
				assertTrue(script.startsWith("classpath:META-INF/tiny-engine/sql/" + database + "/"),
					database + " 脚本必须是本模块 classpath 资源: " + script);
				String resourceName = script.substring("classpath:".length());
				assertNotNull(getClass().getClassLoader().getResourceAsStream(resourceName),
					database + " 脚本资源不存在: " + script);
			}
		}
	}

	@Test
	public void shouldReturnEmptyListForUnknownDatabase() throws Exception {
		Object manifest = newManifest();
		Method getScripts = manifest.getClass().getMethod("getScripts", String.class);
		@SuppressWarnings("unchecked")
		List<String> scripts = (List<String>) getScripts.invoke(manifest, "oracle");
		assertTrue(scripts.isEmpty(), "未知数据库必须返回空脚本列表");
	}

	private void assertScriptsAreOrdered(String database, List<String> scripts) {
		String previous = "";
		for (String script : scripts) {
			assertTrue(previous.compareTo(script) <= 0, database + " 脚本顺序必须稳定");
			previous = script;
		}
		assertEquals(scripts.size(), new java.util.LinkedHashSet<String>(scripts).size(), database + " 不得重复声明脚本");
	}

	private Object newManifest() throws Exception {
		return Class.forName(MANIFEST_CLASS).getDeclaredConstructor().newInstance();
	}
}
