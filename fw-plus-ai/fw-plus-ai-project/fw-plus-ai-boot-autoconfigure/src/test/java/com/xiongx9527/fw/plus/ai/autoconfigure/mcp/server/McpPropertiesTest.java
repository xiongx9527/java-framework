package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

public class McpPropertiesTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(McpPropertiesTestConfiguration.class);

	@Test
	public void shouldApplyDefaultMcpProperties() {
		contextRunner.run(context -> {
			McpProperties properties = context.getBean(McpProperties.class);
			assertThat(properties.getTransport()).isEqualTo("http-sse");
			assertThat(properties.isEnabled()).isTrue();
			assertThat(properties.getBasePackages()).isNull();
			assertThat(properties.getEndpoint()).isEqualTo("/mcp");
			assertThat(properties.getServer().getName()).isEqualTo("fw-plus-mcp-server");
			assertThat(properties.getServer().getVersion()).isEqualTo("1.0.0");
		});
	}

	@Test
	public void shouldBindCustomServerAndTransport() {
		contextRunner
				.withPropertyValues(
						"fw-plus.ai.mcp.transport=streamable-http",
						"fw-plus.ai.mcp.enabled=false",
						"fw-plus.ai.mcp.base-packages=com.example.mcp,com.example.other",
						"fw-plus.ai.mcp.endpoint=/api/mcp",
						"fw-plus.ai.mcp.server.name=custom-server",
						"fw-plus.ai.mcp.server.version=2.0.0")
				.run(context -> {
					McpProperties properties = context.getBean(McpProperties.class);
					assertThat(properties.getTransport()).isEqualTo("streamable-http");
					assertThat(properties.isEnabled()).isFalse();
					assertThat(properties.getBasePackages()).isEqualTo("com.example.mcp,com.example.other");
					assertThat(properties.getEndpoint()).isEqualTo("/api/mcp");
					assertThat(properties.getServer().getName()).isEqualTo("custom-server");
					assertThat(properties.getServer().getVersion()).isEqualTo("2.0.0");
				});
	}

	@org.springframework.boot.context.properties.EnableConfigurationProperties(McpProperties.class)
	private static class McpPropertiesTestConfiguration {
	}
}
