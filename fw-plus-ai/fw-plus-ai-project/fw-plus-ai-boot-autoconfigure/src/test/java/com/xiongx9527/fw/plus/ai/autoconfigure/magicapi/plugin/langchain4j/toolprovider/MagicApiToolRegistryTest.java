package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MagicApiToolRegistryTest {

	private static final String SCHEMA_JSON = "{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"}}}";

	private MagicResourceService magicResourceService;

	private Langchain4jToolProperties.Tools tools;

	private MagicApiToolRegistry registry;

	@BeforeEach
	void setUp() {
		magicResourceService = mock(MagicResourceService.class);
		when(magicResourceService.getGroupPath(anyString())).thenReturn("/公共");
		tools = new Langchain4jToolProperties.Tools();
		registry = new MagicApiToolRegistry(magicResourceService, tools);
	}

	@Test
	void shouldExposeFunctionsAndApisAsTools() {
		when(magicResourceService.<MagicEntity>files("function")).thenReturn(functionEntities());
		when(magicResourceService.<ApiInfo>files("api")).thenReturn(apiInfos());

		List<MagicApiTool> snapshot = registry.snapshot();

		assertThat(snapshot).extracting(MagicApiTool::getName)
				.containsExactlyInAnyOrder("getEnv", "POST /评估/查询");
		assertThat(snapshot).extracting(MagicApiTool::getType)
				.containsExactlyInAnyOrder(MagicApiTool.TYPE_FUNCTION, MagicApiTool.TYPE_API);
	}

	@Test
	void shouldFilterApisByConfiguredMethods() {
		tools.getApi().setMethods(Collections.singletonList("GET"));
		when(magicResourceService.<ApiInfo>files("api")).thenReturn(Arrays.asList(apiInfo("POST"), apiInfo("GET")));

		List<MagicApiTool> snapshot = registry.snapshot();

		assertThat(snapshot).extracting(MagicApiTool::getMethod).containsExactly("GET");
	}

	@Test
	void shouldFilterByIncludeGroups() {
		tools.getApi().setIncludeGroups(Collections.singletonList("/评估"));
		when(magicResourceService.getGroupPath("group-eval")).thenReturn("/评估/查询");
		when(magicResourceService.getGroupPath("group-other")).thenReturn("/系统/配置");
		when(magicResourceService.<ApiInfo>files("api")).thenReturn(
				Arrays.asList(apiInfoInGroup("group-eval"), apiInfoInGroup("group-other")));

		List<MagicApiTool> snapshot = registry.snapshot();

		assertThat(snapshot).extracting(MagicApiTool::getGroupId).containsExactly("group-eval");
	}

	@Test
	void shouldFilterByExcludeGroups() {
		tools.getApi().setExcludeGroups(Collections.singletonList("/系统/**"));
		when(magicResourceService.getGroupPath("group-eval")).thenReturn("/评估/查询");
		when(magicResourceService.getGroupPath("group-other")).thenReturn("/系统/配置");
		when(magicResourceService.<ApiInfo>files("api")).thenReturn(
				Arrays.asList(apiInfoInGroup("group-eval"), apiInfoInGroup("group-other")));

		List<MagicApiTool> snapshot = registry.snapshot();

		assertThat(snapshot).extracting(MagicApiTool::getGroupId).containsExactly("group-eval");
	}

	@Test
	void shouldParseInputSchemaFromMapAttribute() {
		ApiInfo api = apiInfos().get(0);
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		api.setAttribute("mcpInputSchema", schema);
		when(magicResourceService.<ApiInfo>files("api")).thenReturn(Collections.singletonList(api));

		assertThat(registry.snapshot().get(0).getInputSchema()).isEqualTo(schema);
	}

	@Test
	void shouldParseInputSchemaFromJsonStringAttribute() {
		ApiInfo api = apiInfos().get(0);
		api.setAttribute("mcpInputSchema", SCHEMA_JSON);
		when(magicResourceService.<ApiInfo>files("api")).thenReturn(Collections.singletonList(api));

		Map<String, Object> inputSchema = registry.snapshot().get(0).getInputSchema();

		assertThat(inputSchema).containsEntry("type", "object");
		assertThat(inputSchema.get("properties")).isNotNull();
	}

	@Test
	void shouldFallbackToObjectSchemaWhenAttributeMissing() {
		when(magicResourceService.<ApiInfo>files("api")).thenReturn(apiInfos());

		assertThat(registry.snapshot().get(0).getInputSchema())
				.containsExactly(entry("type", "object"));
	}

	@Test
	void shouldPreferFunctionWhenApiToolNameConflicts() {
		PathMagicEntity function = new PathMagicEntity();
		function.setId("function-1");
		function.setName("POST /评估/查询");
		function.setGroupId("group-1");
		when(magicResourceService.<MagicEntity>files("function")).thenReturn(Collections.singletonList(function));
		when(magicResourceService.<ApiInfo>files("api")).thenReturn(apiInfos());

		List<MagicApiTool> snapshot = registry.snapshot();

		assertThat(snapshot).hasSize(1);
		assertThat(snapshot.get(0).getType()).isEqualTo(MagicApiTool.TYPE_FUNCTION);
	}

	@Test
	void shouldCacheSnapshotUntilInvalidated() {
		when(magicResourceService.<ApiInfo>files("api")).thenReturn(
				Collections.singletonList(apiInfoWithPath("/first")));
		assertThat(registry.snapshot()).extracting(MagicApiTool::getPath).containsExactly("/first");

		when(magicResourceService.<ApiInfo>files("api")).thenReturn(
				Collections.singletonList(apiInfoWithPath("/second")));
		assertThat(registry.snapshot()).extracting(MagicApiTool::getPath)
				.as("未失效前应返回缓存")
				.containsExactly("/first");

		registry.invalidateSnapshot();
		assertThat(registry.snapshot()).extracting(MagicApiTool::getPath).containsExactly("/second");
	}

	@Test
	void shouldFindApiByIdForLocalExecution() {
		ApiInfo api = apiInfos().get(0);
		when(magicResourceService.<ApiInfo>files("api")).thenReturn(Collections.singletonList(api));
		registry.snapshot();

		assertThat(registry.api("api-1")).isSameAs(api);
		assertThat(registry.api("missing")).isNull();
	}

	@Test
	void shouldSkipApiWithoutMethodWhenMethodWhitelistConfigured() {
		tools.getApi().setMethods(Collections.singletonList("GET"));
		when(magicResourceService.<ApiInfo>files("api"))
				.thenReturn(Collections.singletonList(apiInfo(null)));

		assertThat(registry.snapshot()).isEmpty();
	}

	private List<MagicEntity> functionEntities() {		PathMagicEntity function = new PathMagicEntity();
		function.setId("function-1");
		function.setName("getEnv");
		function.setGroupId("group-1");
		function.setPath("/公共/获取环境信息");
		return Collections.singletonList(function);
	}

	private List<ApiInfo> apiInfos() {
		return Collections.singletonList(apiInfoWithPath("/评估/查询"));
	}

	private ApiInfo apiInfo(String method) {
		ApiInfo api = apiInfoWithPath("/评估/查询");
		api.setMethod(method);
		return api;
	}

	private ApiInfo apiInfoWithPath(String path) {
		ApiInfo api = new ApiInfo();
		api.setId("api-1");
		api.setName("查询");
		api.setGroupId("group-2");
		api.setPath(path);
		api.setMethod("POST");
		return api;
	}

	private ApiInfo apiInfoInGroup(String groupId) {
		ApiInfo api = apiInfoWithPath("/评估/查询");
		api.setGroupId(groupId);
		return api;
	}
}
