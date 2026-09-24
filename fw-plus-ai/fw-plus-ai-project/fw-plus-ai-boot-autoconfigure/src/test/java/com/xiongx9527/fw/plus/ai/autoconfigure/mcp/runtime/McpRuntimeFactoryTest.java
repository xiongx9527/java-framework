package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.RedisServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.feature.McpFeatureScanner;
import com.xiongx9527.fw.plus.ai.mcp.support.DefaultServerSseRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class McpRuntimeFactoryTest {

	private final McpRuntimeFactory factory = new McpRuntimeFactory();

	private final McpAuthService authService = mock(McpAuthService.class);

	@Test
	void shouldCreateSingleDefaultRuntimeFromTopLevelConfig() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = top();
		top.setTransport("http-sse");
		top.setEndpoint("/mcp");
		top.setBasePackages("com.example.a");

		List<McpServerRuntime> runtimes = factory.createRuntimes(top, authService, emptyProvider());

		assertThat(runtimes).hasSize(1);
		McpServerRuntime runtime = runtimes.get(0);
		assertThat(runtime.getKey()).isEqualTo(McpFeatureScanner.DEFAULT_KEY);
		assertThat(runtime.getEndpoint()).isEqualTo("/mcp");
		assertThat(runtime.getTransport()).isEqualTo("http-sse");
		assertThat(runtime.getBasePackages()).containsExactly("com.example.a");
		assertThat(runtime.getRegistry()).isInstanceOf(DefaultServerSseRegistry.class);
	}

	@Test
	void shouldAppendNamedRuntimesAfterTheDefault() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = top();
		top.setTransport("http-sse");
		top.setEndpoint("/mcp");
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties weather = new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		weather.setKey("weather");
		weather.setTransport("streamable-http");
		weather.setEndpoint("/mcp/weather");
		top.getServers().add(weather);

		List<McpServerRuntime> runtimes = factory.createRuntimes(top, authService, emptyProvider());

		assertThat(runtimes).hasSize(2);
		assertThat(runtimes.get(0).getKey()).isEqualTo("default");
		assertThat(runtimes.get(1).getKey()).isEqualTo("weather");
		assertThat(runtimes.get(1).getTransport()).isEqualTo("streamable-http");
		assertThat(runtimes.get(1).getEndpoint()).isEqualTo("/mcp/weather");
	}

	@Test
	void shouldRejectNamedServerWithBlankKey() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = top();
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties blank = new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		blank.setKey(" ");
		blank.setEndpoint("/mcp/blank");
		top.getServers().add(blank);

		assertThatThrownBy(() -> factory.createRuntimes(top, authService, emptyProvider()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("key must not be blank");
	}

	@Test
	void shouldRejectDuplicateEndpointWithinSameTransport() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = top();
		top.setTransport("http-sse");
		top.setEndpoint("/mcp");
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties weather = new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		weather.setKey("weather");
		// 未配置 endpoint，继承顶层 /mcp，与 default 冲突
		top.getServers().add(weather);

		assertThatThrownBy(() -> factory.createRuntimes(top, authService, emptyProvider()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("endpoint conflict");
	}

	@Test
	void shouldAllowSameEndpointAcrossDifferentTransports() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = top();
		top.setTransport("http-sse");
		top.setEndpoint("/mcp");
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties streamable = new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		streamable.setKey("streamable");
		streamable.setTransport("streamable-http");
		streamable.setEndpoint("/mcp");
		top.getServers().add(streamable);

		List<McpServerRuntime> runtimes = factory.createRuntimes(top, authService, emptyProvider());

		assertThat(runtimes).hasSize(2);
		assertThat(runtimes.get(1).getEndpoint()).isEqualTo("/mcp");
	}

	@Test
	void shouldUseRedisRegistryWhenBroadcasterAvailable() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = top();
		top.setEndpoint("/mcp");

		McpRedisBroadcaster broadcaster = new McpRedisBroadcaster(null, "server-1");
		List<McpServerRuntime> runtimes = factory.createRuntimes(top, authService, provider(broadcaster));

		assertThat(runtimes.get(0).getRegistry()).isInstanceOf(RedisServerSseRegistry.class);
	}

	@Test
	void shouldPickLocalRegistryWhenBroadcasterMissing() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = top();
		top.setEndpoint("/mcp");

		List<McpServerRuntime> runtimes = factory.createRuntimes(top, authService, emptyProvider());

		assertThat(runtimes.get(0).getRegistry()).isInstanceOf(DefaultServerSseRegistry.class);
	}

	private com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top =
				new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		top.setEnabled(true);
		top.setTransport("http-sse");
		top.setEndpoint("/mcp");
		return top;
	}

	@SuppressWarnings("unchecked")
	private ObjectProvider<McpRedisBroadcaster> provider(McpRedisBroadcaster broadcaster) {
		ObjectProvider<McpRedisBroadcaster> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(broadcaster);
		return provider;
	}

	@SuppressWarnings("unchecked")
	private ObjectProvider<McpRedisBroadcaster> emptyProvider() {
		ObjectProvider<McpRedisBroadcaster> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(null);
		return provider;
	}
}