package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.tools;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.*;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.*;
import java.util.function.BiFunction;

import static org.assertj.core.api.Assertions.assertThat;

class MagicApiMcpToolProviderTest {

	@Test
	void shouldExposeMagicApiFunctionsAsTools() {
		MagicApiMcpToolProvider provider = new MagicApiMcpToolProvider(new FixedResourceService(), new MagicAPIMcpProperties(),
				(name, arguments) -> Collections.singletonMap("name", name));

		McpPage<McpToolDefinition> page = provider.listTools(new McpProviderContext(new McpRequestContext()), new McpPageRequest());

		assertThat(page.getItems()).extracting(McpToolDefinition::getName).contains("echo");
	}

	@Test
	void shouldCallMagicApiFunction() {
		MagicApiMcpToolProvider provider = new MagicApiMcpToolProvider(new FixedResourceService(), new MagicAPIMcpProperties(),
				(name, arguments) -> Collections.singletonMap("text", arguments.get("text")));
		McpToolCallRequest request = new McpToolCallRequest();
		request.setName("echo");
		request.setArguments(Collections.singletonMap("text", "hello"));

		McpToolResult result = provider.callTool(new McpProviderContext(new McpRequestContext()), request);

		assertThat(result.isError()).isFalse();
		assertThat(result.getStructuredContent()).isEqualTo(Collections.singletonMap("text", "hello"));
	}

	@Test
	void shouldNotExposeMagicApiApisByDefault() {
		MagicApiMcpToolProvider provider = new MagicApiMcpToolProvider(new FixedResourceService(), new MagicAPIMcpProperties(),
				(name, arguments) -> Collections.singletonMap("name", name));

		McpPage<McpToolDefinition> page = provider.listTools(new McpProviderContext(new McpRequestContext()), new McpPageRequest());

		assertThat(page.getItems()).extracting(McpToolDefinition::getName).doesNotContain("GET /orders/list");
	}

	@Test
	void shouldExposeApiOnlyWhenScopeMatches() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		properties.getTools().getApi().setEnabled(true);
		properties.getTools().getApi().getIncludeGroups().add("/admin");
		properties.getTools().getApi().getIncludePaths().add("/orders/**");
		properties.getTools().getApi().getMethods().add("GET");
		properties.getTools().getApi().setRequireOptIn(true);
		MagicApiMcpToolProvider provider = new MagicApiMcpToolProvider(new FixedResourceService(), properties,
				(name, arguments) -> Collections.singletonMap("tool", name));

		McpPage<McpToolDefinition> page = provider.listTools(new McpProviderContext(new McpRequestContext()), new McpPageRequest());

		assertThat(page.getItems()).extracting(McpToolDefinition::getName).contains("GET /orders/list");
	}

	@Test
	void shouldExcludeApiBeforeIncludeScope() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		properties.getTools().getApi().setEnabled(true);
		properties.getTools().getApi().getIncludePaths().add("/orders/**");
		properties.getTools().getApi().getExcludePaths().add("/orders/list");
		MagicApiMcpToolProvider provider = new MagicApiMcpToolProvider(new FixedResourceService(), properties,
				(name, arguments) -> Collections.singletonMap("tool", name));

		McpPage<McpToolDefinition> page = provider.listTools(new McpProviderContext(new McpRequestContext()), new McpPageRequest());

		assertThat(page.getItems()).extracting(McpToolDefinition::getName).doesNotContain("GET /orders/list");
	}

	@Test
	void shouldRejectApiCallWhenApiNotExposed() {
		MagicApiMcpToolProvider provider = new MagicApiMcpToolProvider(new FixedResourceService(), new MagicAPIMcpProperties(),
				(name, arguments) -> Collections.singletonMap("tool", name));
		McpToolCallRequest request = new McpToolCallRequest();
		request.setName("GET /orders/list");

		McpToolResult result = provider.callTool(new McpProviderContext(new McpRequestContext()), request);

		assertThat(result).isNull();
	}

	@Test
	void shouldCallScopedApiTool() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		properties.getTools().getApi().setEnabled(true);
		properties.getTools().getApi().getIncludePaths().add("/orders/**");
		MagicApiMcpToolProvider provider = new MagicApiMcpToolProvider(new FixedResourceService(), properties,
				(name, arguments) -> Collections.singletonMap("function", name),
				(info, arguments) -> Collections.singletonMap("api", info.getPath()));
		McpToolCallRequest request = new McpToolCallRequest();
		request.setName("GET /orders/list");

		McpToolResult result = provider.callTool(new McpProviderContext(new McpRequestContext()), request);

		assertThat(result).isNotNull();
		assertThat(result.isError()).isFalse();
		assertThat(result.getStructuredContent()).isEqualTo(Collections.singletonMap("api", "/orders/list"));
	}

	@Test
	void shouldMatchApiScopeWithGroupPathMethodAndOptIn() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		properties.getTools().getApi().setEnabled(true);
		properties.getTools().getApi().getIncludeGroups().add("/admin");
		properties.getTools().getApi().getIncludePaths().add("/orders/**");
		properties.getTools().getApi().getMethods().add("GET");
		properties.getTools().getApi().setRequireOptIn(true);

		McpToolScopeMatcher matcher = new McpToolScopeMatcher(properties, new FixedResourceService());

		assertThat(matcher.matches(FixedResourceService.api())).isTrue();
	}

	private static class FixedResourceService implements MagicResourceService {

		@Override
		public <T extends MagicEntity> List<T> files(String type) {
			if ("function".equals(type)) {
				MagicEntity entity = new MagicEntity();
				entity.setId("function-1");
				entity.setName("echo");
				return (List<T>) Collections.singletonList(entity);
			}
			if ("api".equals(type)) {
				return (List<T>) Collections.singletonList(api());
			}
			return Collections.emptyList();
		}

		private static ApiInfo api() {
			ApiInfo info = new ApiInfo();
			info.setId("api-1");
			info.setName("订单列表");
			info.setGroupId("group-admin");
			info.setPath("/orders/list");
			info.setMethod("GET");
			info.setDescription("List orders");
			info.setAttribute("mcpTool", true);
			return info;
		}

		@Override public void refresh() {}
		@Override public org.ssssssss.magicapi.core.resource.Resource getResource() { return null; }
		@Override public boolean processNotify(org.ssssssss.magicapi.core.model.MagicNotify magicNotify) { return false; }
		@Override public boolean saveGroup(org.ssssssss.magicapi.core.model.Group group) { return false; }
		@Override public boolean move(String s, String s1) { return false; }
		@Override public String copyGroup(String s, String s1) { return null; }
		@Override public org.ssssssss.magicapi.core.model.TreeNode<org.ssssssss.magicapi.core.model.Group> tree(String s) { return null; }
		@Override public Map<String, org.ssssssss.magicapi.core.model.TreeNode<org.ssssssss.magicapi.core.model.Group>> tree() { return null; }
		@Override public List<org.ssssssss.magicapi.core.model.Group> getGroupsByFileId(String s) { return Collections.emptyList(); }
		@Override public org.ssssssss.magicapi.core.resource.Resource getGroupResource(String s) { return null; }
		@Override public <T extends MagicEntity> boolean saveFile(T t) { return false; }
		@Override public boolean delete(String s) { return false; }
		@Override public <T extends MagicEntity> List<T> listFiles(String s) { return files(s); }
		@Override public <T extends MagicEntity> T file(String s) { return null; }
		@Override public org.ssssssss.magicapi.core.model.Group getGroup(String s) { return null; }
		@Override public void export(String s, List<org.ssssssss.magicapi.core.model.SelectedResource> list, java.io.OutputStream outputStream) {}
		@Override public boolean lock(String s) { return false; }
		@Override public boolean unlock(String s) { return false; }
		@Override public boolean upload(java.io.InputStream inputStream, boolean b) { return false; }
		@Override public String getGroupPath(String s) { return "group-admin".equals(s) ? "/admin" : null; }
		@Override public String getGroupName(String s) { return null; }
	}
}
