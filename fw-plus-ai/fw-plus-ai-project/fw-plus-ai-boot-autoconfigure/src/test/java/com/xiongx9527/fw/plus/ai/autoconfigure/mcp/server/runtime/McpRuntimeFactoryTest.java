package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.runtime;

import com.ajaxjs.mcp.server.McpServer;
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
	void shouldRejectServerKeyCollidingWithDefaultKey() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = top();
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties defaultKey = new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		defaultKey.setKey("default");
		defaultKey.setEndpoint("/mcp/custom");
		top.getServers().add(defaultKey);

		assertThatThrownBy(() -> factory.createRuntimes(top, authService, emptyProvider()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("server key 'default' is declared more than once");
	}

	@Test
	void shouldRejectDuplicateKeyBetweenNamedServers() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = top();
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties weather = new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		weather.setKey("weather");
		weather.setEndpoint("/mcp/weather");
		top.getServers().add(weather);
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties weather2 = new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		weather2.setKey("weather");
		weather2.setEndpoint("/mcp/weather-2");
		top.getServers().add(weather2);

		assertThatThrownBy(() -> factory.createRuntimes(top, authService, emptyProvider()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("server key 'weather' is declared more than once");
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
	void shouldInheritTopLevelTransportEndpointAndBasePackagesWhenNamedServerOmitsThem() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = top();
		top.setTransport("streamable-http");
		top.setBasePackages("com.example.shared");
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties order =
				new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		order.setKey("order");
		order.setEndpoint("/mcp/order");
		top.getServers().add(order);

		List<McpServerRuntime> runtimes = factory.createRuntimes(top, authService, emptyProvider());

		McpServerRuntime orderRuntime = runtimes.get(1);
		assertThat(orderRuntime.getKey()).isEqualTo("order");
		assertThat(orderRuntime.getTransport()).isEqualTo("streamable-http");
		assertThat(orderRuntime.getEndpoint()).isEqualTo("/mcp/order");
		assertThat(orderRuntime.getBasePackages()).containsExactly("com.example.shared");
		assertThat(orderRuntime.getProperties().getServer().getName()).isEqualTo(top.getServer().getName());
		assertThat(orderRuntime.getProperties().getServer().getVersion()).isEqualTo(top.getServer().getVersion());
	}

	@Test
	void shouldFallBackToBuiltInTransportAndEndpointWhenTopLevelAndNamedServerOmitThem() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		top.setEnabled(true);
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties order =
				new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		order.setKey("order");
		order.setEndpoint("/mcp/order");
		top.getServers().add(order);

		List<McpServerRuntime> runtimes = factory.createRuntimes(top, authService, emptyProvider());

		assertThat(runtimes.get(0).getTransport()).isEqualTo("http-sse");
		assertThat(runtimes.get(0).getEndpoint()).isEqualTo("/mcp");
		assertThat(runtimes.get(1).getTransport()).isEqualTo("http-sse");
		assertThat(runtimes.get(1).getEndpoint()).isEqualTo("/mcp/order");
	}

	@Test
	void shouldRejectDuplicateEndpointWhenTransportCaseDiffers() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = top();
		top.setTransport("http-sse");
		top.setEndpoint("/mcp");
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties upper = new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		upper.setKey("upper");
		// 大小写不同的同一 transport，且继承顶层 endpoint，构成冲突
		upper.setTransport("HTTP-SSE");
		top.getServers().add(upper);

		assertThatThrownBy(() -> factory.createRuntimes(top, authService, emptyProvider()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("endpoint conflict");
	}

	@Test
	void shouldRejectUnsupportedTransport() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = top();
		// 拼写错误：必须 fail-fast，而非被静默当作 http-sse 处理
		top.setTransport("streamabla-http");

		assertThatThrownBy(() -> factory.createRuntimes(top, authService, emptyProvider()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("unsupported transport");
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

	@Test
	void shouldCreateServerThroughProvidedFactory() {
		List<McpServerRuntime> runtimes = new McpRuntimeFactory((config, featureMgr) -> new StubMcpServer())
				.createRuntimes(top(), authService, emptyProvider());

		assertThat(runtimes.get(0).getServer()).isInstanceOf(StubMcpServer.class);
	}

	@Test
	void shouldCreateDistinctServerInstancePerRuntime() {
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties top = top();
		com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties named =
				new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties();
		named.setKey("report");
		named.setTransport("http-sse");
		named.setEndpoint("/mcp-report");
		top.setServers(java.util.Collections.singletonList(named));

		List<McpServerRuntime> runtimes = new McpRuntimeFactory((config, featureMgr) -> new StubMcpServer())
				.createRuntimes(top, authService, emptyProvider());

		assertThat(runtimes).hasSize(2);
		assertThat(runtimes.get(0).getServer()).isNotSameAs(runtimes.get(1).getServer());
		assertThat(runtimes.get(0).getFeatureMgr()).isNotSameAs(runtimes.get(1).getFeatureMgr());
	}

	@Test
	void shouldBindRegistryAsServerTransportSoNotificationsCanBeBroadcast() {
		List<McpServerRuntime> runtimes = factory.createRuntimes(top(), authService, emptyProvider());

		assertThat(runtimes.get(0).getServer().getTransport()).isSameAs(runtimes.get(0).getRegistry());
	}

	private static final class StubMcpServer extends McpServer {
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
