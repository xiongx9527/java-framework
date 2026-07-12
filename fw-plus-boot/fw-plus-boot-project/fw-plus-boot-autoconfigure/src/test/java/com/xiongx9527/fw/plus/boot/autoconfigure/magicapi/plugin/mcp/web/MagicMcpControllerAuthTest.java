package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpAuthorizationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpPrincipal;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcResponse;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MagicMcpControllerAuthTest {

	@Test
	void shouldReturnUnauthorizedWhenAuthFails() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		McpAuthService authService = request -> {
			throw new McpAuthorizationException(401, "Missing token");
		};
		McpHttpTransportService transportService = new McpHttpTransportService(properties,
				new McpJsonRpcDispatcher(Collections.emptyMap()), new McpSessionService(1800), authService);
		MagicMcpController controller = new MagicMcpController(properties, transportService, new McpSseStreamService(properties));

		ResponseEntity<?> response = controller.post("application/json", null, null, null, request("ping"), null);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getHeaders().getFirst("WWW-Authenticate")).contains("Bearer");
	}

	@Test
	void shouldRejectUntrustedOriginBeforeDispatch() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		properties.getTrustedOrigins().add("https://trusted.example");
		McpHttpTransportService transportService = new McpHttpTransportService(properties,
				new McpJsonRpcDispatcher(Collections.emptyMap()), new McpSessionService(1800),
				request -> null);

		ResponseEntity<?> response = transportService.post("application/json", null, null,
				"https://evil.example", request("ping"));

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void shouldPassHttpRequestToAuthAndPrincipalToDispatcher() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		AtomicReference<String> authorization = new AtomicReference<>();
		McpAuthService authService = request -> {
			authorization.set(request.getHeader("Authorization"));
			return new McpPrincipal("user-1", Collections.singleton("mcp:tools"));
		};
		AtomicReference<McpPrincipal> principal = new AtomicReference<>();
		McpJsonRpcDispatcher dispatcher = new McpJsonRpcDispatcher(Collections.singletonMap("ping", (context, params) -> {
			principal.set(context.getPrincipal());
			return Collections.emptyMap();
		}));
		McpHttpTransportService transportService = new McpHttpTransportService(properties, dispatcher,
				new McpSessionService(1800), authService);
		MagicMcpController controller = new MagicMcpController(properties, transportService, new McpSseStreamService(properties));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader("Authorization", "Bearer token-1");

		ResponseEntity<?> response = controller.post("application/json", null, null, null, request("ping"), request);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(authorization.get()).isEqualTo("Bearer token-1");
		assertThat(principal.get()).extracting(McpPrincipal::getSubject).isEqualTo("user-1");
		assertThat(response.getBody()).isInstanceOf(McpJsonRpcResponse.class);
	}

	@Test
	void shouldAuthenticateSseStreamOpen() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		McpAuthService authService = request -> {
			throw new McpAuthorizationException(401, "Missing token");
		};
		MagicMcpController controller = new MagicMcpController(properties,
				new McpHttpTransportService(properties, new McpJsonRpcDispatcher(Collections.emptyMap()),
						new McpSessionService(1800), authService),
				new McpSseStreamService(properties, authService));

		ResponseEntity<?> response = controller.get("text/event-stream", "session-1", new MockHttpServletRequest());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void shouldAuthenticateSessionDelete() {
		MagicAPIMcpProperties properties = new MagicAPIMcpProperties();
		McpAuthService authService = request -> {
			throw new McpAuthorizationException(401, "Missing token");
		};
		MagicMcpController controller = new MagicMcpController(properties,
				new McpHttpTransportService(properties, new McpJsonRpcDispatcher(Collections.emptyMap()),
						new McpSessionService(1800), authService),
				new McpSseStreamService(properties, authService));

		ResponseEntity<?> response = controller.delete("session-1", new MockHttpServletRequest());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	private Map<String, Object> request(String method) {
		Map<String, Object> request = new HashMap<>();
		request.put("jsonrpc", "2.0");
		request.put("id", 1);
		request.put("method", method);
		return request;
	}
}
