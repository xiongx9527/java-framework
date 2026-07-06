package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcResponse;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MagicMcpDiagnosticControllerTest {

	@Test
	void shouldExecuteDiagnosticMessageThroughTransport() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		McpSessionService sessionService = new McpSessionService(1800);
		MagicMcpDiagnosticController controller = new MagicMcpDiagnosticController(properties,
				new McpHttpTransportService(properties,
						new McpJsonRpcDispatcher(Collections.singletonMap("tools/list", (context, params) -> Collections.singletonMap("tools", Collections.emptyList()))),
						sessionService));

		ResponseEntity<?> response = controller.execute("application/json", null, null, request("tools/list"), null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isInstanceOf(McpJsonRpcResponse.class);
		assertThat(((McpJsonRpcResponse) response.getBody()).getResult()).isEqualTo(Collections.singletonMap("tools", Collections.emptyList()));
	}

	private Map<String, Object> request(String method) {
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", "diagnostic-1");
		request.put("method", method);
		return request;
	}
}
