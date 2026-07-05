package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.model.McpPromptInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.service.McpPromptMagicResourceStorage;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.*;
import java.util.function.Supplier;

public class MagicApiMcpPromptProvider implements McpPromptProvider {

	private final Supplier<List<McpPromptInfo>> promptSupplier;

	public MagicApiMcpPromptProvider(MagicResourceService magicResourceService) {
		this(() -> magicResourceService.files(McpPromptMagicResourceStorage.FOLDER));
	}

	public MagicApiMcpPromptProvider(Supplier<List<McpPromptInfo>> promptSupplier) {
		this.promptSupplier = promptSupplier;
	}

	@Override
	public McpPage<McpPromptDefinition> listPrompts(McpProviderContext context, McpPageRequest pageRequest) {
		List<McpPromptDefinition> definitions = new ArrayList<>();
		for (McpPromptInfo info : prompts()) {
			definitions.add(toDefinition(info));
		}
		return McpPage.of(definitions, null);
	}

	@Override
	public McpPromptResult getPrompt(McpProviderContext context, McpPromptGetRequest request) {
		if (request == null || request.getName() == null) {
			return null;
		}
		for (McpPromptInfo info : prompts()) {
			if (request.getName().equals(info.getName())) {
				McpPromptResult result = new McpPromptResult();
				result.setDescription(info.getDescription());
				result.setMessages(renderMessages(parseList(info.getMessagesJson()), request.getArguments()));
				return result;
			}
		}
		return null;
	}

	private McpPromptDefinition toDefinition(McpPromptInfo info) {
		McpPromptDefinition definition = new McpPromptDefinition();
		definition.setName(info.getName());
		definition.setDescription(info.getDescription());
		definition.setArguments(toArguments(parseList(info.getArgumentsJson())));
		definition.setAnnotations(parseObject(info.getAnnotationsJson()));
		return definition;
	}

	private List<McpPromptArgument> toArguments(List<Map<String, Object>> values) {
		List<McpPromptArgument> arguments = new ArrayList<>();
		for (Map<String, Object> value : values) {
			McpPromptArgument argument = new McpPromptArgument();
			argument.setName(asString(value.get("name")));
			argument.setDescription(asString(value.get("description")));
			argument.setRequired(Boolean.TRUE.equals(value.get("required")) || "true".equalsIgnoreCase(asString(value.get("required"))));
			arguments.add(argument);
		}
		return arguments;
	}

	private List<Map<String, Object>> renderMessages(List<Map<String, Object>> messages, Map<String, Object> arguments) {
		if (arguments == null || arguments.isEmpty()) {
			return messages;
		}
		List<Map<String, Object>> rendered = new ArrayList<>();
		for (Map<String, Object> message : messages) {
			rendered.add(renderMap(message, arguments));
		}
		return rendered;
	}

	private Map<String, Object> renderMap(Map<String, Object> source, Map<String, Object> arguments) {
		Map<String, Object> target = new LinkedHashMap<>();
		for (Map.Entry<String, Object> entry : source.entrySet()) {
			target.put(entry.getKey(), renderValue(entry.getValue(), arguments));
		}
		return target;
	}

	@SuppressWarnings("unchecked")
	private Object renderValue(Object value, Map<String, Object> arguments) {
		if (value instanceof String) {
			String text = (String) value;
			for (Map.Entry<String, Object> argument : arguments.entrySet()) {
				text = text.replace("{{" + argument.getKey() + "}}", String.valueOf(argument.getValue()));
			}
			return text;
		}
		if (value instanceof Map) {
			return renderMap((Map<String, Object>) value, arguments);
		}
		if (value instanceof List) {
			List<Object> list = new ArrayList<>();
			for (Object item : (List<Object>) value) {
				list.add(renderValue(item, arguments));
			}
			return list;
		}
		return value;
	}

	private List<McpPromptInfo> prompts() {
		List<McpPromptInfo> prompts = promptSupplier.get();
		return prompts == null ? Collections.<McpPromptInfo>emptyList() : prompts;
	}

	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> parseList(String json) {
		if (StringUtils.isBlank(json)) {
			return Collections.emptyList();
		}
		return JSON.parseObject(json, List.class);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> parseObject(String json) {
		if (StringUtils.isBlank(json)) {
			return null;
		}
		return JSON.parseObject(json, Map.class);
	}

	private String asString(Object value) {
		return value == null ? null : String.valueOf(value);
	}
}
