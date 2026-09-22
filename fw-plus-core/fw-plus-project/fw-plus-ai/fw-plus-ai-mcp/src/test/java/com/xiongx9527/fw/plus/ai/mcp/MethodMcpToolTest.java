package com.xiongx9527.fw.plus.ai.mcp;

import com.xiongx9527.fw.plus.ai.mcp.annotation.McpTool;
import com.xiongx9527.fw.plus.ai.mcp.annotation.McpToolParam;
import com.xiongx9527.fw.plus.ai.mcp.server.MethodMcpTool;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpContent;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolCallRequest;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolResult;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MethodMcpTool 注解适配器单元测试
 *
 * <p>spec 7.2：@McpTool 方法注解 + @McpToolParam 参数元数据，
 * 自动生成 inputSchema 并可被 tools/call 调用。
 */
public class MethodMcpToolTest {

	public static class GreetingService {
		@McpTool(name = "greet", description = "Greet a user")
		public String greet(@McpToolParam(name = "name", required = true) String name) {
			return "Hello, " + name + "!";
		}

		@McpTool(name = "fail", description = "Always fails")
		public String fail() {
			throw new RuntimeException("boom");
		}

		@McpTool(name = "content", description = "Returns content")
		public McpContent content() {
			return McpContent.text("raw");
		}
	}

	@Test
	public void shouldGenerateSchemaAndListTool() throws Exception {
		GreetingService bean = new GreetingService();
		Method method = GreetingService.class.getMethod("greet", String.class);
		com.xiongx9527.fw.plus.ai.mcp.spi.McpTool toolDef =
				com.xiongx9527.fw.plus.ai.mcp.spi.McpTool.builder()
						.name("greet")
						.description("Greet a user")
						.build();
		MethodMcpTool provider = new MethodMcpTool(bean, method, toolDef);
		List<com.xiongx9527.fw.plus.ai.mcp.spi.McpTool> tools =
				provider.listTools(McpProviderContext.context(), null);
		assertThat(tools).hasSize(1);
		com.xiongx9527.fw.plus.ai.mcp.spi.McpTool tool = tools.get(0);
		assertThat(tool.name()).isEqualTo("greet");
		Map<String, Object> schema = tool.inputSchema();
		assertThat(schema).isNotNull();
		assertThat(schema.get("type")).isEqualTo("object");
		Map<?, ?> properties = (Map<?, ?>) schema.get("properties");
		assertThat(properties.containsKey("name")).isTrue();
		assertThat(((Map<?, ?>) properties.get("name")).get("type")).isEqualTo("string");
		@SuppressWarnings("unchecked")
		List<String> required = (List<String>) schema.get("required");
		assertThat(required).contains("name");
	}

	@Test
	public void shouldCallToolAndWrapStringResult() throws Exception {
		GreetingService bean = new GreetingService();
		Method method = GreetingService.class.getMethod("greet", String.class);
		com.xiongx9527.fw.plus.ai.mcp.spi.McpTool toolDef =
				com.xiongx9527.fw.plus.ai.mcp.spi.McpTool.builder()
						.name("greet")
						.description("Greet a user")
						.build();
		MethodMcpTool provider = new MethodMcpTool(bean, method, toolDef);
		McpToolCallRequest request = new McpToolCallRequest();
		request.setName("greet");
		Map<String, Object> args = new LinkedHashMap<>();
		args.put("name", "Alice");
		request.setArguments(args);
		McpToolResult result = provider.callTool(McpProviderContext.context(), request);
		assertThat(result.isError()).isFalse();
		List<McpContent> contents = result.getContent();
		assertThat(contents).hasSize(1);
		assertThat(contents.get(0).getText()).isEqualTo("Hello, Alice!");
	}

	@Test
	public void shouldWrapExceptionAsErrorResult() throws Exception {
		GreetingService bean = new GreetingService();
		Method method = GreetingService.class.getMethod("fail");
		com.xiongx9527.fw.plus.ai.mcp.spi.McpTool toolDef =
				com.xiongx9527.fw.plus.ai.mcp.spi.McpTool.builder()
						.name("fail")
						.description("Always fails")
						.build();
		MethodMcpTool provider = new MethodMcpTool(bean, method, toolDef);
		McpToolCallRequest request = new McpToolCallRequest();
		request.setName("fail");
		McpToolResult result = provider.callTool(McpProviderContext.context(), request);
		assertThat(result.isError()).isTrue();
		assertThat(result.getContent()).isNotEmpty();
		assertThat(result.getContent().get(0).getText()).contains("boom");
	}

	@Test
	public void shouldWrapMcpContentResult() throws Exception {
		GreetingService bean = new GreetingService();
		Method method = GreetingService.class.getMethod("content");
		com.xiongx9527.fw.plus.ai.mcp.spi.McpTool toolDef =
				com.xiongx9527.fw.plus.ai.mcp.spi.McpTool.builder()
						.name("content")
						.description("Returns content")
						.build();
		MethodMcpTool provider = new MethodMcpTool(bean, method, toolDef);
		McpToolCallRequest request = new McpToolCallRequest();
		request.setName("content");
		McpToolResult result = provider.callTool(McpProviderContext.context(), request);
		assertThat(result.isError()).isFalse();
		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().get(0).getText()).isEqualTo("raw");
	}
}
