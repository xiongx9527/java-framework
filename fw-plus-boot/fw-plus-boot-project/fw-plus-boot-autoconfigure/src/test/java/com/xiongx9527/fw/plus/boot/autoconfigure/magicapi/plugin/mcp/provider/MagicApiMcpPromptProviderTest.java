package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model.McpPromptInfo;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MagicApiMcpPromptProviderTest {

	@Test
	void shouldListAndGetConsoleManagedPrompt() {
		McpPromptInfo info = new McpPromptInfo();
		info.setName("summarize");
		info.setDescription("Summarize content");
		info.setArgumentsJson("[{\"name\":\"content\",\"description\":\"Text\",\"required\":true}]");
		info.setMessagesJson("[{\"role\":\"user\",\"content\":{\"type\":\"text\",\"text\":\"{{content}}\"}}]");
		MagicApiMcpPromptProvider provider = new MagicApiMcpPromptProvider(() -> Collections.singletonList(info));
		McpPromptGetRequest request = new McpPromptGetRequest();
		request.setName("summarize");

		McpPage<McpPromptDefinition> page = provider.listPrompts(new McpProviderContext(), new McpPageRequest());
		McpPromptResult result = provider.getPrompt(new McpProviderContext(), request);

		assertThat(page.getItems()).hasSize(1);
		assertThat(page.getItems().get(0).getName()).isEqualTo("summarize");
		assertThat(page.getItems().get(0).getArguments()).hasSize(1);
		assertThat(page.getItems().get(0).getArguments().get(0).getName()).isEqualTo("content");
		assertThat(result.getDescription()).isEqualTo("Summarize content");
		assertThat(result.getMessages()).hasSize(1);
	}

	@Test
	@SuppressWarnings("unchecked")
	void shouldRenderNestedStructuredMessageContent() {
		McpPromptInfo info = new McpPromptInfo();
		info.setName("summarize");
		info.setDescription("Summarize content");
		info.setArgumentsJson("[{\"name\":\"content\",\"description\":\"Text\",\"required\":true}]");
		info.setMessagesJson("[{\"role\":\"user\",\"content\":{\"type\":\"text\",\"text\":\"Summarize: {{content}}\"}}]");
		MagicApiMcpPromptProvider provider = new MagicApiMcpPromptProvider(() -> Collections.singletonList(info));
		McpPromptGetRequest request = new McpPromptGetRequest();
		request.setName("summarize");
		request.setArguments(Collections.singletonMap("content", "hello"));

		McpPromptResult result = provider.getPrompt(new McpProviderContext(), request);

		Map<String, Object> message = result.getMessages().get(0);
		Map<String, Object> content = (Map<String, Object>) message.get("content");
		assertThat(content.get("type")).isEqualTo("text");
		assertThat(content.get("text")).isEqualTo("Summarize: hello");
	}
}
