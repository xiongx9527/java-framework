package com.xiongx9527.fw.plus.tinyengine.support;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

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
		close(inputStream);
	}

	@Test
	public void shouldExposeOrderedNonBlankClasspathScriptsForSupportedDatabases() throws Exception {
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
				InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
				assertNotNull(inputStream, database + " 脚本资源不存在: " + script);
				assertTrue(readContent(inputStream).trim().length() > 0, database + " 脚本不得为空白: " + script);
			}
		}
	}

	@Test
	public void shouldNotExposeTestFixturesInDefaultScripts() throws Exception {
		Object manifest = newManifest();
		Method getScripts = manifest.getClass().getMethod("getScripts", String.class);
		for (String database : new String[]{"mysql", "h2"}) {
			@SuppressWarnings("unchecked")
			List<String> scripts = (List<String>) getScripts.invoke(manifest, database);
			for (String script : scripts) {
				assertFalse(script.endsWith("/init_data_for_test_v1.0.0.sql"),
					database + " 默认初始化脚本不得包含测试夹具: " + script);
			}
		}
	}

	@Test
	public void shouldQuotePostgresqlReservedColumnNames() throws Exception {
		String resourceName = "META-INF/tiny-engine/sql/postgresql/create_all_tables_ddl_v1.postgresql.sql";
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourceName);
		assertNotNull(inputStream, "PostgreSQL 建表脚本资源不存在");
		String content = readContent(inputStream);
		assertTrue(content.contains("\"group\""), "PostgreSQL 建表脚本必须引用 group 列的保留字标识符");
		assertFalse(Pattern.compile("(?m)^\\s*group\\s+varchar").matcher(content).find(),
			"PostgreSQL 建表脚本不得保留未转义的 group 列");
	}

	@Test
	public void shouldReturnEmptyListForUnknownDatabase() throws Exception {
		Object manifest = newManifest();
		Method getScripts = manifest.getClass().getMethod("getScripts", String.class);
		@SuppressWarnings("unchecked")
		List<String> scripts = (List<String>) getScripts.invoke(manifest, "oracle");
		assertTrue(scripts.isEmpty(), "未知数据库必须返回空脚本列表");
	}

	private void assertScriptsAreOrdered(String database, List<String> scripts) throws Exception {
		Properties manifest = new Properties();
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(MANIFEST_RESOURCE);
		assertNotNull(inputStream, "SQL manifest 资源不存在");
		try {
			manifest.load(inputStream);
		} finally {
			inputStream.close();
		}

		List<String> keys = new ArrayList<String>();
		String prefix = database + ".";
		for (Object key : manifest.keySet()) {
			String propertyName = String.valueOf(key);
			if (propertyName.startsWith(prefix)) {
				keys.add(propertyName);
			}
		}
		Collections.sort(keys);
		List<String> expected = new ArrayList<String>(keys.size());
		for (String key : keys) {
			expected.add(manifest.getProperty(key));
		}
		assertEquals(expected, scripts, database + " 脚本必须遵循 manifest key 顺序");
		assertEquals(scripts.size(), new java.util.LinkedHashSet<String>(scripts).size(), database + " 不得重复声明脚本");
	}

	private Object newManifest() throws Exception {
		return Class.forName(MANIFEST_CLASS).getDeclaredConstructor().newInstance();
	}

	private String readContent(InputStream inputStream) throws Exception {
		try {
			java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int count;
			while ((count = inputStream.read(buffer)) >= 0) {
				outputStream.write(buffer, 0, count);
			}
			return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
		} finally {
			inputStream.close();
		}
	}

	private void close(InputStream inputStream) {
		try {
			inputStream.close();
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to close SQL manifest resource", exception);
		}
	}
}
