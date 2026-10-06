package com.xiongx9527.fw.plus.cloud.autoconfigure.magicapi.plugin.eureka.starter;

import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.Plugin;

import java.lang.reflect.Field;

/**
 * Eureka 插件注册的元数据上报口径。
 *
 * <p>后端<strong>始终</strong>上报完整三元组（标识 + 全局名 + 前端产物名），不按前端产物
 * 是否落在 classpath 上改变上报内容——那属于使用方的依赖配置问题，编码进注册逻辑只会让
 * 「依赖漏声明」在运行期表现为「插件静默消失」，排查成本远高于控制台直接报 404。
 */
public class MagicAPIEurekaConfigurationTest {

	@Test
	public void testEurekaAlwaysExposesFrontendIdentityAndConfiguredPath() throws Exception {
		MagicAPIEurekaConfiguration configuration = new MagicAPIEurekaConfiguration();
		Field path = MagicAPIEurekaConfiguration.class.getDeclaredField("eurekaDashboardPath");
		path.setAccessible(true);
		path.set(configuration, "/custom-console");

		Plugin plugin = configuration.plugin();

		assertEquals("eureka", plugin.getName());
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