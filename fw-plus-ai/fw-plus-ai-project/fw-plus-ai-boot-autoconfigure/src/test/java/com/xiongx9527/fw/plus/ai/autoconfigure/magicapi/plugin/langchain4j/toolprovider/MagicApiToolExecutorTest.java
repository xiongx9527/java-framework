package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.ssssssss.magicapi.core.model.ApiInfo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicApiToolExecutorTest {

	private MagicApiToolRegistry registry;

	private MagicToolInvoker invoker;

	private MagicApiToolExecutor executor;

	@BeforeEach
	void setUp() {
		registry = mock(MagicApiToolRegistry.class);
		invoker = mock(MagicToolInvoker.class);
		executor = new MagicApiToolExecutor(registry, invoker);
	}

	@Test
	void shouldInvokeFunctionByName() {
		when(registry.snapshot()).thenReturn(Collections.singletonList(functionTool()));
		when(invoker.invokeFunction(eq("getEnv"), any())).thenReturn("_dev");

		String result = executor.execute(request("getEnv", "{}"), null);

		assertThat(result).isEqualTo("_dev");
		verify(invoker).invokeFunction(eq("getEnv"), any());
	}

	@Test
	void shouldInvokeApiScriptWithParsedArguments() {
		ApiInfo api = apiInfo();
		when(registry.snapshot()).thenReturn(Collections.singletonList(apiTool()));
		when(registry.api("api-1")).thenReturn(api);
		when(invoker.invokeApi(eq(api), any())).thenReturn(Collections.singletonMap("total", 2));

		String result = executor.execute(request("POST /评估/查询", "{\"name\":\"x\"}"), null);

		assertThat(result).isEqualTo("{\"total\":2}");
		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, Object>> arguments = ArgumentCaptor.forClass(Map.class);
		verify(invoker).invokeApi(eq(api), arguments.capture());
		assertThat(arguments.getValue()).containsEntry("name", "x");
	}

	@Test
	void shouldReturnErrorTextInsteadOfThrowingWhenInvocationFails() {
		when(registry.snapshot()).thenReturn(Collections.singletonList(functionTool()));
		when(invoker.invokeFunction(eq("getEnv"), any())).thenThrow(new IllegalStateException("boom"));

		String result = executor.execute(request("getEnv", "{}"), null);

		assertThat(result).contains("boom");
	}

	@Test
	void shouldReportUnknownTool() {
		when(registry.snapshot()).thenReturn(Collections.emptyList());

		String result = executor.execute(request("missing", "{}"), null);

		assertThat(result).contains("unknown tool");
	}

	private ToolExecutionRequest request(String name, String arguments) {
		return ToolExecutionRequest.builder().name(name).arguments(arguments).build();
	}

	private ApiInfo apiInfo() {
		ApiInfo api = new ApiInfo();
		api.setId("api-1");
		api.setScript("return db.select('x')");
		return api;
	}

	private MagicApiTool functionTool() {
		return new MagicApiTool("function-1", "getEnv", "获取环境", MagicApiTool.TYPE_FUNCTION, null, "/公共/getEnv",
				"group-1", "/公共", objectSchema());
	}

	private MagicApiTool apiTool() {
		return new MagicApiTool("api-1", "POST /评估/查询", "查询", MagicApiTool.TYPE_API, "POST", "/评估/查询",
				"group-2", "/评估", objectSchema());
	}

	private Map<String, Object> objectSchema() {
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		return schema;
	}
}
