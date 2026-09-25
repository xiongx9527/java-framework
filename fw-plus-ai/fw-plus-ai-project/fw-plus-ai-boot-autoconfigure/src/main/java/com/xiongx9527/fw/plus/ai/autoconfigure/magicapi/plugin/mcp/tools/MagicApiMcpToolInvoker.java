package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools;

import com.ajaxjs.mcp.protocol.common.Content;
import com.ajaxjs.mcp.protocol.common.ContentText;
import com.ajaxjs.mcp.protocol.tools.StructuredToolResult;
import org.ssssssss.magicapi.core.model.ApiInfo;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 按工具名从注册表快照中定位工具并执行：function 走 {@link FunctionInvoker}，
 * api 走 {@link ApiInvoker}，结果统一组装为 content + structuredContent。
 */
public class MagicApiMcpToolInvoker {

	private final MagicApiMcpToolRegistry registry;

	private final FunctionInvoker functionInvoker;

	private final ApiInvoker apiInvoker;

	public MagicApiMcpToolInvoker(MagicApiMcpToolRegistry registry, FunctionInvoker functionInvoker, ApiInvoker apiInvoker) {
		this.registry = registry;
		this.functionInvoker = functionInvoker;
		this.apiInvoker = apiInvoker;
	}

	/**
	 * @return 工具不存在或已被过滤时返回 {@code null}，否则返回调用结果
	 */
	public StructuredToolResult invoke(String toolName, Map<String, Object> arguments) {
		MagicApiMcpTool tool = find(toolName);
		if (tool == null) {
			return null;
		}
		Map<String, Object> args = arguments == null ? Collections.emptyMap() : arguments;
		try {
			Object value = MagicApiMcpTool.TYPE_FUNCTION.equals(tool.getType())
					? functionInvoker.invoke(tool, args)
					: apiInvoker.invoke(findApi(tool), args);
			return success(value);
		} catch (Exception e) {
			return error(e.getMessage());
		}
	}

	private MagicApiMcpTool find(String toolName) {
		if (toolName == null) {
			return null;
		}
		for (MagicApiMcpTool tool : registry.snapshot()) {
			if (toolName.equals(tool.getName())) {
				return tool;
			}
		}
		return null;
	}

	private ApiInfo findApi(MagicApiMcpTool tool) {
		List<ApiInfo> apis = registry.getMagicResourceService().files("api");
		for (ApiInfo info : apis) {
			if (tool.getId().equals(info.getId())) {
				return info;
			}
		}
		return null;
	}

	private StructuredToolResult success(Object value) {
		StructuredToolResult result = new StructuredToolResult();
		result.setError(false);
		result.setContent(Collections.<Content>singletonList(new ContentText(String.valueOf(value))));
		if (value instanceof Map) {
			result.setStructuredContent((Map<String, Object>) value);
		} else {
			Map<String, Object> structured = new LinkedHashMap<>();
			structured.put("result", value);
			result.setStructuredContent(structured);
		}
		return result;
	}

	private StructuredToolResult error(String message) {
		String text = message == null ? "tool invocation failed" : message;
		StructuredToolResult result = new StructuredToolResult();
		result.setError(true);
		result.setContent(Collections.<Content>singletonList(new ContentText(text)));
		Map<String, Object> structured = new LinkedHashMap<>();
		structured.put("message", text);
		result.setStructuredContent(structured);
		return result;
	}

	public interface FunctionInvoker {

		Object invoke(MagicApiMcpTool tool, Map<String, Object> arguments);
	}

	public interface ApiInvoker {

		Object invoke(ApiInfo info, Map<String, Object> arguments);
	}
}
