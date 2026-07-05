package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.tools;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.*;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.util.*;
import java.util.function.BiFunction;

public class MagicApiMcpToolProvider implements McpToolProvider {

	private final MagicResourceService magicResourceService;

	private final BiFunction<String, Map<String, Object>, Object> functionInvoker;

	public MagicApiMcpToolProvider(MagicResourceService magicResourceService) {
		this(magicResourceService, MagicApiMcpToolProvider::invokeFunction);
	}

	public MagicApiMcpToolProvider(MagicResourceService magicResourceService,
								   BiFunction<String, Map<String, Object>, Object> functionInvoker) {
		this.magicResourceService = magicResourceService;
		this.functionInvoker = functionInvoker;
	}

	@Override
	public McpPage<McpToolDefinition> listTools(McpProviderContext context, McpPageRequest pageRequest) {
		List<McpToolDefinition> tools = new ArrayList<>();
		for (MagicEntity entity : magicResourceService.<MagicEntity>files("function")) {
			McpToolDefinition definition = new McpToolDefinition();
			definition.setName(toolName(entity));
			definition.setDescription(entity.getName());
			Map<String, Object> schema = new HashMap<>();
			schema.put("type", "object");
			definition.setInputSchema(schema);
			definition.setOutputSchema(schema);
			tools.add(definition);
		}
		return McpPage.of(tools, null);
	}

	@Override
	public McpToolResult callTool(McpProviderContext context, McpToolCallRequest request) {
		if (request == null || request.getName() == null || !exists(request.getName())) {
			return null;
		}
		McpToolResult result = new McpToolResult();
		Object value;
		try {
			value = functionInvoker.apply(request.getName(),
					request.getArguments() == null ? Collections.emptyMap() : request.getArguments());
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

	private boolean exists(String name) {
		for (MagicEntity entity : magicResourceService.<MagicEntity>files("function")) {
			if (name.equals(toolName(entity))) {
				return true;
			}
		}
		return false;
	}

	private String toolName(MagicEntity entity) {
		return entity.getName() == null ? entity.getId() : entity.getName();
	}

	private static Object invokeFunction(String name, Map<String, Object> arguments) {
		MagicScriptContext context = new MagicScriptContext();
		context.set("funcName", name);
		context.set("args", arguments);
		return ScriptManager.executeScript("import magic; return magic.invoke(funcName, args);", context);
	}
}
