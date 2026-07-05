package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class McpJsonRpcDispatcherTest {

	@Test
	void shouldReturnMethodNotFoundForUnknownMethod() {
		McpJsonRpcDispatcher dispatcher = new McpJsonRpcDispatcher(Collections.emptyMap());
		Map<String, Object> request = request(1, "tools/list", null);

		McpJsonRpcResponse response = dispatcher.dispatch(new McpRequestContext(), request);

		assertThat(response.getId()).isEqualTo(1);
		assertThat(response.getError()).isNotNull();
		assertThat(response.getError().getCode()).isEqualTo(-32601);
	}

	@Test
	void shouldReturnInvalidParamsWhenHandlerRejectsParameters() {
		Map<String, McpMethodHandler> handlers = new HashMap<>();
		handlers.put("tools/call", (context, params) -> {
			throw new McpJsonRpcException(-32602, "Invalid params");
		});
		McpJsonRpcDispatcher dispatcher = new McpJsonRpcDispatcher(handlers);
		Map<String, Object> request = request("call-1", "tools/call", Collections.emptyMap());

		McpJsonRpcResponse response = dispatcher.dispatch(new McpRequestContext(), request);

		assertThat(response.getId()).isEqualTo("call-1");
		assertThat(response.getError()).isNotNull();
		assertThat(response.getError().getCode()).isEqualTo(-32602);
	}

	@Test
	void shouldReturnNullForNotifications() {
		Map<String, McpMethodHandler> handlers = new HashMap<>();
		handlers.put("notifications/initialized", (context, params) -> Collections.emptyMap());
		McpJsonRpcDispatcher dispatcher = new McpJsonRpcDispatcher(handlers);
		Map<String, Object> notification = request(null, "notifications/initialized", null);
		notification.remove("id");

		McpJsonRpcResponse response = dispatcher.dispatch(new McpRequestContext(), notification);

		assertThat(response).isNull();
	}

	@Test
	void shouldDispatchPingWithEmptyResult() {
		Map<String, McpMethodHandler> handlers = new HashMap<>();
		handlers.put("ping", (context, params) -> Collections.emptyMap());
		McpJsonRpcDispatcher dispatcher = new McpJsonRpcDispatcher(handlers);
		Map<String, Object> request = request(2, "ping", null);

		McpJsonRpcResponse response = dispatcher.dispatch(new McpRequestContext(), request);

		assertThat(response.getId()).isEqualTo(2);
		assertThat(response.getError()).isNull();
		assertThat(response.getResult()).isEqualTo(Collections.emptyMap());
	}

	private Map<String, Object> request(Object id, String method, Object params) {
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", id);
		request.put("method", method);
		if (params != null) {
			request.put("params", params);
		}
		return request;
	}
}
