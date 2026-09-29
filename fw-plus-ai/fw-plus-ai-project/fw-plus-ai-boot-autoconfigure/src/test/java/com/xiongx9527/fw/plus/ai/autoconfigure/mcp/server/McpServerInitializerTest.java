package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.BaseServerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationContext;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpServerInitializerTest {

	@Test
	void shouldResolveDefaultPackageFromBootApplicationClass() {
		Class<?> bootClass = McpServerInitializerTest.class;
		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBeanNamesForAnnotation(org.springframework.boot.SpringBootConfiguration.class))
				.thenReturn(new String[] { "sampleApplication" });
		doReturn(bootClass).when(context).getType("sampleApplication");
		McpServerRuntime runtime = runtime("default", new McpProperties(), null);

		List<String> packages = McpServerInitializer.resolveBasePackages(new McpProperties(), context,
				Collections.singletonList(runtime));

		assertThat(packages).containsExactly("com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server");
	}

	@Test
	void shouldResolveMultiplePackagesFromConfiguration() {
		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBeanNamesForAnnotation(org.springframework.boot.SpringBootConfiguration.class))
				.thenReturn(new String[0]);
		McpProperties properties = new McpProperties();
		properties.setBasePackages(" com.example.a , , com.example.b ");
		McpServerRuntime runtime = runtime("default", properties, null);

		List<String> packages = McpServerInitializer.resolveBasePackages(properties, context,
				Collections.singletonList(runtime));

		assertThat(packages).containsExactly("com.example.a", "com.example.b");
	}

	@Test
	void shouldMergeConfiguredPackagesWithDefaultBootPackage() {
		Class<?> bootClass = McpServerInitializerTest.class;
		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBeanNamesForAnnotation(org.springframework.boot.SpringBootConfiguration.class))
				.thenReturn(new String[] { "sampleApplication" });
		doReturn(bootClass).when(context).getType("sampleApplication");
		McpProperties properties = new McpProperties();
		// 第二个包即启动类所在包，与默认包取并集时应去重。
		properties.setBasePackages("com.example.a,com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server");
		McpServerRuntime runtime = runtime("default", properties, null);

		List<String> packages = McpServerInitializer.resolveBasePackages(properties, context,
				Collections.singletonList(runtime));

		assertThat(packages).containsExactly("com.example.a", "com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server");
	}

	@Test
	void shouldResolveNothingWhenNoPackageIsAvailable() {
		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBeanNamesForAnnotation(org.springframework.boot.SpringBootConfiguration.class))
				.thenReturn(new String[0]);
		McpServerRuntime runtime = runtime("default", new McpProperties(), null);

		List<String> packages = McpServerInitializer.resolveBasePackages(new McpProperties(), context,
				Collections.singletonList(runtime));

		assertThat(packages).isEmpty();
	}

	@Test
	void shouldIncludeRuntimeBasePackagesInScanSet() {
		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBeanNamesForAnnotation(org.springframework.boot.SpringBootConfiguration.class))
				.thenReturn(new String[0]);
		McpServerRuntime runtime = runtime("weather", new McpProperties(),
				Collections.singletonList("com.example.weather"));

		List<String> packages = McpServerInitializer.resolveBasePackages(new McpProperties(), context,
				Collections.singletonList(runtime));

		assertThat(packages).containsExactly("com.example.weather");
	}

	@Test
	void shouldStartEveryRuntimeWithoutBindingTransport() {
		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBeanNamesForAnnotation(org.springframework.boot.SpringBootConfiguration.class))
				.thenReturn(new String[0]);
		McpProperties properties = new McpProperties();
		properties.setBasePackages("com.example.a,com.example.b");

		McpServer serverA = mock(McpServer.class);
		McpServer serverB = mock(McpServer.class);
		BaseServerRegistry registry = mock(BaseServerRegistry.class);
		McpServerRuntime runtimeA = new McpServerRuntime("default", properties, serverA, mock(FeatureMgr.class),
				registry, null, Collections.emptyList());
		McpServerRuntime runtimeB = new McpServerRuntime("weather", properties, serverB, mock(FeatureMgr.class),
				registry, null, Collections.emptyList());
		List<McpServerRuntime> runtimes = java.util.Arrays.asList(runtimeA, runtimeB);

		new McpServerInitializer(runtimes, properties, context).run(mock(ApplicationArguments.class));

		verify(serverA, never()).setTransport(any());
		verify(serverB, never()).setTransport(any());
		verify(serverA).start();
		verify(serverB).start();
	}

	@Test
	void shouldBindTransportWhenRegistryIsSyncTransport() {
		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBeanNamesForAnnotation(org.springframework.boot.SpringBootConfiguration.class))
				.thenReturn(new String[0]);
		McpProperties properties = new McpProperties();
		properties.setBasePackages("com.example.a");

		McpServer server = mock(McpServer.class);
		FeatureMgr featureMgr = mock(FeatureMgr.class);
		BaseServerRegistry registry = new com.xiongx9527.fw.plus.ai.mcp.server.support.DefaultServerSseRegistry(
				server, "server-1");
		McpServerRuntime runtime = new McpServerRuntime("default", properties, server, featureMgr, registry,
				null, Collections.emptyList());
		List<McpServerRuntime> runtimes = Collections.singletonList(runtime);

		new McpServerInitializer(runtimes, properties, context).run(mock(ApplicationArguments.class));

		verify(server).setTransport(any(McpTransportSync.class));
		verify(server).start();
	}

	private McpServerRuntime runtime(String key, McpProperties properties, List<String> basePackages) {
		return new McpServerRuntime(key, properties, mock(McpServer.class), null, null, null,
				basePackages == null ? Collections.<String>emptyList() : basePackages);
	}
}