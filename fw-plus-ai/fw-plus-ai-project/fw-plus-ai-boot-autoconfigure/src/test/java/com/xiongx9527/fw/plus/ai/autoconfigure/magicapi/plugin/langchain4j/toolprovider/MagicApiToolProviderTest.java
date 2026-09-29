package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider;

import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MagicApiToolProviderTest {

	private MagicApiToolRegistry registry;

	private MagicToolInvoker invoker;

	private MagicApiToolProvider provider;

	@BeforeEach
	void setUp() {
		registry = mock(MagicApiToolRegistry.class);
		invoker = mock(MagicToolInvoker.class);
		provider = new MagicApiToolProvider(registry, new MagicApiToolExecutor(registry, invoker));
	}

	@Test
	void shouldProvideSpecificationsForFunctionsAndApis() {
		when(registry.snapshot()).thenReturn(Arrays.asList(functionTool(), apiTool()));

		ToolProviderResult result = provideOnce();

		assertThat(specNames(result)).containsExactlyInAnyOrder("getEnv", "POST /评估/查询");
		ToolSpecification functionSpec = specNamed(result, "getEnv");
		assertThat(functionSpec.description()).isEqualTo("获取环境");

		ToolSpecification apiSpec = specNamed(result, "POST /评估/查询");
		assertThat(apiSpec.description()).isEqualTo("查询");
		assertThat(apiSpec.parameters().required()).containsExactly("name");
		// 所有工具共享同一个 executor 实例。
		assertThat(result.tools().values()).allMatch(executor -> executor == result.tools().get(functionSpec));
	}

	@Test
	void shouldProvideNoToolsWhenRegistryEmpty() {
		when(registry.snapshot()).thenReturn(Collections.emptyList());

		assertThat(provideOnce().tools()).isEmpty();
	}

	@Test
	void shouldKeepFirstSpecificationWhenNamesCollide() {
		MagicApiTool function = functionTool();
		when(registry.snapshot()).thenReturn(Arrays.asList(function, toolWithName(apiTool(), "getEnv")));

		ToolProviderResult result = provideOnce();

		assertThat(specNames(result)).containsExactly("getEnv");
		assertThat(specNamed(result, "getEnv").description()).isEqualTo("获取环境");
	}

	private ToolProviderResult provideOnce() {
		return provider.provideTools(new ToolProviderRequest(null, null));
	}

	private static List<String> specNames(ToolProviderResult result) {
		return result.tools().keySet().stream().map(ToolSpecification::name).collect(java.util.stream.Collectors.toList());
	}

	private static ToolSpecification specNamed(ToolProviderResult result, String name) {
		return result.tools().keySet().stream().filter(spec -> name.equals(spec.name())).findFirst()
				.orElseThrow(() -> new AssertionError("missing spec: " + name));
	}

	private MagicApiTool functionTool() {
		return new MagicApiTool("function-1", "getEnv", "获取环境", MagicApiTool.TYPE_FUNCTION, null, "/公共/getEnv",
				"group-1", "/公共", objectSchema());
	}

	private MagicApiTool apiTool() {
		return new MagicApiTool("api-1", "POST /评估/查询", "查询", MagicApiTool.TYPE_API, "POST", "/评估/查询",
				"group-2", "/评估", schemaWithName());
	}

	private MagicApiTool toolWithName(MagicApiTool tool, String name) {
		return new MagicApiTool(tool.getId(), name, tool.getDescription(), tool.getType(), tool.getMethod(),
				tool.getPath(), tool.getGroupId(), tool.getGroupPath(), tool.getInputSchema());
	}

	private Map<String, Object> objectSchema() {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		return schema;
	}

	private Map<String, Object> schemaWithName() {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		Map<String, Object> properties = new LinkedHashMap<>();
		Map<String, Object> name = new LinkedHashMap<>();
		name.put("type", "string");
		properties.put("name", name);
		schema.put("properties", properties);
		schema.put("required", Collections.singletonList("name"));
		return schema;
	}
}
