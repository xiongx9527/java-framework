package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.completion;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcError;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.provider.*;

import java.util.*;

public class McpCompletionHandler {

	private final List<McpCompletionProvider> providers;

	public McpCompletionHandler(List<McpCompletionProvider> providers) {
		this.providers = providers == null ? Collections.emptyList() : providers;
	}

	public Map<String, Object> complete(McpRequestContext context, Map<String, Object> params) {
		McpCompletionRequest request = request(params);
		for (McpCompletionProvider provider : providers) {
			McpCompletionResult result = provider.complete(new McpProviderContext(context), request);
			if (result != null) {
				Map<String, Object> completion = new HashMap<>();
				completion.put("values", result.getValues() == null ? Collections.emptyList() : result.getValues());
				completion.put("hasMore", result.isHasMore());
				Map<String, Object> response = new HashMap<>();
				response.put("completion", completion);
				return response;
			}
		}
		throw new McpJsonRpcException(McpJsonRpcError.INVALID_PARAMS, "Completion target not found");
	}

	@SuppressWarnings("unchecked")
	private McpCompletionRequest request(Map<String, Object> params) {
		McpCompletionRequest request = new McpCompletionRequest();
		if (params != null) {
			Object ref = params.get("ref");
			Object argument = params.get("argument");
			if (ref instanceof Map) {
				request.setRef((Map<String, Object>) ref);
			}
			if (argument instanceof Map) {
				request.setArgument((Map<String, Object>) argument);
			}
		}
		return request;
	}
}
