package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.starter;

import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.tools.CallToolRequestParams;
import com.ajaxjs.mcp.protocol.tools.CallToolResult;
import com.ajaxjs.mcp.protocol.tools.CallToolResultDetail;
import com.ajaxjs.mcp.protocol.tools.StructuredToolResult;
import com.ajaxjs.mcp.common.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolInvoker;
import com.xiongx9527.fw.plus.ai.mcp.server.spi.McpServerRegistry;

import java.util.Collections;
import java.util.Map;

/**
 * 在内核响应之上执行 Magic-API 工具。
 *
 * <p>aj-mcp 的 {@code toolCall} 以「反射 Method + paramsOrder 位置参数」执行工具，无法承载运行期
 * 动态的 Magic-API 资源，因此工具以占位方法注册进 toolStore（保证 {@code tools/list} 与能力声明
 * 正常），真正的执行与原始 arguments 解析放在这里：命中 Magic-API 快照时改写响应，未命中时透传
 * 内核响应。{@code prompts/get}、{@code resources/read} 等能力按同一模式在后续任务接入。</p>
 */
public class MagicApiMcpServerRegistry extends McpServerRegistry {

	private static final String TOOLS_CALL = "tools/call";

	private final MagicApiMcpToolInvoker toolInvoker;

	public MagicApiMcpServerRegistry(MagicApiMcpToolInvoker toolInvoker) {
		this.toolInvoker = toolInvoker;
	}

	@Override
	public McpResponse process(McpResponse response, McpRequestRawInfo requestRaw) {
		if (toolInvoker == null || requestRaw == null || !TOOLS_CALL.equals(requestRaw.getMethod())) {
			return response;
		}
		CallToolRequestParams params = params(requestRaw.getJsonNode());
		if (params == null) {
			return response;
		}
		StructuredToolResult result = toolInvoker.invoke(params.getName(),
				params.getArguments() == null ? Collections.emptyMap() : params.getArguments());
		if (result == null) {
			return response;
		}
		return callToolResult(requestRaw.getId(), result);
	}

	private CallToolRequestParams params(JsonNode node) {
		if (node == null) {
			return null;
		}
		JsonNode params = node.get("params");
		return params == null ? null : JsonUtils.jsonNode2bean(params, CallToolRequestParams.class);
	}

	private McpResponse callToolResult(Object id, StructuredToolResult result) {
		CallToolResultDetail detail = new CallToolResultDetail();
		detail.setContent(result.getContent());
		detail.setStructuredContent(result.getStructuredContent());
		detail.setIsError(result.isError());
		CallToolResult callToolResult = new CallToolResult();
		callToolResult.setId(id);
		callToolResult.setResult(detail);
		return callToolResult;
	}
}
