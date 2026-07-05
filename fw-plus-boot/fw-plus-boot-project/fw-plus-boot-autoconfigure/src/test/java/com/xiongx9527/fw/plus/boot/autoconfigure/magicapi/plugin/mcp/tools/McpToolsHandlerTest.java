package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.tools;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpAuthorizationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpPrincipal;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.SaTokenMcpAuthService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.*;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpToolsHandlerTest {

	@Test
	void shouldListToolsFromProviders() {
		McpToolsHandler handler = new McpToolsHandler(Collections.singletonList(new FixedToolProvider()));

		Map<String, Object> result = handler.listTools(new McpRequestContext(), Collections.emptyMap());

		assertThat(result.get("tools")).asList().hasSize(1);
	}

	@Test
	void shouldCallKnownTool() {
		McpToolsHandler handler = new McpToolsHandler(Collections.singletonList(new FixedToolProvider()));
		Map<String, Object> params = new HashMap<>();
		params.put("name", "echo");
		params.put("arguments", Collections.singletonMap("text", "hello"));

		Map<String, Object> result = handler.callTool(new McpRequestContext(), params);

		assertThat(result.get("isError")).isEqualTo(false);
		assertThat(result.get("structuredContent")).isEqualTo(Collections.singletonMap("text", "hello"));
	}

	@Test
	void shouldRejectUnknownTool() {
		McpToolsHandler handler = new McpToolsHandler(Collections.singletonList(new FixedToolProvider()));
		Map<String, Object> params = new HashMap<>();
		params.put("name", "missing");

		assertThatThrownBy(() -> handler.callTool(new McpRequestContext(), params))
				.isInstanceOf(McpJsonRpcException.class)
				.hasMessageContaining("Tool not found");
	}

	@Test
	void shouldRequireToolsScope() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		SaTokenMcpAuthService authService = new SaTokenMcpAuthService(properties,
				token -> new McpPrincipal("user-1", Collections.emptySet()));
		McpToolsHandler handler = new McpToolsHandler(Collections.singletonList(new FixedToolProvider()), authService, properties);
		McpRequestContext context = new McpRequestContext(new McpPrincipal("user-1", Collections.emptySet()));

		assertThatThrownBy(() -> handler.listTools(context, Collections.emptyMap()))
				.isInstanceOf(McpAuthorizationException.class)
				.hasMessageContaining("Insufficient scope");
	}

	@Test
	void shouldBuildToolsListChangedNotification() {
		Map<String, Object> notification = McpToolsHandler.listChanged();

		assertThat(notification.get("method")).isEqualTo("notifications/tools/list_changed");
	}

	@Test
	void shouldRejectOversizedToolOutput() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		properties.setMaxResponseChars(10);
		McpToolsHandler handler = new McpToolsHandler(Collections.singletonList(new LargeToolProvider()), null, properties);
		Map<String, Object> params = new HashMap<>();
		params.put("name", "large");

		assertThatThrownBy(() -> handler.callTool(new McpRequestContext(), params))
				.isInstanceOf(McpJsonRpcException.class)
				.hasMessageContaining("Tool output too large");
	}

	private static class FixedToolProvider implements McpToolProvider {

		@Override
		public McpPage<McpToolDefinition> listTools(McpProviderContext context, McpPageRequest pageRequest) {
			McpToolDefinition definition = new McpToolDefinition();
			definition.setName("echo");
			definition.setDescription("Echo text");
			definition.setInputSchema(Collections.singletonMap("type", "object"));
			return McpPage.of(Collections.singletonList(definition), null);
		}

		@Override
		public McpToolResult callTool(McpProviderContext context, McpToolCallRequest request) {
			if (!"echo".equals(request.getName())) {
				return null;
			}
			McpToolResult result = new McpToolResult();
			result.setError(false);
			result.setStructuredContent(request.getArguments());
			result.setContent(Collections.singletonList(Collections.singletonMap("type", "text")));
			return result;
		}
	}

	private static class LargeToolProvider implements McpToolProvider {

		@Override
		public McpPage<McpToolDefinition> listTools(McpProviderContext context, McpPageRequest pageRequest) {
			return McpPage.of(Collections.emptyList(), null);
		}

		@Override
		public McpToolResult callTool(McpProviderContext context, McpToolCallRequest request) {
			McpToolResult result = new McpToolResult();
			result.setStructuredContent(Collections.singletonMap("text", "01234567890123456789"));
			return result;
		}
	}
}
