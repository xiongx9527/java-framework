package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.tools;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.ApiInfo;

import static org.assertj.core.api.Assertions.assertThat;

class McpToolScopeMatcherTest {

	@Test
	void shouldKeepApiDisabledByDefault() {
		McpToolScopeMatcher matcher = new McpToolScopeMatcher(new MagicAPIMcpProperties(), groupId -> "/admin");

		assertThat(matcher.matches(api("/orders/list", "GET", true))).isFalse();
	}

	@Test
	void shouldLetExcludePathWin() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		properties.getTools().getApi().setEnabled(true);
		properties.getTools().getApi().getIncludePaths().add("/orders/**");
		properties.getTools().getApi().getExcludePaths().add("/orders/**");
		McpToolScopeMatcher matcher = new McpToolScopeMatcher(properties, groupId -> "/admin");

		assertThat(matcher.matches(api("/orders/list", "GET", true))).isFalse();
	}

	@Test
	void shouldRequireExplicitOptInWhenConfigured() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		properties.getTools().getApi().setEnabled(true);
		properties.getTools().getApi().setRequireOptIn(true);
		McpToolScopeMatcher matcher = new McpToolScopeMatcher(properties, groupId -> "/admin");

		assertThat(matcher.matches(api("/orders/list", "GET", false))).isFalse();
		assertThat(matcher.matches(api("/orders/list", "GET", true))).isTrue();
	}

	private ApiInfo api(String path, String method, boolean optIn) {
		ApiInfo info = new ApiInfo();
		info.setGroupId("group-admin");
		info.setPath(path);
		info.setMethod(method);
		info.setAttribute("mcpTool", optIn);
		return info;
	}
}
