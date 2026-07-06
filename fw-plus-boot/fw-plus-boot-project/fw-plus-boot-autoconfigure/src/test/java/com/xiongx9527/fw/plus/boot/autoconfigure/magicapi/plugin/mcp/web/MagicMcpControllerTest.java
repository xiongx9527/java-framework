package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcResponse;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSession;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionState;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
	void shouldReturnSessionHeaderAfterInitialize() {
		McpSessionService sessionService = new McpSessionService(1800);
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		MagicMcpController controller = new MagicMcpController(properties,
				new McpHttpTransportService(properties,
						new McpJsonRpcDispatcher(Collections.singletonMap("initialize", (context, params) -> {
							McpSession session = sessionService.createSession(null, Collections.emptyMap(), "2025-06-18");
							context.setSessionId(session.getId());
							return Collections.emptyMap();
						})), sessionService),
				new McpSseStreamService(properties, null, sessionService));

		ResponseEntity<?> response = controller.post("application/json", null, null, null, request("initialize"), null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getFirst(properties.getHeaders().getSessionId())).isNotBlank();
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
	void shouldDispatchBatchPostRequest() {
		MagicMcpController controller = controller(new MagicAPIMcpProperties(), pingDispatcher());

		ResponseEntity<?> response = controller.post("application/json", null, null, null,
				java.util.Arrays.asList(request("ping"), request("ping")), null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isInstanceOf(List.class);
		assertThat((List<?>) response.getBody()).hasSize(2);
	}

	@Test
	void shouldRejectFeatureRequestBeforeSessionReady() {
		McpSessionService sessionService = new McpSessionService(1800);
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		MagicMcpController controller = new MagicMcpController(properties,
				new McpHttpTransportService(properties,
						new McpJsonRpcDispatcher(Collections.singletonMap("tools/list", (context, params) -> Collections.emptyMap()),
								new com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpLifecycleHandlers(sessionService)),
						sessionService),
				new McpSseStreamService(properties, null, sessionService));

		ResponseEntity<?> response = controller.post("application/json", null, null, null, request("tools/list"), null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isInstanceOf(McpJsonRpcResponse.class);
		assertThat(((McpJsonRpcResponse) response.getBody()).getError().getCode()).isEqualTo(-32600);
	}

	@Test
	void shouldOpenSseStreamWhenEnabled() {
		McpSessionService sessionService = new McpSessionService(1800);
		McpSession session = sessionService.createSession(null, Collections.emptyMap(), "2025-06-18");
		session.setState(McpSessionState.READY);
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		MagicMcpController controller = new MagicMcpController(properties,
				new McpHttpTransportService(properties, pingDispatcher(), sessionService),
				new McpSseStreamService(properties, null, sessionService));

		ResponseEntity<?> response = controller.get("text/event-stream", session.getId(), null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isInstanceOf(SseEmitter.class);
	}

	@Test
	void shouldRejectSseStreamForUnknownSession() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		McpSessionService sessionService = new McpSessionService(1800);
		MagicMcpController controller = new MagicMcpController(properties,
				new McpHttpTransportService(properties, pingDispatcher(), sessionService),
				new McpSseStreamService(properties, null, sessionService));

		ResponseEntity<?> response = controller.get("text/event-stream", "missing-session", null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
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

	@Test
	void shouldReadConfiguredHeaderNamesFromRequest() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		properties.getHeaders().setAccept("X-Mcp-Accept");
		properties.getHeaders().setProtocolVersion("X-Mcp-Protocol");
		properties.getHeaders().setSessionId("X-Mcp-Session");
		properties.getHeaders().setOrigin("X-Mcp-Origin");
		MagicMcpController controller = controller(properties, pingDispatcher());
		MockHttpServletRequest servletRequest = new MockHttpServletRequest();
		servletRequest.addHeader("X-Mcp-Accept", "application/json");
		servletRequest.addHeader("X-Mcp-Protocol", "2025-06-18");
		servletRequest.addHeader("X-Mcp-Session", "session-1");
		servletRequest.addHeader("X-Mcp-Origin", "https://client.example");

		ResponseEntity<?> response = controller.post(request("ping"), servletRequest);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody()).isInstanceOf(McpJsonRpcResponse.class);
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
