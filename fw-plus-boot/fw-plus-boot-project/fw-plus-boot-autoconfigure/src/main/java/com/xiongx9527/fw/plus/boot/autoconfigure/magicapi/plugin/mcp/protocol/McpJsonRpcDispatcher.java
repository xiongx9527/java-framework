package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;

import java.util.Collections;
import java.util.Map;

public class McpJsonRpcDispatcher {

	private final Map<String, McpMethodHandler> handlers;

	public McpJsonRpcDispatcher(Map<String, McpMethodHandler> handlers) {
		this.handlers = handlers == null ? Collections.emptyMap() : handlers;
	}

	public McpJsonRpcResponse dispatch(McpRequestContext context, Map<String, Object> message) {
		Object id = message.get("id");
		boolean notification = !message.containsKey("id");
		Object methodValue = message.get("method");
		if (!(methodValue instanceof String)) {
			return notification ? null : McpJsonRpcResponse.error(id,
					new McpJsonRpcError(McpJsonRpcError.INVALID_REQUEST, "Invalid Request", null));
		}
		String method = (String) methodValue;
		McpMethodHandler handler = handlers.get(method);
		if (handler == null) {
			return notification ? null : McpJsonRpcResponse.error(id,
					new McpJsonRpcError(McpJsonRpcError.METHOD_NOT_FOUND, "Method not found", method));
		}
		try {
			Object result = handler.handle(context, message.get("params"));
			return notification ? null : McpJsonRpcResponse.success(id, result);
		} catch (McpJsonRpcException e) {
			return notification ? null : McpJsonRpcResponse.error(id,
					new McpJsonRpcError(e.getCode(), e.getMessage(), e.getData()));
		} catch (Exception e) {
			return notification ? null : McpJsonRpcResponse.error(id,
					new McpJsonRpcError(McpJsonRpcError.INTERNAL_ERROR, "Internal error", null));
		}
	}
}
