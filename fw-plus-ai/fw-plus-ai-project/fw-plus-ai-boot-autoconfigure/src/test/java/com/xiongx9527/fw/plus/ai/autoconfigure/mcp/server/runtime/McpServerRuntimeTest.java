package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.server.support.DefaultServerSseRegistry;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class McpServerRuntimeTest {

	@Test
	void shouldNotShareSessionsBetweenTwoRuntimes() {
		DefaultServerSseRegistry registryA = new DefaultServerSseRegistry(mock(McpServer.class), "server-a");
		DefaultServerSseRegistry registryB = new DefaultServerSseRegistry(mock(McpServer.class), "server-b");

		McpServerRuntime runtimeA = runtime("weather", registryA);
		McpServerRuntime runtimeB = runtime("order", registryB);

		// 会话只属于 runtime A
		registryA.addConnections("session-1", new PrintWriter(new StringWriter()));

		assertThat(runtimeA.hasSession("session-1")).isTrue();
		assertThat(runtimeB.hasSession("session-1")).isFalse();
	}

	@Test
	void shouldReportNoSessionForUnsupportedRegistryType() {
		McpServerRuntime runtime = new McpServerRuntime("weird", new McpProperties(), mock(McpServer.class),
				mock(FeatureMgr.class), null, null, Collections.emptyList());

		assertThat(runtime.hasSession("any")).isFalse();
	}

	@Test
	void shouldExposeEndpointAndTransportFromEffectiveProperties() {
		McpProperties properties = new McpProperties();
		properties.setEndpoint("/mcp/weather");
		properties.setTransport("http-sse");
		McpServerRuntime runtime = runtime("weather", new DefaultServerSseRegistry(mock(McpServer.class), "server-a"));

		McpServerRuntime configured = new McpServerRuntime("weather", properties, mock(McpServer.class),
				mock(FeatureMgr.class), null, null, Collections.emptyList());

		assertThat(configured.getEndpoint()).isEqualTo("/mcp/weather");
		assertThat(configured.getTransport()).isEqualTo("http-sse");
		// runtime() 用的默认 properties
		assertThat(runtime.getEndpoint()).isEqualTo("/mcp");
		assertThat(runtime.getKey()).isEqualTo("weather");
	}

	private McpServerRuntime runtime(String key, ServerSseRegistry registry) {
		return new McpServerRuntime(key, new McpProperties(), mock(McpServer.class), mock(FeatureMgr.class),
				registry, null, Collections.emptyList());
	}
}