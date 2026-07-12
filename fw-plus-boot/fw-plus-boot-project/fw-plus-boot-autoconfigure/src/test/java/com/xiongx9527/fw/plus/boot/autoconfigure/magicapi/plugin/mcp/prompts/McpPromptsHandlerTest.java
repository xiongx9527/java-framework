package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.prompts;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.*;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpPromptsHandlerTest {

	@Test
	void shouldListPromptsFromProviders() {
		McpPromptsHandler handler = new McpPromptsHandler(Collections.singletonList(new FixedPromptProvider()));

		Map<String, Object> result = handler.listPrompts(new McpRequestContext(), Collections.emptyMap());

		assertThat(result.get("prompts")).asList().hasSize(1);
	}

	@Test
	void shouldRenderPromptWithArguments() {
		McpPromptsHandler handler = new McpPromptsHandler(Collections.singletonList(new FixedPromptProvider()));
		Map<String, Object> params = new HashMap<>();
		params.put("name", "greet");
		params.put("arguments", Collections.singletonMap("name", "9527"));

		Map<String, Object> result = handler.getPrompt(new McpRequestContext(), params);

		assertThat(result.get("messages")).asList().hasSize(1);
	}

	@Test
	void shouldRejectMissingRequiredPromptArgument() {
		McpPromptsHandler handler = new McpPromptsHandler(Collections.singletonList(new FixedPromptProvider()));

		assertThatThrownBy(() -> handler.getPrompt(new McpRequestContext(), Collections.singletonMap("name", "greet")))
				.isInstanceOf(McpJsonRpcException.class)
				.hasMessageContaining("Missing prompt argument");
	}

	@Test
	void shouldBuildPromptsListChangedNotification() {
		Map<String, Object> notification = McpPromptsHandler.listChanged();

		assertThat(notification.get("method")).isEqualTo("notifications/prompts/list_changed");
	}

	private static class FixedPromptProvider implements McpPromptProvider {

		@Override
		public McpPage<McpPromptDefinition> listPrompts(McpProviderContext context, McpPageRequest pageRequest) {
			McpPromptDefinition definition = new McpPromptDefinition();
			definition.setName("greet");
			definition.setDescription("Greeting prompt");
			McpPromptArgument argument = new McpPromptArgument();
			argument.setName("name");
			argument.setRequired(true);
			definition.setArguments(Collections.singletonList(argument));
			return McpPage.of(Collections.singletonList(definition), null);
		}

		@Override
		public McpPromptResult getPrompt(McpProviderContext context, McpPromptGetRequest request) {
			if (!"greet".equals(request.getName())) {
				return null;
			}
			McpPromptResult result = new McpPromptResult();
			result.setMessages(Collections.singletonList(Collections.singletonMap("role", "user")));
			return result;
		}
	}
}
