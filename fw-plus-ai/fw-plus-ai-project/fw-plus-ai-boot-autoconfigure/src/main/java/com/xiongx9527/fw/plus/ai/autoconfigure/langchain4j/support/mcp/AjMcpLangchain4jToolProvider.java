package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.mcp;

import com.ajaxjs.mcp.client.IMcpClient;
import com.ajaxjs.mcp.client.McpToolProvider;
import com.ajaxjs.mcp.client.McpToolProviderResult;
import com.ajaxjs.mcp.common.JsonUtils;
import com.ajaxjs.mcp.protocol.tools.CallToolRequest;
import com.ajaxjs.mcp.protocol.tools.JsonSchema;
import com.ajaxjs.mcp.protocol.tools.JsonSchemaProperty;
import com.ajaxjs.mcp.protocol.tools.ToolItem;
import com.fasterxml.jackson.databind.JsonNode;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 把 MCP 客户端（aj-mcp）的工具集适配为 LangChain4j {@link ToolProvider}。
 *
 * <p>两层转换：底层先用 aj-mcp 的 {@link McpToolProvider} 聚合一个或多个
 * {@link IMcpClient} 的工具并把每个工具绑定到所属 client 的执行函数，本类再把这些
 * {@link ToolItem} 转成 {@link ToolSpecification}、把绑定函数包成 {@code ToolExecutor}，
 * 产出 LangChain4j 的 {@link ToolProviderResult}，可直接挂到
 * {@code AssistantBuilder.toolProvider(...)} 或 {@code AiServices.toolProvider(...)}。</p>
 *
 * <p>工具发现按需进行：每次 {@link #provideTools(ToolProviderRequest)} 都会向各 server
 * 发起 {@code tools/list}，本类不缓存（与 aj-mcp 语义一致）。client 的初始化与关闭由
 * 调用方负责。</p>
 *
 * <p>失败策略沿用 aj-mcp：默认（{@code failIfOneServerFails=false}）单 server 拉取失败
 * 只记日志并跳过；单个工具转换失败只跳过该工具，不影响其余工具。</p>
 */
public class AjMcpLangchain4jToolProvider implements ToolProvider {

	private static final Logger logger = LoggerFactory.getLogger(AjMcpLangchain4jToolProvider.class);

	/** aj-mcp 工具聚合器（负责 tools/list 与 client 绑定）。 */
	private final McpToolProvider mcpToolProvider;

	/**
	 * 单客户端装配。
	 *
	 * @param client 已 {@link IMcpClient#initialize()} 的 MCP 客户端
	 */
	public AjMcpLangchain4jToolProvider(IMcpClient client) {
		this(AjMcpToolProviderFactory.fromClient(client));
	}

	/**
	 * 多客户端装配，单 server 失败时忽略并继续。
	 *
	 * @param clients 已初始化的 MCP 客户端列表
	 */
	public AjMcpLangchain4jToolProvider(List<IMcpClient> clients) {
		this(AjMcpToolProviderFactory.fromClients(clients));
	}

	/**
	 * 多客户端装配，可指定失败策略。
	 *
	 * @param clients              已初始化的 MCP 客户端列表
	 * @param failIfOneServerFails 为 {@code true} 时任一 server 拉取工具失败即抛异常
	 */
	public AjMcpLangchain4jToolProvider(List<IMcpClient> clients, boolean failIfOneServerFails) {
		this(AjMcpToolProviderFactory.fromClients(clients, failIfOneServerFails));
	}

	/**
	 * 直接持有已配置好的 aj-mcp 工具聚合器。
	 *
	 * @param mcpToolProvider aj-mcp 的工具聚合器，不能为 {@code null}
	 */
	public AjMcpLangchain4jToolProvider(McpToolProvider mcpToolProvider) {
		if (mcpToolProvider == null) {
			throw new IllegalArgumentException("mcpToolProvider must not be null");
		}
		this.mcpToolProvider = mcpToolProvider;
	}

	@Override
	public ToolProviderResult provideTools(ToolProviderRequest request) {
		McpToolProviderResult discovered = mcpToolProvider.provideTools();

		ToolProviderResult.Builder builder = ToolProviderResult.builder();
		for (Map.Entry<ToolItem, Function<CallToolRequest, String>> entry : discovered.entrySet()) {
			ToolItem tool = entry.getKey();
			try {
				builder.add(toSpecification(tool), toExecutor(entry.getValue()));
			} catch (Exception e) {
				logger.warn("MCP 工具 [{}] 转换为 LangChain4j 规格失败，已跳过",
						tool == null ? null : tool.getName(), e);
			}
		}
		return builder.build();
	}

	/**
	 * MCP 工具元数据 → LangChain4j 工具规格。
	 *
	 * @param tool MCP 工具
	 * @return 工具规格
	 * @throws IllegalArgumentException 工具缺少 name
	 */
	private static ToolSpecification toSpecification(ToolItem tool) {
		if (tool == null || tool.getName() == null || tool.getName().isEmpty()) {
			throw new IllegalArgumentException("MCP tool name must not be empty");
		}

		ToolSpecification.Builder spec = ToolSpecification.builder().name(tool.getName());
		if (tool.getDescription() != null) {
			spec.description(tool.getDescription());
		}
		spec.parameters(toParameters(tool.getInputSchema()));
		return spec.build();
	}

	/**
	 * MCP inputSchema → LangChain4j 参数规格（JSON Schema 的
	 * {@code type/properties/required} 三要素）。
	 *
	 * @param schema MCP 输入模式，可为 {@code null}（视为无参 object）
	 * @return 参数规格
	 */
	private static ToolParameters toParameters(JsonSchema schema) {
		if (schema == null) {
			return ToolParameters.builder()
					.type("object")
					.properties(Collections.<String, Map<String, Object>>emptyMap())
					.build();
		}

		Map<String, Map<String, Object>> properties = new LinkedHashMap<>();
		if (schema.getProperties() != null) {
			for (Map.Entry<String, JsonSchemaProperty> entry : schema.getProperties().entrySet()) {
				Map<String, Object> keywords = toPropertyKeywords(entry.getValue());
				if (keywords != null) {
					properties.put(entry.getKey(), keywords);
				}
			}
		}

		ToolParameters.Builder parameters = ToolParameters.builder()
				.type(resolveType(schema))
				.properties(properties);
		if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
			parameters.required(schema.getRequired());
		}
		return parameters.build();
	}

	/**
	 * 单个参数属性 → JSON 关键字映射（{@code type}、{@code description} 及扩展关键字）。
	 *
	 * @param property 参数属性，可为 {@code null}
	 * @return 关键字映射；属性为 {@code null} 时返回 {@code null}
	 */
	private static Map<String, Object> toPropertyKeywords(JsonSchemaProperty property) {
		if (property == null) {
			return null;
		}

		Map<String, Object> keywords = new LinkedHashMap<>();
		if (property.getTypeValue() != null) {
			keywords.put("type", toPlainValue(property.getTypeValue()));
		}
		if (property.getDescription() != null) {
			keywords.put("description", property.getDescription());
		}
		if (property.getKeywords() != null) {
			for (Map.Entry<String, JsonNode> keyword : property.getKeywords().entrySet()) {
				keywords.put(keyword.getKey(), toPlainValue(keyword.getValue()));
			}
		}
		return keywords;
	}

	/**
	 * 顶层 schema 的 {@code type}：标量直接用；数组等复杂形式回退为 {@code object}
	 * （MCP 工具 inputSchema 按规范即 object）。
	 *
	 * @param schema MCP 输入模式
	 * @return 类型字符串
	 */
	private static String resolveType(JsonSchema schema) {
		String type = schema.getType();
		if (type != null) {
			return type;
		}
		Object typeValue = schema.getTypeValue();
		if (typeValue instanceof JsonNode && ((JsonNode) typeValue).isTextual()) {
			return ((JsonNode) typeValue).textValue();
		}
		return "object";
	}

	/**
	 * Jackson 树节点 → 普通 Java 值（Map/List/String…），非节点值原样返回。
	 *
	 * @param value 值
	 * @return 普通 Java 值
	 */
	private static Object toPlainValue(Object value) {
		if (value instanceof JsonNode) {
			return JsonUtils.convertValue(value, Object.class);
		}
		return value;
	}

	/**
	 * client 绑定的执行函数 → LangChain4j 工具执行器。
	 *
	 * @param callTool 由 aj-mcp 绑定到所属 client 的执行函数
	 * @return 工具执行器
	 */
	private static ToolExecutor toExecutor(Function<CallToolRequest, String> callTool) {
		return (executionRequest, memoryId) -> {
			String arguments = executionRequest.arguments();
			CallToolRequest request = (arguments == null || arguments.trim().isEmpty())
					? new CallToolRequest(executionRequest.name())
					: new CallToolRequest(executionRequest.name(), arguments);
			return callTool.apply(request);
		};
	}
}
