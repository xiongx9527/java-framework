package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpAccessTokenVerifier;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MagicAPIMcpConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MagicAPIMcpConfiguration.class);

	@Test
	void shouldCreateMcpPluginConfigurationByDefault() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(MagicAPIMcpConfiguration.class);
			MagicPluginConfiguration configuration = context.getBean(MagicAPIMcpConfiguration.class);
			assertThat(configuration.plugin().getName()).isEqualTo("mcp");
		});
	}

	@Test
	void shouldDisableMcpPluginWhenPropertyIsFalse() {
		contextRunner.withPropertyValues("magic-api.plugin.mcp.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(MagicAPIMcpConfiguration.class));
	}

	@Test
	void shouldUseDefaultEndpoint() {
		contextRunner.run(context -> {
			MagicAPIMcpProperties properties = context.getBean(MagicAPIMcpProperties.class);
			assertThat(properties.getEndpoint()).isEqualTo("/mcp");
		});
	}

	@Test
	void shouldRegisterToolsMethodsInDispatcher() {
		contextRunner.withPropertyValues("magic-api.plugin.mcp.auth.enabled=false")
				.run(context -> {
					Object response = context.getBean(com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcDispatcher.class)
							.dispatch(new com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext(), request("tools/list"));

					assertThat(response).extracting("error").isNull();
				});
	}

	@Test
	void shouldRegisterResourcesMethodsInDispatcher() {
		contextRunner.withPropertyValues("magic-api.plugin.mcp.auth.enabled=false")
				.run(context -> {
					Object response = context.getBean(com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcDispatcher.class)
							.dispatch(new com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext(), request("resources/list"));

					assertThat(response).extracting("error").isNull();
				});
	}

	@Test
	void shouldRegisterPromptsAndCompletionMethodsInDispatcher() {
		contextRunner.withPropertyValues("magic-api.plugin.mcp.auth.enabled=false")
				.run(context -> {
					com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcDispatcher dispatcher =
							context.getBean(com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcDispatcher.class);

					assertThat(dispatcher.dispatch(new com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext(),
							request("prompts/list"))).extracting("error").isNull();
					Object completionResponse = dispatcher.dispatch(
							new com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext(),
							request("completion/complete"));
					assertThat(completionResponse).extracting("error").extracting("code").isNotEqualTo(-32601);
				});
	}

	@Test
	void shouldRegisterLoggingAndClientRequestBeans() {
		contextRunner.withPropertyValues("magic-api.plugin.mcp.auth.enabled=false")
				.run(context -> {
					com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcDispatcher dispatcher =
							context.getBean(com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcDispatcher.class);

					assertThat(dispatcher.dispatch(new com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext(),
							request("logging/setLevel"))).extracting("error").isNull();
					assertThat(context).hasSingleBean(com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.client.McpClientRequestService.class);
				});
	}

	@Test
	void shouldExposeControllerRegister() {
		contextRunner.run(context -> {
			MagicControllerRegister register = context.getBean(MagicAPIMcpConfiguration.class).controllerRegister();
			assertThat(register).isNotNull();
		});
	}

	@Test
	void shouldAllowCustomAccessTokenVerifier() {
		contextRunner.withBean(McpAccessTokenVerifier.class,
						() -> token -> new McpPrincipal("custom-user", java.util.Collections.singleton("mcp:tools")))
				.run(context -> {
					assertThat(context).hasSingleBean(McpAccessTokenVerifier.class);
					McpPrincipal principal = context.getBean(McpAccessTokenVerifier.class).verify("token-1");
					assertThat(principal.getSubject()).isEqualTo("custom-user");
				});
	}

	private Map<String, Object> request(String method) {
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", 1);
		request.put("method", method);
		return request;
	}
}
