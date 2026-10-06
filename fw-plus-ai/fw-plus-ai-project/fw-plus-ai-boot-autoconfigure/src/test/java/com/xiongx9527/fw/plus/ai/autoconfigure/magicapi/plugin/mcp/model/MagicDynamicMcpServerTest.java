package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.model;

import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.resource.service.MagicApiMcpCapabilityRegistrar;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolInvoker;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.auth.core.PermitAllAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpRuntimeFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicDynamicMcpServerTest {

	@Test
	void shouldTrackConsoleCreatedInstanceInServerInfos() {
		MagicDynamicMcpServer server = server(runtime());

		MagicMcpServerInfo info = new MagicMcpServerInfo();
		info.setKey("console");
		info.setName("控制台实例");
		info.setEndpoint("/mcp/console");
		assertThat(server.createClient(info)).isTrue();

		assertThat(server.serverInfos()).extracting(MagicMcpServerInfo::getKey).contains("console");
		assertThat(server.getServer("console")).isNotNull();
	}

	@Test
	void shouldDropInstanceFromServerInfosAfterDestroy() {
		MagicDynamicMcpServer server = server(runtime());
		MagicMcpServerInfo info = new MagicMcpServerInfo();
		info.setKey("console");
		info.setEndpoint("/mcp/console");

		assertThat(server.createClient(info)).isTrue();
		server.destroyClientByKey("console");

		assertThat(server.serverInfos()).extracting(MagicMcpServerInfo::getKey).doesNotContain("console");
		assertThat(server.getServer("console")).isNull();
	}

	@Test
	void shouldStillExposeConfiguredServersWhenNoConsoleInstanceExists() {
		MagicApiMcpProperties properties = new MagicApiMcpProperties();
		MagicApiMcpProperties.Server configured = new MagicApiMcpProperties.Server();
		configured.setKey("cfg");
		configured.setEndpoint("/mcp/cfg");
		properties.getServers().add(configured);

		MagicDynamicMcpServer server = new MagicDynamicMcpServer(properties,
				mock(MagicResourceService.class), mock(McpRuntimeFactory.class), null, null, null,
				mock(MagicApiMcpToolInvoker.class), null, nullProvider());

		assertThat(server.serverInfos()).extracting(MagicMcpServerInfo::getKey).contains("cfg");
	}

	@Test
	void shouldPassProviderAuthSelectionToRuntimeCreation() {
		McpRuntimeFactory factory = mock(McpRuntimeFactory.class);
		InstanceAuthRegistry authRegistry = new InstanceAuthRegistry();
		McpServerRuntime runtime = runtime();
		when(factory.createRuntime(any(McpProperties.class), any(McpProperties.class), same(authRegistry),
				any(), any())).thenReturn(runtime);
		McpProperties mcpProperties = new McpProperties();
		mcpProperties.setScopeCheckEnabled(true);
		MagicDynamicMcpServer server = new MagicDynamicMcpServer(new MagicApiMcpProperties(),
				mock(MagicResourceService.class), factory, authRegistry, mcpProperties, null,
				mock(MagicApiMcpToolInvoker.class), null, nullProvider());
		MagicMcpServerInfo info = new MagicMcpServerInfo();
		info.setKey("console");
		info.setEndpoint("/mcp/console");
		MagicApiMcpProperties.Auth auth = new MagicApiMcpProperties.Auth();
		auth.setProvider("permit-all");
		info.setAuth(auth);

		assertThat(server.createClient(info)).isTrue();

		ArgumentCaptor<McpProperties> propertiesCaptor = ArgumentCaptor.forClass(McpProperties.class);
		verify(factory).createRuntime(any(McpProperties.class), propertiesCaptor.capture(), same(authRegistry),
				any(), any());
		assertThat(propertiesCaptor.getValue().getKey()).isEqualTo("console");
		assertThat(propertiesCaptor.getValue().getAuth().getProvider()).isEqualTo("permit-all");
		assertThat(authRegistry.resolve(propertiesCaptor.getValue().getKey(),
				toCoreAuth(propertiesCaptor.getValue().getAuth()))).isInstanceOf(PermitAllAuthService.class);
		assertThat(propertiesCaptor.getValue().isScopeCheckEnabled()).isTrue();
	}

	// 适配器：把 McpProperties.Auth 转为核心 InstanceAuthProperties
	private com.xiongx9527.fw.plus.auth.core.config.InstanceAuthProperties toCoreAuth(com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties.Auth auth) {
		if (auth == null) return null;
		com.xiongx9527.fw.plus.auth.core.config.InstanceAuthProperties core = new com.xiongx9527.fw.plus.auth.core.config.InstanceAuthProperties();
		core.setProvider(auth.getProvider());
		return core;
	}

	private MagicDynamicMcpServer server(McpServerRuntime runtime) {
		McpRuntimeFactory factory = mock(McpRuntimeFactory.class);
		when(factory.createRuntime(any(McpProperties.class), any(McpProperties.class), any(), any(), any()))
				.thenReturn(runtime);
		return new MagicDynamicMcpServer(new MagicApiMcpProperties(), mock(MagicResourceService.class), factory,
				null, null, null, mock(MagicApiMcpToolInvoker.class), null, nullProvider());
	}

	@SuppressWarnings("unchecked")
	private ObjectProvider<MagicApiMcpCapabilityRegistrar> nullProvider() {
		ObjectProvider<MagicApiMcpCapabilityRegistrar> provider = mock(ObjectProvider.class);
		when(provider.getIfAvailable()).thenReturn(null);
		return provider;
	}

	private McpServerRuntime runtime() {
		McpServerRuntime runtime = mock(McpServerRuntime.class);
		when(runtime.getEndpoint()).thenReturn("/mcp/console");
		when(runtime.getFeatureMgr()).thenReturn(new FeatureMgr());
		when(runtime.getServer()).thenReturn(mock(com.ajaxjs.mcp.server.McpServer.class));
		return runtime;
	}
}