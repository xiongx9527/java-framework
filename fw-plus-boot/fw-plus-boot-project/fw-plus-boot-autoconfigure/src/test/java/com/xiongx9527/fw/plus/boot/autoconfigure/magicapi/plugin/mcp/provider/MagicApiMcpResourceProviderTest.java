package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model.McpResourceInfo;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MagicApiMcpResourceProviderTest {

	@Test
	void shouldListAndReadConsoleManagedResource() {
		McpResourceInfo info = new McpResourceInfo();
		info.setName("Readme");
		info.setUri("mcp://docs/readme");
		info.setMimeType("text/plain");
		info.setDescription("Project readme");
		info.setScript("hello");
		MagicApiMcpResourceProvider provider = new MagicApiMcpResourceProvider(() -> Collections.singletonList(info));

		McpPage<McpResourceDefinition> page = provider.listResources(new McpProviderContext(), new McpPageRequest());
		McpResourceReadRequest request = new McpResourceReadRequest();
		request.setUri("mcp://docs/readme");
		McpResourceReadResult result = provider.readResource(new McpProviderContext(), request);

		assertThat(page.getItems()).hasSize(1);
		assertThat(page.getItems().get(0).getUri()).isEqualTo("mcp://docs/readme");
		assertThat(page.getItems().get(0).getName()).isEqualTo("Readme");
		assertThat(result.getContents()).hasSize(1);
		Map<String, Object> content = result.getContents().get(0);
		assertThat(content).containsEntry("uri", "mcp://docs/readme");
		assertThat(content).containsEntry("mimeType", "text/plain");
		assertThat(content).containsEntry("text", "hello");
	}
}
