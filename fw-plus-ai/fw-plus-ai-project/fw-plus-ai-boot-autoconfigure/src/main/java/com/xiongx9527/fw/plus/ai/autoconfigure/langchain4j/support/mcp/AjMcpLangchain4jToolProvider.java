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
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonEnumSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonRawSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.service.tool.ToolProviderRequest;
import dev.langchain4j.service.tool.ToolProviderResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
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
	 * <p>langchain4j 1.x 起 {@code ToolSpecification.Builder#parameters} 只接受
	 * {@link JsonObjectSchema}（MCP 工具 inputSchema 按规范即 object）。</p>
	 *
	 * @param schema MCP 输入模式，可为 {@code null}（视为无参 object）
	 * @return 参数规格
	 */
	private static JsonObjectSchema toParameters(JsonSchema schema) {
		if (schema == null) {
			return JsonObjectSchema.builder().build();
		}

		JsonObjectSchema.Builder parameters = JsonObjectSchema.builder();
		if (schema.getProperties() != null) {
			for (Map.Entry<String, JsonSchemaProperty> entry : schema.getProperties().entrySet()) {
				JsonSchemaElement element = toElement(toNode(entry.getValue()));
				if (element != null) {
					parameters.addProperty(entry.getKey(), element);
				}
			}
		}
		if (schema.getRequired() != null && !schema.getRequired().isEmpty()) {
			parameters.required(schema.getRequired());
		}
		if (schema.getAdditionalProperties() != null) {
			parameters.additionalProperties(schema.getAdditionalProperties());
		}
		return parameters.build();
	}

	/**
	 * 单个参数属性 → 等价的 JSON 树节点（{@code type} / {@code description} /
	 * 扩展关键字合并到同一节点），供 {@link #toElement(JsonNode)} 递归解析。
	 *
	 * @param property 参数属性，可为 {@code null}
	 * @return JSON 树节点；属性为 {@code null} 时返回 {@code null}
	 */
	private static JsonNode toNode(JsonSchemaProperty property) {
		if (property == null) {
			return null;
		}

		ObjectNode node = JsonUtils.createObjectNode();
		if (property.getType() != null) {
			node.put("type", property.getType());
		} else {
			Object typeValue = property.getTypeValue();
			if (typeValue instanceof JsonNode) {
				node.set("type", (JsonNode) typeValue);
			} else if (typeValue != null) {
				node.put("type", String.valueOf(typeValue));
			}
		}
		if (property.getDescription() != null) {
			node.put("description", property.getDescription());
		}
		if (property.getKeywords() != null) {
			for (Map.Entry<String, JsonNode> keyword : property.getKeywords().entrySet()) {
				node.set(keyword.getKey(), keyword.getValue());
			}
		}
		return node;
	}

	/**
	 * JSON Schema 节点 → LangChain4j {@link JsonSchemaElement}（递归）。
	 *
	 * <p>覆盖常见关键字：标量 {@code type} 与 {@code description}、{@code enum}
	 * （转 {@link JsonEnumSchema}）、数组 {@code items}、对象
	 * {@code properties/required/additionalProperties}；其余无法结构化的复合形式
	 * （如 {@code oneOf}/{@code anyOf}）以 {@link JsonRawSchema} 原样保真。</p>
	 *
	 * @param node JSON Schema 节点，可为 {@code null}
	 * @return Schema 元素；节点为 {@code null} / 空时返回 {@code null}
	 */
	private static JsonSchemaElement toElement(JsonNode node) {
		if (node == null || node.isNull() || node.isMissingNode()) {
			return null;
		}
		if (!node.isObject()) {
			return JsonRawSchema.from(node.toString());
		}

		String description = text(node, "description");
		JsonNode enumNode = node.get("enum");
		if (enumNode != null && enumNode.isArray() && enumNode.size() > 0) {
			List<String> values = new ArrayList<>();
			for (JsonNode value : enumNode) {
				values.add(value.asText());
			}
			return JsonEnumSchema.builder().description(description).enumValues(values).build();
		}

		String type = text(node, "type");
		if (type == null) {
			type = node.has("properties") ? "object" : null;
		}
		if (type == null) {
			return JsonRawSchema.from(node.toString());
		}

		switch (type) {
			case "integer":
				return JsonIntegerSchema.builder().description(description).build();
			case "number":
				return JsonNumberSchema.builder().description(description).build();
			case "boolean":
				return JsonBooleanSchema.builder().description(description).build();
			case "string":
				return JsonStringSchema.builder().description(description).build();
			case "array": {
				JsonArraySchema.Builder array = JsonArraySchema.builder().description(description);
				JsonSchemaElement items = toElement(node.get("items"));
				if (items != null) {
					array.items(items);
				}
				return array.build();
			}
			case "object":
				return toObjectElement(node, description);
			default:
				return JsonRawSchema.from(node.toString());
		}
	}

	/**
	 * 对象 Schema 节点 → {@link JsonObjectSchema}。
	 *
	 * @param node        对象节点（含 {@code properties/required/additionalProperties}）
	 * @param description 描述，可为 {@code null}
	 * @return 对象 Schema
	 */
	private static JsonObjectSchema toObjectElement(JsonNode node, String description) {
		JsonObjectSchema.Builder object = JsonObjectSchema.builder().description(description);
		JsonNode properties = node.get("properties");
		if (properties != null && properties.isObject()) {
			Iterator<Map.Entry<String, JsonNode>> fields = properties.fields();
			while (fields.hasNext()) {
				Map.Entry<String, JsonNode> field = fields.next();
				JsonSchemaElement element = toElement(field.getValue());
				if (element != null) {
					object.addProperty(field.getKey(), element);
				}
			}
		}
		JsonNode required = node.get("required");
		if (required != null && required.isArray()) {
			List<String> requiredNames = new ArrayList<>();
			for (JsonNode name : required) {
				requiredNames.add(name.asText());
			}
			if (!requiredNames.isEmpty()) {
				object.required(requiredNames);
			}
		}
		JsonNode additional = node.get("additionalProperties");
		if (additional != null && additional.isBoolean()) {
			object.additionalProperties(additional.booleanValue());
		}
		return object.build();
	}

	/**
	 * 取文本字段。
	 *
	 * @param node  JSON 节点
	 * @param field 字段名
	 * @return 文本值；非文本或缺失时返回 {@code null}
	 */
	private static String text(JsonNode node, String field) {
		JsonNode value = node.get(field);
		return value != null && value.isTextual() ? value.textValue() : null;
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
