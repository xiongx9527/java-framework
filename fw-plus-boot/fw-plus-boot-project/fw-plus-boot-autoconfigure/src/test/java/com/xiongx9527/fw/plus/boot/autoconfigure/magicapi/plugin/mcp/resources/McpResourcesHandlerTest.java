package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.resources;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpResourcesHandlerTest {

	@Test
	void shouldListResourcesFromProviders() {
		McpResourcesHandler handler = new McpResourcesHandler(Collections.singletonList(new FixedResourceProvider()),
				new McpResourceSubscriptionService());

		Map<String, Object> result = handler.listResources(new McpRequestContext(), Collections.emptyMap());

		assertThat(result.get("resources")).asList().hasSize(1);
	}

	@Test
	void shouldReadTextResource() {
		McpResourcesHandler handler = new McpResourcesHandler(Collections.singletonList(new FixedResourceProvider()),
				new McpResourceSubscriptionService());

		Map<String, Object> result = handler.readResource(new McpRequestContext(), Collections.singletonMap("uri", "mcp://demo/readme"));

		assertThat(result.get("contents")).asList().hasSize(1);
	}

	@Test
	void shouldRejectInvalidResourceUri() {
		McpResourcesHandler handler = new McpResourcesHandler(Collections.singletonList(new FixedResourceProvider()),
				new McpResourceSubscriptionService());

		assertThatThrownBy(() -> handler.readResource(new McpRequestContext(), Collections.singletonMap("uri", "file:///etc/passwd")))
				.isInstanceOf(McpJsonRpcException.class)
				.hasMessageContaining("Invalid resource uri");
	}

	@Test
	void shouldListResourceTemplates() {
		McpResourcesHandler handler = new McpResourcesHandler(Collections.singletonList(new FixedResourceProvider()),
				new McpResourceSubscriptionService());

		Map<String, Object> result = handler.listTemplates(new McpRequestContext(), Collections.emptyMap());

		assertThat(result.get("resourceTemplates")).asList().hasSize(1);
	}

	@Test
	void shouldTrackResourceSubscriptionsAndUpdates() {
		McpResourceSubscriptionService subscriptionService = new McpResourceSubscriptionService();
		McpResourcesHandler handler = new McpResourcesHandler(Collections.singletonList(new FixedResourceProvider()), subscriptionService);
		McpRequestContext context = new McpRequestContext();
		context.setSessionId("session-1");
		Map<String, Object> params = Collections.singletonMap("uri", "mcp://demo/readme");

		handler.subscribe(context, params);
		List<Map<String, Object>> notifications = subscriptionService.updated("mcp://demo/readme");

		assertThat(notifications).hasSize(1);
		assertThat(notifications.get(0).get("method")).isEqualTo("notifications/resources/updated");
		handler.unsubscribe(context, params);
		assertThat(subscriptionService.updated("mcp://demo/readme")).isEmpty();
	}

	@Test
	void shouldBuildListChangedNotification() {
		McpResourceSubscriptionService subscriptionService = new McpResourceSubscriptionService();

		Map<String, Object> notification = subscriptionService.listChanged();

		assertThat(notification.get("method")).isEqualTo("notifications/resources/list_changed");
	}

	@Test
	void shouldRejectOversizedResourceOutput() {
		com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties properties =
				new com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties();
		properties.setMaxResponseChars(10);
		McpResourcesHandler handler = new McpResourcesHandler(Collections.singletonList(new FixedResourceProvider()),
				new McpResourceSubscriptionService(), null, properties);

		assertThatThrownBy(() -> handler.readResource(new McpRequestContext(), Collections.singletonMap("uri", "mcp://demo/readme")))
				.isInstanceOf(McpJsonRpcException.class)
				.hasMessageContaining("Resource output too large");
	}

	private static class FixedResourceProvider implements McpResourceProvider {

		@Override
		public McpPage<McpResourceDefinition> listResources(McpProviderContext context, McpPageRequest pageRequest) {
			McpResourceDefinition definition = new McpResourceDefinition();
			definition.setUri("mcp://demo/readme");
			definition.setName("readme");
			definition.setMimeType("text/plain");
			return McpPage.of(Collections.singletonList(definition), null);
		}

		@Override
		public McpResourceReadResult readResource(McpProviderContext context, McpResourceReadRequest request) {
			if (!"mcp://demo/readme".equals(request.getUri())) {
				return null;
			}
			McpResourceReadResult result = new McpResourceReadResult();
			result.setContents(Collections.singletonList(Collections.singletonMap("text", "hello")));
			return result;
		}

		@Override
		public McpPage<McpResourceTemplate> listTemplates(McpProviderContext context, McpPageRequest pageRequest) {
			McpResourceTemplate template = new McpResourceTemplate();
			template.setUriTemplate("mcp://demo/{name}");
			template.setName("demo");
			return McpPage.of(Collections.singletonList(template), null);
		}
	}
}
