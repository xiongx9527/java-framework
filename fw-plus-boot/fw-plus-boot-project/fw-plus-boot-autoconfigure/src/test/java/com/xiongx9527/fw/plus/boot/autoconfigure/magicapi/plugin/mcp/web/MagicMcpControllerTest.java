package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcResponse;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MagicMcpControllerTest {

	@Test
	void shouldRejectPostWithoutAcceptableMediaType() {
		MagicMcpController controller = controller(new MagicAPIMcpProperties(), pingDispatcher());

		ResponseEntity<?> response = controller.post(null, null, null, null, request("ping"), null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_ACCEPTABLE);
	}

	@Test
	void shouldDispatchPostRequest() {
		MagicMcpController controller = controller(new MagicAPIMcpProperties(), pingDispatcher());

		ResponseEntity<?> response = controller.post("application/json, text/event-stream", null, null, null, request("ping"), null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isInstanceOf(McpJsonRpcResponse.class);
		assertThat(((McpJsonRpcResponse) response.getBody()).getResult()).isEqualTo(Collections.emptyMap());
	}

	@Test
	void shouldAcceptNotificationWithoutBody() {
		MagicMcpController controller = controller(new MagicAPIMcpProperties(),
				new McpJsonRpcDispatcher(Collections.singletonMap("notifications/initialized", (context, params) -> Collections.emptyMap())));
		Map<String, Object> notification = request("notifications/initialized");
		notification.remove("id");

		ResponseEntity<?> response = controller.post("application/json, text/event-stream", null, null, null, notification, null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
		assertThat(response.getBody()).isNull();
	}

	@Test
	void shouldOpenSseStreamWhenEnabled() {
		MagicMcpController controller = controller(new MagicAPIMcpProperties(), pingDispatcher());

		ResponseEntity<?> response = controller.get("text/event-stream", null, null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isInstanceOf(SseEmitter.class);
	}

	@Test
	void shouldReturnMethodNotAllowedWhenSseStreamDisabled() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		properties.setSseStreamEnabled(false);
		MagicMcpController controller = controller(properties, pingDispatcher());

		ResponseEntity<?> response = controller.get("text/event-stream", null, null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
	}

	@Test
	void shouldDeleteSession() {
		McpSessionService sessionService = new McpSessionService(1800);
		MagicMcpController controller = new MagicMcpController(new MagicAPIMcpProperties(),
				new McpHttpTransportService(new MagicAPIMcpProperties(), pingDispatcher(), sessionService),
				new McpSseStreamService(new MagicAPIMcpProperties()));

		ResponseEntity<?> response = controller.delete("session-1", null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
	}

	@Test
	void shouldReturnProtectedResourceMetadata() {
		MagicMcpController controller = controller(new MagicAPIMcpProperties(), pingDispatcher());

		ResponseEntity<?> response = controller.protectedResourceMetadata();

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
		assertThat(response.getBody()).isInstanceOf(Map.class);
	}

	@Test
	void shouldRejectOversizedRequestBody() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		properties.setMaxRequestBytes(8);
		MagicMcpController controller = controller(properties, pingDispatcher());

		ResponseEntity<?> response = controller.post("application/json", null, null, null, request("ping"), null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
	}

	private MagicMcpController controller(MagicAPIMcpProperties properties, McpJsonRpcDispatcher dispatcher) {
		McpSessionService sessionService = new McpSessionService(1800);
		return new MagicMcpController(properties,
				new McpHttpTransportService(properties, dispatcher, sessionService),
				new McpSseStreamService(properties));
	}

	private McpJsonRpcDispatcher pingDispatcher() {
		return new McpJsonRpcDispatcher(Collections.singletonMap("ping", (context, params) -> Collections.emptyMap()));
	}

	private Map<String, Object> request(String method) {
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", 1);
		request.put("method", method);
		return request;
	}
}
