package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.mcp;

import com.ajaxjs.mcp.client.IMcpClient;
import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.tools.CallToolRequest;
import com.ajaxjs.mcp.protocol.tools.JsonSchema;
import com.ajaxjs.mcp.protocol.tools.JsonSchemaProperty;
import com.ajaxjs.mcp.protocol.tools.ToolItem;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MCP 客户端 → LangChain4j ToolProvider 适配：规格转换、执行回路由与失败语义。
 */
class AjMcpLangchain4jToolProviderTest {

	@Test
	void shouldConvertToolSpecificationFromMcpTool() {
		IMcpClient client = mock(IMcpClient.class);
		when(client.listTools()).thenReturn(Collections.singletonList(weatherTool()));

		ToolProviderResult result = new AjMcpLangchain4jToolProvider(client).provideTools(null);

		assertThat(result.tools()).hasSize(1);
		ToolSpecification spec = soleKey(result);
		assertThat(spec.name()).isEqualTo("get_weather");
		assertThat(spec.description()).isEqualTo("查询天气");
		// langchain4j 1.x：参数规格为 JsonObjectSchema（type 隐含 object）
		assertThat(spec.parameters()).isNotNull();
		assertThat(spec.parameters().required()).containsExactly("city");
		assertThat(spec.parameters().properties()).containsKey("city");
		JsonSchemaElement city = spec.parameters().properties().get("city");
		assertThat(city).isInstanceOf(JsonEnumSchema.class);
		JsonEnumSchema citySchema = (JsonEnumSchema) city;
		assertThat(citySchema.description()).isEqualTo("城市名");
		assertThat(citySchema.enumValues()).containsExactly("北京", "上海");
	}

	@Test
	void shouldTreatMissingInputSchemaAsEmptyObject() {
		IMcpClient client = mock(IMcpClient.class);
		ToolItem noSchema = new ToolItem();
		noSchema.setName("ping");
		when(client.listTools()).thenReturn(Collections.singletonList(noSchema));

		ToolProviderResult result = new AjMcpLangchain4jToolProvider(client).provideTools(null);

		ToolSpecification spec = soleKey(result);
		assertThat(spec.parameters()).isNotNull();
		assertThat(spec.parameters().properties()).isEmpty();
	}

	@Test
	void shouldRouteExecutionToOwningClient() {
		IMcpClient first = mock(IMcpClient.class);
		when(first.listTools()).thenReturn(Collections.singletonList(tool("tool_a")));
		when(first.callTool(any(CallToolRequest.class))).thenReturn("result_a");
		IMcpClient second = mock(IMcpClient.class);
		when(second.listTools()).thenReturn(Collections.singletonList(tool("tool_b")));
		when(second.callTool(any(CallToolRequest.class))).thenReturn("result_b");

		ToolProviderResult result = new AjMcpLangchain4jToolProvider(Arrays.asList(first, second))
				.provideTools(null);

		assertThat(result.tools()).hasSize(2);
		String firstOut = execute(result, "tool_a", "{\"city\":\"北京\"}");
		String secondOut = execute(result, "tool_b", "{\"city\":\"上海\"}");

		assertThat(firstOut).isEqualTo("result_a");
		assertThat(secondOut).isEqualTo("result_b");

		ArgumentCaptor<CallToolRequest> captor = ArgumentCaptor.forClass(CallToolRequest.class);
		verify(first).callTool(captor.capture());
		assertThat(captor.getValue().getParams().getName()).isEqualTo("tool_a");
		assertThat(captor.getValue().getParams().getArguments()).containsEntry("city", "北京");
		verify(second).callTool(captor.capture());
		assertThat(captor.getValue().getParams().getName()).isEqualTo("tool_b");
	}

	@Test
	void shouldBuildCallToolRequestWithoutArgumentsWhenArgumentsAbsent() {
		IMcpClient client = mock(IMcpClient.class);
		when(client.listTools()).thenReturn(Collections.singletonList(tool("ping")));
		when(client.callTool(any(CallToolRequest.class))).thenReturn("pong");

		ToolProviderResult result = new AjMcpLangchain4jToolProvider(client).provideTools(null);

		assertThat(execute(result, "ping", null)).isEqualTo("pong");

		ArgumentCaptor<CallToolRequest> captor = ArgumentCaptor.forClass(CallToolRequest.class);
		verify(client).callTool(captor.capture());
		assertThat(captor.getValue().getParams().getName()).isEqualTo("ping");
		assertThat(captor.getValue().getParams().getArguments()).isNull();
	}

	@Test
	void shouldSkipToolWithMissingName() {
		IMcpClient client = mock(IMcpClient.class);
		ToolItem nameless = new ToolItem();
		nameless.setDescription("无名工具");
		when(client.listTools()).thenReturn(Arrays.asList(nameless, tool("ok")));

		ToolProviderResult result = new AjMcpLangchain4jToolProvider(client).provideTools(null);

		assertThat(result.tools()).hasSize(1);
		assertThat(soleKey(result).name()).isEqualTo("ok");
	}

	@Test
	void shouldReturnEmptyWhenServerFailsByDefault() {
		IMcpClient client = mock(IMcpClient.class);
		when(client.listTools()).thenThrow(new IllegalStateException("连接失败"));

		ToolProviderResult result = new AjMcpLangchain4jToolProvider(client).provideTools(null);

		assertThat(result.tools()).isEmpty();
	}

	@Test
	void shouldPropagateServerFailureWhenConfigured() {
		IMcpClient client = mock(IMcpClient.class);
		when(client.listTools()).thenThrow(new IllegalStateException("连接失败"));
		List<IMcpClient> clients = Collections.singletonList(client);

		AjMcpLangchain4jToolProvider provider = new AjMcpLangchain4jToolProvider(clients, true);

		assertThatThrownBy(() -> provider.provideTools(null))
				.isInstanceOf(RuntimeException.class)
				.hasMessageContaining("Failed to retrieve tools from MCP server");
	}

	@Test
	void shouldRejectNullMcpToolProvider() {
		assertThatThrownBy(() -> new AjMcpLangchain4jToolProvider((com.ajaxjs.mcp.client.McpToolProvider) null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	/** 构造带 inputSchema（type/properties/required/enum 扩展关键字）的天气工具。 */
	private static ToolItem weatherTool() {
		ToolItem tool = new ToolItem();
		tool.setName("get_weather");
		tool.setDescription("查询天气");

		JsonSchemaProperty city = new JsonSchemaProperty();
		city.setType("string");
		city.setDescription("城市名");
		city.setKeyword("enum", JsonUtils.valueToTree(Arrays.asList("北京", "上海")));

		JsonSchema schema = new JsonSchema();
		schema.setType("object");
		schema.setProperties(Collections.singletonMap("city", city));
		schema.setRequired(Collections.singletonList("city"));
		tool.setInputSchema(schema);
		return tool;
	}

	private static ToolItem tool(String name) {
		ToolItem tool = new ToolItem();
		tool.setName(name);
		return tool;
	}

	/** 按工具名执行，返回工具结果字符串。 */
	private static String execute(ToolProviderResult result, String name, String arguments) {
		ToolExecutionRequest request = ToolExecutionRequest.builder()
				.id("call-1").name(name).arguments(arguments).build();
		for (Map.Entry<ToolSpecification, ToolExecutor> entry : result.tools().entrySet()) {
			if (entry.getKey().name().equals(name)) {
				return entry.getValue().execute(request, null);
			}
		}
		throw new AssertionError("tool not found: " + name);
	}

	private static ToolSpecification soleKey(ToolProviderResult result) {
		assertThat(result.tools()).hasSize(1);
		return new ArrayList<>(result.tools().keySet()).get(0);
	}
}
