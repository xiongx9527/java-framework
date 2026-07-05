package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model.McpPromptInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model.McpResourceInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpMagicResourceStorageTest {

	@Test
	void shouldUseMcpResourceFolder() {
		assertThat(new McpResourceMagicResourceStorage().folder()).isEqualTo("mcp-resource");
	}

	@Test
	void shouldUseMcpPromptFolder() {
		assertThat(new McpPromptMagicResourceStorage().folder()).isEqualTo("mcp-prompt");
	}

	@Test
	void shouldNotUseDatasourceFolder() {
		assertThat(new McpResourceMagicResourceStorage().folder()).doesNotContain("datasource");
		assertThat(new McpPromptMagicResourceStorage().folder()).doesNotContain("datasource");
	}

	@Test
	void shouldRejectInvalidResourceUri() {
		McpResourceInfo info = new McpResourceInfo();
		info.setName("Readme");
		info.setUri("file:///etc/passwd");

		assertThatThrownBy(() -> new McpResourceMagicResourceStorage().validate(info))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("uri");
	}

	@Test
	void shouldRejectInvalidMimeType() {
		McpResourceInfo info = new McpResourceInfo();
		info.setName("Readme");
		info.setUri("mcp://docs/readme");
		info.setMimeType("text");

		assertThatThrownBy(() -> new McpResourceMagicResourceStorage().validate(info))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("mimeType");
	}

	@Test
	void shouldRejectPromptWithoutName() {
		McpPromptInfo info = new McpPromptInfo();
		info.setDescription("Demo prompt");

		assertThatThrownBy(() -> new McpPromptMagicResourceStorage().validate(info))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("name");
	}

	@Test
	void shouldRejectPromptArgumentWithoutName() {
		McpPromptInfo info = new McpPromptInfo();
		info.setName("summarize");
		info.setArgumentsJson("[{\"description\":\"Text\"}]");

		assertThatThrownBy(() -> new McpPromptMagicResourceStorage().validate(info))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("argumentsJson")
				.hasMessageContaining("name");
	}

	@Test
	void shouldRejectPromptMessageWithoutContent() {
		McpPromptInfo info = new McpPromptInfo();
		info.setName("summarize");
		info.setMessagesJson("[{\"role\":\"user\"}]");

		assertThatThrownBy(() -> new McpPromptMagicResourceStorage().validate(info))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("messagesJson")
				.hasMessageContaining("content");
	}
}
