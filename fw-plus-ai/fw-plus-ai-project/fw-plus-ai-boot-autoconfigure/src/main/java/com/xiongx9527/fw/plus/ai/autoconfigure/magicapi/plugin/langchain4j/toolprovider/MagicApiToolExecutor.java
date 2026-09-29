package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.toolprovider;

import com.alibaba.fastjson2.JSON;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.service.tool.ToolExecutor;
import org.ssssssss.magicapi.core.model.ApiInfo;

import java.util.Collections;
import java.util.Map;

/**
 * LangChain4j {@link ToolExecutor}：把一次工具调用落到 Magic-API 本地脚本执行。
 *
 * <p>语义：</p>
 * <ul>
 * <li>入参：LLM 传来的 JSON 字符串解析为 Map，原样透传给脚本（function 的 {@code args}、
 * api 的 {@code args/params/body}）；</li>
 * <li>返回：执行结果序列化为字符串（Map/集合/数组走 JSON，其余走 {@code String.valueOf}）；</li>
 * <li>失败：不抛给 agent 循环——工具不存在或脚本异常时返回错误文本，
 * 让 LLM 自行决定重试或放弃。</li>
 * </ul>
 */
public class MagicApiToolExecutor implements ToolExecutor {

	private final MagicApiToolRegistry registry;

	private final MagicToolInvoker invoker;

	public MagicApiToolExecutor(MagicApiToolRegistry registry, MagicToolInvoker invoker) {
		this.registry = registry;
		this.invoker = invoker;
	}

	@Override
	public String execute(ToolExecutionRequest request, Object memoryId) {
		try {
			MagicApiTool tool = find(request.name());
			if (tool == null) {
				return "unknown tool: " + request.name();
			}
			Map<String, Object> arguments = parseArguments(request.arguments());
			Object value = MagicApiTool.TYPE_FUNCTION.equals(tool.getType())
					? invoker.invokeFunction(tool.getName(), arguments)
					: invoker.invokeApi(locateApi(tool), arguments);
			return stringify(value);
		} catch (Exception e) {
			String message = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
			return "tool execution failed: " + message;
		}
	}

	private MagicApiTool find(String name) {
		if (name == null) {
			return null;
		}
		for (MagicApiTool tool : registry.snapshot()) {
			if (name.equals(tool.getName())) {
				return tool;
			}
		}
		return null;
	}

	private ApiInfo locateApi(MagicApiTool tool) {
		ApiInfo info = registry.api(tool.getId());
		if (info == null) {
			throw new IllegalStateException("api resource missing: " + tool.getId());
		}
		return info;
	}

	private Map<String, Object> parseArguments(String arguments) {
		if (arguments == null || arguments.trim().isEmpty() || "null".equals(arguments.trim())) {
			return Collections.emptyMap();
		}
		try {
			Map<String, Object> parsed = JSON.parseObject(arguments, Map.class);
			return parsed == null ? Collections.emptyMap() : parsed;
		} catch (Exception e) {
			throw new IllegalArgumentException("arguments are not a JSON object: " + arguments);
		}
	}

	private String stringify(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof String) {
			return (String) value;
		}
		if (value instanceof Map || value instanceof Iterable || value.getClass().isArray()) {
			return JSON.toJSONString(value);
		}
		return String.valueOf(value);
	}
}
