package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicApiMcpToolRegistryTest {

    private MagicResourceService magicResourceService;

    @BeforeEach
    void setUp() {
        magicResourceService = mock(MagicResourceService.class);
        when(magicResourceService.<MagicEntity>files("function")).thenReturn(Arrays.asList(echoFunction(), hiddenFunction()));
        when(magicResourceService.<ApiInfo>files("api")).thenReturn(Arrays.asList(listApi(), createApi(), customOptInApi()));
        when(magicResourceService.getGroupPath(anyString())).thenReturn("/admin");
        when(magicResourceService.getGroupPath("group-admin")).thenReturn("/admin");
        when(magicResourceService.getGroupPath("group-public")).thenReturn("/public");
    }

    @Test
    void shouldExposeFunctionByDefault() {
        MagicApiMcpToolRegistry registry = registry(new MagicApiMcpProperties());

        List<MagicApiMcpTool> tools = registry.snapshot();

        assertThat(tools).extracting(MagicApiMcpTool::getName).contains("echo", "hidden");
        assertThat(tools).extracting(MagicApiMcpTool::getType)
                .containsOnly(MagicApiMcpTool.TYPE_FUNCTION);
    }

    @Test
    void shouldNotExposeFunctionWhenFunctionsDisabled() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getFunctions().setEnabled(false);

        List<MagicApiMcpTool> tools = registry(properties).snapshot();

        assertThat(tools).isEmpty();
    }

    @Test
    void shouldNotExposeApiByDefault() {
        List<MagicApiMcpTool> tools = registry(new MagicApiMcpProperties()).snapshot();

        assertThat(tools).extracting(MagicApiMcpTool::getName)
                .doesNotContain("GET /orders/list", "POST /orders/create");
    }

    @Test
    void shouldExcludeApiByGroupWhenIncludeGroupsNotMatched() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getApi().setEnabled(true);
        properties.getTools().getApi().getIncludeGroups().add("/other");

        List<MagicApiMcpTool> tools = registry(properties).snapshot();

        assertThat(tools).extracting(MagicApiMcpTool::getName)
                .doesNotContain("GET /orders/list", "POST /orders/create");
    }

    @Test
    void shouldIncludeApiByAntGroupPath() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getApi().setEnabled(true);
        properties.getTools().getApi().getIncludeGroups().add("/adm*");

        List<MagicApiMcpTool> tools = registry(properties).snapshot();

        assertThat(tools).extracting(MagicApiMcpTool::getName)
                .contains("GET /orders/list", "POST /orders/create");
    }

    @Test
    void shouldIncludeApiByAntIncludePaths() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getApi().setEnabled(true);
        properties.getTools().getApi().getIncludePaths().add("/orders/**");

        List<MagicApiMcpTool> tools = registry(properties).snapshot();

        assertThat(tools).extracting(MagicApiMcpTool::getName)
                .contains("GET /orders/list", "POST /orders/create");
    }

    @Test
    void shouldPreferExcludePathsOverIncludePaths() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getApi().setEnabled(true);
        properties.getTools().getApi().getIncludePaths().add("/orders/**");
        properties.getTools().getApi().getExcludePaths().add("/orders/list");

        List<MagicApiMcpTool> tools = registry(properties).snapshot();

        assertThat(tools).extracting(MagicApiMcpTool::getName)
                .doesNotContain("GET /orders/list")
                .contains("POST /orders/create");
    }

    @Test
    void shouldFilterApiByMethodsIgnoringCase() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getApi().setEnabled(true);
        properties.getTools().getApi().getMethods().add("get");

        List<MagicApiMcpTool> tools = registry(properties).snapshot();

        assertThat(tools).extracting(MagicApiMcpTool::getName)
                .contains("GET /orders/list", "GET /orders/custom")
                .doesNotContain("POST /orders/create");
    }

    @Test
    void shouldExcludeApiWithoutOptInWhenRequireOptIn() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getApi().setEnabled(true);
        properties.getTools().getApi().setRequireOptIn(true);

        List<MagicApiMcpTool> tools = registry(properties).snapshot();

        assertThat(tools).extracting(MagicApiMcpTool::getName)
                .contains("GET /orders/list")
                .doesNotContain("POST /orders/create");
    }

    @Test
    void shouldHonorCustomOptInOptionName() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getApi().setEnabled(true);
        properties.getTools().getApi().setRequireOptIn(true);
        properties.getTools().getApi().setOptInOptionName("exposeMcp");

        List<MagicApiMcpTool> tools = registry(properties).snapshot();

        assertThat(tools).extracting(MagicApiMcpTool::getName)
                .doesNotContain("GET /orders/list")
                .contains("GET /orders/custom");
    }

    @Test
    void shouldExcludeFunctionByAntExcludePaths() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getFunctions().getExcludePaths().add("/internal/**");

        List<MagicApiMcpTool> tools = registry(properties).snapshot();

        assertThat(tools).extracting(MagicApiMcpTool::getName)
                .contains("echo")
                .doesNotContain("hidden");
    }

    @Test
    void shouldIncludeFunctionByAntIncludePaths() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getFunctions().getIncludePaths().add("/public/**");

        List<MagicApiMcpTool> tools = registry(properties).snapshot();

        assertThat(tools).extracting(MagicApiMcpTool::getName)
                .contains("echo")
                .doesNotContain("hidden");
    }

    @Test
    void shouldFilterFunctionByIncludeGroups() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getFunctions().getIncludeGroups().add("/public");

        List<MagicApiMcpTool> tools = registry(properties).snapshot();

        assertThat(tools).extracting(MagicApiMcpTool::getName)
                .contains("echo")
                .doesNotContain("hidden");
    }

    @Test
    void shouldCarryApiMetadataIntoSnapshot() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getApi().setEnabled(true);

        MagicApiMcpTool tool = registry(properties).snapshot().stream()
                .filter(item -> "GET /orders/list".equals(item.getName()))
                .findFirst()
                .orElseThrow(AssertionError::new);

        assertThat(tool.getId()).isEqualTo("api-list");
        assertThat(tool.getType()).isEqualTo(MagicApiMcpTool.TYPE_API);
        assertThat(tool.getMethod()).isEqualTo("GET");
        assertThat(tool.getPath()).isEqualTo("/orders/list");
        assertThat(tool.getGroupId()).isEqualTo("group-admin");
        assertThat(tool.getGroupPath()).isEqualTo("/admin");
        assertThat(tool.isOptIn()).isTrue();
    }

    @Test
    void shouldUseMapInputSchemaFromFunctionAttribute() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", Collections.singletonMap("page", Collections.singletonMap("type", "integer")));
        MagicEntity entity = functionEntity("function-schema-map", "schemaMap", "/public/schemaMap", "mcpInputSchema", schema);
        MagicResourceService service = serviceWithFunctions(entity);

        MagicApiMcpTool tool = new MagicApiMcpToolRegistry(service, new MagicApiMcpProperties())
                .snapshot().get(0);

        assertThat(tool.getInputSchema()).containsEntry("type", "object");
        assertThat(tool.getInputSchema()).containsKey("properties");
    }

    @Test
    void shouldParseJsonStringInputSchema() {
        MagicEntity entity = functionEntity("function-schema-json", "schemaJson", "/public/schemaJson", "mcpInputSchema",
                "{\"type\":\"object\",\"properties\":{\"status\":{\"type\":\"string\"}},\"required\":[\"status\"]}");
        MagicResourceService service = serviceWithFunctions(entity);

        MagicApiMcpTool tool = new MagicApiMcpToolRegistry(service, new MagicApiMcpProperties())
                .snapshot().get(0);

        assertThat(tool.getInputSchema()).containsEntry("type", "object");
        assertThat(tool.getInputSchema()).containsKey("required");
    }

    @Test
    void shouldFallbackWhenInputSchemaIsInvalidJson() {
        MagicEntity entity = functionEntity("function-schema-broken", "schemaBroken", "/public/schemaBroken",
                "mcpInputSchema", "{not-json");
        MagicResourceService service = serviceWithFunctions(entity);

        MagicApiMcpTool tool = new MagicApiMcpToolRegistry(service, new MagicApiMcpProperties())
                .snapshot().get(0);

        assertThat(tool.getInputSchema()).containsExactly(entry("type", "object"));
    }

    @Test
    void shouldFallbackWhenInputSchemaTypeIsNotObject() {
        MagicEntity entity = functionEntity("function-schema-array", "schemaArray", "/public/schemaArray",
                "mcpInputSchema", "{\"type\":\"array\"}");
        MagicResourceService service = serviceWithFunctions(entity);

        MagicApiMcpTool tool = new MagicApiMcpToolRegistry(service, new MagicApiMcpProperties())
                .snapshot().get(0);

        assertThat(tool.getInputSchema()).containsExactly(entry("type", "object"));
    }

    @Test
    void shouldFallbackToObjectSchemaWhenAttributeMissing() {
        MagicEntity entity = new MagicEntity();
        entity.setId("function-no-schema");
        entity.setName("noSchema");
        MagicResourceService service = serviceWithFunctions(entity);

        MagicApiMcpTool tool = new MagicApiMcpToolRegistry(service, new MagicApiMcpProperties())
                .snapshot().get(0);

        assertThat(tool.getInputSchema()).containsExactly(entry("type", "object"));
    }

    @Test
    void shouldReadInputSchemaFromApiMetadata() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getApi().setEnabled(true);
        ApiInfo info = new ApiInfo();
        info.setId("api-schema");
        info.setName("带 schema 的 API");
        info.setGroupId("group-admin");
        info.setPath("/orders/schema");
        info.setMethod("GET");
        info.setAttribute("mcpInputSchema",
                "{\"type\":\"object\",\"properties\":{\"page\":{\"type\":\"integer\"}},\"required\":[\"page\"]}");
        MagicResourceService service = mock(MagicResourceService.class);
        when(service.<MagicEntity>files("function")).thenReturn(Collections.emptyList());
        when(service.<ApiInfo>files("api")).thenReturn(Collections.singletonList(info));
        when(service.getGroupPath("group-admin")).thenReturn("/admin");

        MagicApiMcpTool tool = new MagicApiMcpToolRegistry(service, properties).snapshot().get(0);

        assertThat(tool.getInputSchema()).containsEntry("type", "object");
        assertThat(tool.getInputSchema()).containsKey("required");
    }

    @Test
    void shouldNotShareFallbackSchemaInstanceBetweenTools() {
        MagicApiMcpToolRegistry registry = registry(new MagicApiMcpProperties());

        MagicApiMcpTool first = registry.snapshot().get(0);
        MagicApiMcpTool second = registry.snapshot().get(1);

        assertThat(first.getInputSchema()).isNotSameAs(second.getInputSchema());
    }

    @Test
    void shouldSnapshotBeImmutable() {
        List<MagicApiMcpTool> tools = registry(new MagicApiMcpProperties()).snapshot();

        assertThatThrownBy(() -> tools.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldNotReadFilesWhenBothScopesDisabled() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getFunctions().setEnabled(false);
        properties.getTools().getApi().setEnabled(false);
        MagicResourceService service = mock(MagicResourceService.class);

        List<MagicApiMcpTool> tools = new MagicApiMcpToolRegistry(service, properties).snapshot();

        assertThat(tools).isEmpty();
        verify(service, never()).files(anyString());
    }

    private MagicApiMcpToolRegistry registry(MagicApiMcpProperties properties) {
        return new MagicApiMcpToolRegistry(magicResourceService, properties);
    }

    private MagicResourceService serviceWithFunctions(MagicEntity... entities) {
        MagicResourceService service = mock(MagicResourceService.class);
        when(service.<MagicEntity>files("function")).thenReturn(Arrays.asList(entities));
        when(service.<ApiInfo>files("api")).thenReturn(Collections.emptyList());
        when(service.getGroupPath(anyString())).thenReturn("/public");
        return service;
    }

    private static MagicEntity functionEntity(String id, String name, String path, String attribute, Object value) {
        PathMagicEntity entity = new PathMagicEntity();
        entity.setId(id);
        entity.setName(name);
        entity.setPath(path);
        entity.setGroupId("group-public");
        entity.setAttribute(attribute, value);
        return entity;
    }

    private static PathMagicEntity echoFunction() {
        PathMagicEntity entity = new PathMagicEntity();
        entity.setId("function-echo");
        entity.setName("echo");
        entity.setPath("/public/echo");
        entity.setGroupId("group-public");
        return entity;
    }

    private static PathMagicEntity hiddenFunction() {
        PathMagicEntity entity = new PathMagicEntity();
        entity.setId("function-hidden");
        entity.setName("hidden");
        entity.setPath("/internal/secret");
        entity.setGroupId("group-admin");
        return entity;
    }

    private static ApiInfo listApi() {
        ApiInfo info = new ApiInfo();
        info.setId("api-list");
        info.setName("订单列表");
        info.setDescription("订单列表接口");
        info.setGroupId("group-admin");
        info.setPath("/orders/list");
        info.setMethod("GET");
        info.setAttribute("mcpTool", true);
        return info;
    }

    private static ApiInfo createApi() {
        ApiInfo info = new ApiInfo();
        info.setId("api-create");
        info.setName("创建订单");
        info.setGroupId("group-admin");
        info.setPath("/orders/create");
        info.setMethod("POST");
        return info;
    }

    private static ApiInfo customOptInApi() {
        ApiInfo info = new ApiInfo();
        info.setId("api-custom");
        info.setName("订单导出");
        info.setGroupId("group-admin");
        info.setPath("/orders/custom");
        info.setMethod("GET");
        info.setAttribute("exposeMcp", true);
        return info;
    }
}
