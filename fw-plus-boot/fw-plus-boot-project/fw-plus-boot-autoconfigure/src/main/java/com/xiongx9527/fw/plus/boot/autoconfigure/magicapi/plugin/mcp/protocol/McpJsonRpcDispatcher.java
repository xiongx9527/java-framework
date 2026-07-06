package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.client.McpClientRequestService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpLifecycleHandlers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class McpJsonRpcDispatcher {

	private final Map<String, McpMethodHandler> handlers;

	private final McpLifecycleHandlers lifecycleHandlers;

	private final McpClientRequestService clientRequestService;

	public McpJsonRpcDispatcher(Map<String, McpMethodHandler> handlers) {
		this(handlers, null);
	}

	public McpJsonRpcDispatcher(Map<String, McpMethodHandler> handlers, McpLifecycleHandlers lifecycleHandlers) {
		this(handlers, lifecycleHandlers, null);
	}

	public McpJsonRpcDispatcher(Map<String, McpMethodHandler> handlers, McpLifecycleHandlers lifecycleHandlers,
							 McpClientRequestService clientRequestService) {
		this.handlers = handlers == null ? Collections.emptyMap() : handlers;
		this.lifecycleHandlers = lifecycleHandlers;
		this.clientRequestService = clientRequestService;
	}

	@SuppressWarnings("unchecked")
	public Object dispatchMessage(McpRequestContext context, Object message) {
		if (message instanceof Map) {
			return dispatch(context, (Map<String, Object>) message);
		}
		if (message instanceof List) {
			List<Object> responses = new ArrayList<>();
			for (Object item : (List<Object>) message) {
				Object response = dispatchMessage(context, item);
				if (response instanceof List) {
					responses.addAll((List<Object>) response);
				} else if (response != null) {
					responses.add(response);
				}
			}
			return responses.isEmpty() ? null : responses;
		}
		return McpJsonRpcResponse.error(null,
				new McpJsonRpcError(McpJsonRpcError.INVALID_REQUEST, "Invalid Request", null));
	}

	public McpJsonRpcResponse dispatch(McpRequestContext context, Map<String, Object> message) {
		if (message == null) {
			return McpJsonRpcResponse.error(null,
					new McpJsonRpcError(McpJsonRpcError.INVALID_REQUEST, "Invalid Request", null));
		}
		Object id = message.get("id");
		boolean notification = !message.containsKey("id");
		if (!"2.0".equals(message.get("jsonrpc"))) {
			return notification ? null : McpJsonRpcResponse.error(id,
					new McpJsonRpcError(McpJsonRpcError.INVALID_REQUEST, "Invalid Request", null));
		}
		if (!message.containsKey("method") && id != null && (message.containsKey("result") || message.containsKey("error"))) {
			if (clientRequestService != null) {
				clientRequestService.resolve(context == null ? null : context.getSessionId(), id, message.get("result"), message.get("error"));
			}
			return null;
		}
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
			requireLifecycleReady(context, method);
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

	private void requireLifecycleReady(McpRequestContext context, String method) {
		if (lifecycleHandlers != null && !isLifecycleMethod(method)) {
			lifecycleHandlers.requireReady(context);
		}
	}

	private boolean isLifecycleMethod(String method) {
		return "initialize".equals(method)
				|| "notifications/initialized".equals(method)
				|| "notifications/cancelled".equals(method)
				|| "notifications/progress".equals(method)
				|| "ping".equals(method);
	}
}
