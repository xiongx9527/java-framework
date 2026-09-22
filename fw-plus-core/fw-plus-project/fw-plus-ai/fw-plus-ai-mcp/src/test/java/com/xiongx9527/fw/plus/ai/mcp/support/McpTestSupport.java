package com.xiongx9527.fw.plus.ai.mcp.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.mcp.server.McpAsyncServerImpl;
import com.xiongx9527.fw.plus.ai.mcp.server.McpServerBuilder;
import com.xiongx9527.fw.plus.ai.mcp.server.McpServerInfo;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpContent;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPageRequest;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptArgument;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptInfo;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptRequest;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpPromptResult;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpProviderContext;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceContent;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceInfo;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceReadRequest;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceTemplate;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpTool;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolCallRequest;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolProvider;
import com.xiongx9527.fw.plus.ai.mcp.spi.McpToolResult;
import com.xiongx9527.fw.plus.ai.mcp.transport.McpStreamableTransport;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Streamable HTTP 传输集成测试共用夹具
 *
 * <p>提供工具/资源/提示词 Provider 夹具、server 装配入口以及 JSON-RPC 报文构建工具，
 * 供 Servlet（Spring MVC）与 Reactive（WebFlux）两套端点集成测试复用。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public final class McpTestSupport {

	public static final String PATH = "/mcp";

	public static final String SESSION_ID_HEADER = "Mcp-Session-Id";

	public static final String PROTOCOL_VERSION = "2025-11-25";

	public static final String ECHO_TOOL = "echo";

	public static final String BOOM_TOOL = "boom";

	public static final String GREETING_RESOURCE_URI = "test://greeting";

	public static final String TEMPLATE_RESOURCE_URI = "test://file/readme.txt";

	public static final String GREETING_PROMPT = "greeting";

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private McpTestSupport() {
	}

	// ==================== server 装配 ====================

	/**
	 * 装配一个包含工具、资源（含模板）与提示词的 MCP server，并绑定到给定传输
	 */
	public static McpAsyncServerImpl buildServer(McpStreamableTransport transport) throws Exception {
		McpServerBuilder builder = new McpServerBuilder(transport)
				.serverInfo(new McpServerInfo("fw-plus-mcp-it", "1.0.0", "MCP integration test server"))
				.instructions("integration test server");

		McpProviderContext context = McpProviderContext.context();
		McpPageRequest pageRequest = new McpPageRequest();

		McpToolProvider toolProvider = toolProvider();
		for (McpTool tool : toolProvider.listTools(context, pageRequest)) {
			builder.tool(tool, toolProvider);
		}

		McpResourceProvider resourceProvider = resourceProvider();
		for (McpResourceInfo resource : resourceProvider.listResources(context)) {
			builder.resource(resource, resourceProvider);
		}
		for (McpResourceTemplate template : resourceProvider.listTemplates(context)) {
			builder.resourceTemplate(template, resourceProvider);
		}

		McpPromptProvider promptProvider = promptProvider();
		for (McpPromptInfo prompt : promptProvider.listPrompts(context)) {
			builder.prompt(prompt, promptProvider);
		}

		McpAsyncServerImpl server = builder.build();
		transport.bindServer(server);
		server.start();
		return server;
	}

	// ==================== Provider 夹具 ====================

	public static McpToolProvider toolProvider() {
		return new McpToolProvider() {

			@Override
			public List<McpTool> listTools(McpProviderContext context, McpPageRequest pageRequest) {
				return Arrays.asList(
						McpTool.builder()
								.name(ECHO_TOOL)
								.description("回显输入文本")
								.inputSchema(objectSchema("message", true))
								.build(),
						McpTool.builder()
								.name(BOOM_TOOL)
								.description("始终抛出异常，用于验证工具错误结果")
								.build());
			}

			@Override
			public McpToolResult callTool(McpProviderContext context, McpToolCallRequest request) {
				if (BOOM_TOOL.equals(request.getName())) {
					throw new IllegalStateException("boom");
				}
				Object message = request.getArguments() == null ? null : request.getArguments().get("message");
				return McpToolResult.success("echo: " + message);
			}
		};
	}

	public static McpResourceProvider resourceProvider() {
		return new McpResourceProvider() {

			@Override
			public List<McpResourceInfo> listResources(McpProviderContext context) {
				return Collections.singletonList(new McpResourceInfo(GREETING_RESOURCE_URI, "greeting"));
			}

			@Override
			public List<McpResourceTemplate> listTemplates(McpProviderContext context) {
				return Collections.singletonList(new McpResourceTemplate("test://file/{name}", "file"));
			}

			@Override
			public McpResourceContent readResource(McpProviderContext context, McpResourceReadRequest request) {
				return McpResourceContent.text(request.getUri(), "resource content of " + request.getUri());
			}
		};
	}

	public static McpPromptProvider promptProvider() {
		return new McpPromptProvider() {

			@Override
			public List<McpPromptInfo> listPrompts(McpProviderContext context) {
				McpPromptInfo prompt = new McpPromptInfo(GREETING_PROMPT, "问候提示词");
				prompt.setArguments(Collections.singletonList(new McpPromptArgument("name", "被问候者", true)));
				return Collections.singletonList(prompt);
			}

			@Override
			public McpPromptResult getPrompt(McpProviderContext context, McpPromptRequest request) {
				Object name = request.getArguments() == null ? null : request.getArguments().get("name");
				return McpPromptResult.of("问候提示词",
						Collections.singletonList(
								McpPromptResult.McpPromptMessage.user(McpContent.text("hello " + name))));
			}
		};
	}

	// ==================== JSON / JSON-RPC 工具 ====================

	public static String rpc(Object id, String method, Map<String, Object> params) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("jsonrpc", "2.0");
		if (id != null) {
			body.put("id", id);
		}
		body.put("method", method);
		if (params != null) {
			body.put("params", params);
		}
		try {
			return OBJECT_MAPPER.writeValueAsString(body);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException(e);
		}
	}

	public static Map<String, Object> params(Object... keyValues) {
		Map<String, Object> params = new LinkedHashMap<>();
		for (int i = 0; i < keyValues.length; i += 2) {
			params.put((String) keyValues[i], keyValues[i + 1]);
		}
		return params;
	}

	public static Map<String, Object> initializeParams(String protocolVersion) {
		return params(
				"protocolVersion", protocolVersion,
				"capabilities", new LinkedHashMap<String, Object>(),
				"clientInfo", params("name", "fw-plus-mcp-it-client", "version", "1.0.0"));
	}

	public static Map<String, Object> objectSchema(String property, boolean required) {
		Map<String, Object> properties = new LinkedHashMap<>();
		properties.put(property, params("type", "string", "description", "输入文本"));
		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("properties", properties);
		if (required) {
			schema.put("required", Collections.singletonList(property));
		}
		return schema;
	}

	public static JsonNode json(String body) {
		try {
			return OBJECT_MAPPER.readTree(body);
		} catch (IOException e) {
			throw new IllegalStateException("Not a JSON payload: " + body, e);
		}
	}
}
