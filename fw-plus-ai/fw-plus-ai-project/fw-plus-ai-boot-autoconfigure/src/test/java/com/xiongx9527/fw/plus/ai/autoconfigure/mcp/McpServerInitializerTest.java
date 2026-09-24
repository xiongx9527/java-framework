package com.xiongx9527.fw.plus.ai.autoconfigure.mcp;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.ajaxjs.mcp.transport.McpTransportSync;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.ApplicationArguments;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationContext;

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

		List<String> packages = McpServerInitializer.resolveBasePackages(new McpProperties(), context);

		assertThat(packages).containsExactly("com.xiongx9527.fw.plus.ai.autoconfigure.mcp");
	}

	@Test
	void shouldResolveMultiplePackagesFromConfiguration() {
		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBeanNamesForAnnotation(org.springframework.boot.SpringBootConfiguration.class))
				.thenReturn(new String[0]);
		McpProperties properties = new McpProperties();
		properties.setBasePackages(" com.example.a , , com.example.b ");

		List<String> packages = McpServerInitializer.resolveBasePackages(properties, context);

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
		properties.setBasePackages("com.example.a,com.xiongx9527.fw.plus.ai.autoconfigure.mcp");

		List<String> packages = McpServerInitializer.resolveBasePackages(properties, context);

		assertThat(packages).containsExactly("com.example.a", "com.xiongx9527.fw.plus.ai.autoconfigure.mcp");
	}

	@Test
	void shouldResolveNothingWhenNoPackageIsAvailable() {
		ApplicationContext context = mock(ApplicationContext.class);
		when(context.getBeanNamesForAnnotation(org.springframework.boot.SpringBootConfiguration.class))
				.thenReturn(new String[0]);

		List<String> packages = McpServerInitializer.resolveBasePackages(new McpProperties(), context);

		assertThat(packages).isEmpty();
	}

	@Test
	void shouldScanEveryPackageThenStartServer() {
		ApplicationContext context = mock(ApplicationContext.class);
		ObjectProvider<McpTransportSync> transportProvider = emptyTransportProvider();
		when(context.getBeanProvider(McpTransportSync.class)).thenReturn(transportProvider);
		when(context.getBeanNamesForAnnotation(org.springframework.boot.SpringBootConfiguration.class))
				.thenReturn(new String[0]);
		McpProperties properties = new McpProperties();
		properties.setBasePackages("com.example.a,com.example.b");
		McpServer server = mock(McpServer.class);
		FeatureMgr featureMgr = mock(FeatureMgr.class);

		new McpServerInitializer(server, properties, context, featureMgr).run(mock(ApplicationArguments.class));

		verify(featureMgr).init("com.example.a");
		verify(featureMgr).init("com.example.b");
		verify(server).start();
		verify(server, never()).setTransport(any());
	}

	@SuppressWarnings("unchecked")
	private ObjectProvider<McpTransportSync> emptyTransportProvider() {
		ObjectProvider<McpTransportSync> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(null);
		return provider;
	}
}
