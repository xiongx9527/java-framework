package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.tools;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.*;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.util.*;
import java.util.function.BiFunction;

public class MagicApiMcpToolProvider implements McpToolProvider {

	private final MagicResourceService magicResourceService;

	private final MagicAPIMcpProperties properties;

	private final McpToolScopeMatcher scopeMatcher;

	private final BiFunction<String, Map<String, Object>, Object> functionInvoker;

	private final ApiInvoker apiInvoker;

	public MagicApiMcpToolProvider(MagicResourceService magicResourceService) {
		this(magicResourceService, new MagicAPIMcpProperties(), MagicApiMcpToolProvider::invokeFunction);
	}

	public MagicApiMcpToolProvider(MagicResourceService magicResourceService, MagicAPIMcpProperties properties) {
		this(magicResourceService, properties, MagicApiMcpToolProvider::invokeFunction);
	}

	public MagicApiMcpToolProvider(MagicResourceService magicResourceService,
								   BiFunction<String, Map<String, Object>, Object> functionInvoker) {
		this(magicResourceService, new MagicAPIMcpProperties(), functionInvoker);
	}

	public MagicApiMcpToolProvider(MagicResourceService magicResourceService, MagicAPIMcpProperties properties,
								   BiFunction<String, Map<String, Object>, Object> functionInvoker) {
		this(magicResourceService, properties, functionInvoker, MagicApiMcpToolProvider::invokeApi);
	}

	public MagicApiMcpToolProvider(MagicResourceService magicResourceService, MagicAPIMcpProperties properties,
								   BiFunction<String, Map<String, Object>, Object> functionInvoker,
								   ApiInvoker apiInvoker) {
		this.magicResourceService = magicResourceService;
		this.properties = properties == null ? new MagicAPIMcpProperties() : properties;
		this.scopeMatcher = new McpToolScopeMatcher(this.properties, magicResourceService);
		this.functionInvoker = functionInvoker;
		this.apiInvoker = apiInvoker;
	}

	@Override
	public McpPage<McpToolDefinition> listTools(McpProviderContext context, McpPageRequest pageRequest) {
		List<McpToolDefinition> tools = new ArrayList<>();
		for (MagicEntity entity : magicResourceService.<MagicEntity>files("function")) {
			if (!scopeMatcher.matchesFunction(entity)) {
				continue;
			}
			McpToolDefinition definition = new McpToolDefinition();
			definition.setName(toolName(entity));
			definition.setDescription(entity.getName());
			definition.setInputSchema(inputSchema(entity));
			definition.setOutputSchema(objectSchema());
			tools.add(definition);
		}
		for (ApiInfo info : magicResourceService.<ApiInfo>files("api")) {
			if (!scopeMatcher.matches(info)) {
				continue;
			}
			McpToolDefinition definition = new McpToolDefinition();
			definition.setName(apiToolName(info));
			definition.setDescription(StringUtils.defaultIfBlank(info.getDescription(), info.getName()));
			definition.setInputSchema(inputSchema(info));
			definition.setOutputSchema(objectSchema());
			tools.add(definition);
		}
		return McpPage.of(tools, null);
	}

	@Override
	public McpToolResult callTool(McpProviderContext context, McpToolCallRequest request) {
		if (request == null || request.getName() == null) {
			return null;
		}
		MagicEntity function = findFunction(request.getName());
		ApiInfo api = function == null ? findApi(request.getName()) : null;
		if (function == null && api == null) {
			return null;
		}
		McpToolResult result = new McpToolResult();
		Object value;
		try {
			Map<String, Object> arguments = request.getArguments() == null ? Collections.emptyMap() : request.getArguments();
			value = function == null ? apiInvoker.apply(api, arguments) : functionInvoker.apply(request.getName(), arguments);
			result.setError(false);
		} catch (Exception e) {
			value = Collections.singletonMap("message", e.getMessage());
			result.setError(true);
		}
		if (value instanceof Map) {
			result.setStructuredContent((Map<String, Object>) value);
		} else {
			Map<String, Object> structured = new HashMap<>();
			structured.put("result", value);
			result.setStructuredContent(structured);
		}
		Map<String, Object> content = new HashMap<>();
		content.put("type", "text");
		content.put("text", String.valueOf(value));
		result.setContent(Collections.singletonList(content));
		return result;
	}

	private MagicEntity findFunction(String name) {
		for (MagicEntity entity : magicResourceService.<MagicEntity>files("function")) {
			if (scopeMatcher.matchesFunction(entity) && name.equals(toolName(entity))) {
				return entity;
			}
		}
		return null;
	}

	private ApiInfo findApi(String name) {
		for (ApiInfo info : magicResourceService.<ApiInfo>files("api")) {
			if (scopeMatcher.matches(info) && name.equals(apiToolName(info))) {
				return info;
			}
		}
		return null;
	}

	private String toolName(MagicEntity entity) {
		return entity.getName() == null ? entity.getId() : entity.getName();
	}

	private String apiToolName(ApiInfo info) {
		return StringUtils.upperCase(info.getMethod(), Locale.ENGLISH) + " " + normalizePath(info.getPath());
	}

	private String normalizePath(String path) {
		if (StringUtils.isBlank(path)) {
			return "/";
		}
		return path.startsWith("/") ? path : "/" + path;
	}

	private Map<String, Object> objectSchema() {
		Map<String, Object> schema = new HashMap<>();
		schema.put("type", "object");
		return schema;
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> inputSchema(MagicEntity entity) {
		Object value = entity.getAttribute("mcpInputSchema");
		if (value instanceof Map) {
			return (Map<String, Object>) value;
		}
		if (value instanceof String && StringUtils.isNotBlank((String) value)) {
			try {
				Map<String, Object> schema = JSON.parseObject((String) value, Map.class);
				if (schema != null && "object".equals(schema.get("type"))) {
					return schema;
				}
			} catch (Exception ignored) {
				return objectSchema();
			}
		}
		return objectSchema();
	}

	private static Object invokeFunction(String name, Map<String, Object> arguments) {
		MagicScriptContext context = new MagicScriptContext();
		context.set("funcName", name);
		context.set("args", arguments);
		return ScriptManager.executeScript("import magic; return magic.invoke(funcName, args);", context);
	}

	private static Object invokeApi(ApiInfo info, Map<String, Object> arguments) {
		MagicScriptContext context = new MagicScriptContext();
		context.set("args", arguments);
		context.set("params", arguments);
		context.set("body", arguments);
		context.set("apiInfo", info);
		return ScriptManager.executeScript(info.getScript(), context);
	}

	public interface ApiInvoker {

		Object apply(ApiInfo info, Map<String, Object> arguments);
	}
}
