package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.admin.server.MagicAPIAdminServerConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.druid.starter.MagicAPIDruidConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.swagger.MagicSwaggerConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.ssssssss.magicapi.core.model.Plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 插件注册的三元组上报口径。
 *
 * <p>后端<strong>始终</strong>上报完整三元组（标识 + 全局名 + 前端产物名），不按前端产物
 * 是否落在 classpath 上改变上报内容。产物在不在 classpath 是使用方的依赖配置问题，
 * 编码进注册逻辑只会让「依赖漏声明」这类配置错误在运行期表现为「插件静默消失」，
 * 排查成本远高于控制台直接报 404。
 *
 * <p>因此这些断言不依赖测试进程的 classpath 上有没有前端产物——无论有没有，三元组都必须齐全。
 */
class PluginFrontendMetadataTest {

	@Test
	void druidAlwaysExposesFrontendIdentity() {
		Plugin plugin = new MagicAPIDruidConfiguration().plugin();

		assertFrontendIdentity(plugin, "druid", "MagicDruid", "magic-druid.1.0.0.iife.js");
	}

	@Test
	void swaggerAlwaysExposesFrontendIdentity() {
		Plugin plugin = new MagicSwaggerConfiguration(null, null, null, null, null, null, null).plugin();

		assertFrontendIdentity(plugin, "swagger", "MagicSwagger", "magic-swagger.1.0.0.iife.js");
	}

	@Test
	void adminAlwaysExposesFrontendIdentityAndConfiguredPath() {
		MagicAPIAdminServerConfiguration configuration = new MagicAPIAdminServerConfiguration();
		ReflectionTestUtils.setField(configuration, "springbootAdminContextPath", "/custom-console");

		Plugin plugin = configuration.plugin();

		assertFrontendIdentity(plugin, "admin", "MagicAdmin", "magic-admin.1.0.0.iife.js");
		assertEquals("/custom-console", plugin.getAttributes().get("path"));
	}

	private void assertFrontendIdentity(Plugin plugin, String name, String globalName, String javascriptFilename) {
		assertEquals(name, plugin.getName());
		assertEquals(globalName, plugin.getGlobalName());
		assertEquals(javascriptFilename, plugin.getJavascriptFilename());
	}
}