package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpAuthorizationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpPrincipal;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcResponse;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class McpHttpTransportService {

	private final MagicAPIMcpProperties properties;

	private final McpJsonRpcDispatcher dispatcher;

	private final McpSessionService sessionService;

	private final McpAuthService authService;

	public McpHttpTransportService(MagicAPIMcpProperties properties, McpJsonRpcDispatcher dispatcher,
								   McpSessionService sessionService) {
		this(properties, dispatcher, sessionService, null);
	}

	public McpHttpTransportService(MagicAPIMcpProperties properties, McpJsonRpcDispatcher dispatcher,
								   McpSessionService sessionService, McpAuthService authService) {
		this.properties = properties;
		this.dispatcher = dispatcher;
		this.sessionService = sessionService;
		this.authService = authService;
	}

	public ResponseEntity<?> post(String accept, String protocolVersion, String sessionId, Map<String, Object> body) {
		return post(accept, protocolVersion, sessionId, null, body);
	}

	public ResponseEntity<?> post(String accept, String protocolVersion, String sessionId, String origin, Map<String, Object> body) {
		return post(accept, protocolVersion, sessionId, origin, body, null);
	}

	public ResponseEntity<?> post(String accept, String protocolVersion, String sessionId, String origin,
								  Map<String, Object> body, HttpServletRequest request) {
		if (!isTrustedOrigin(origin)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		if (body != null && String.valueOf(body).length() > properties.getMaxRequestBytes()) {
			return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
		}
		McpPrincipal principal = null;
		if (authService != null) {
			try {
				principal = authService.authenticate(request);
			} catch (McpAuthorizationException e) {
				return ResponseEntity.status(e.getStatus())
						.header("WWW-Authenticate", "Bearer resource=\"" + properties.getAuth().getResource() + "\"")
						.build();
			}
		}
		if (!acceptsMcpResponse(accept)) {
			return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
		}
		McpRequestContext context = new McpRequestContext();
		context.setPrincipal(principal);
		context.setSessionId(sessionId);
		McpJsonRpcResponse response = dispatcher.dispatch(context, body);
		if (response == null) {
			return ResponseEntity.accepted().build();
		}
		return ResponseEntity.ok(response);
	}

	public ResponseEntity<?> delete(String sessionId) {
		return delete(sessionId, null);
	}

	public ResponseEntity<?> delete(String sessionId, HttpServletRequest request) {
		ResponseEntity<?> authFailure = authenticateOnly(request);
		if (authFailure != null) {
			return authFailure;
		}
		if (sessionId != null) {
			sessionService.delete(sessionId);
		}
		return ResponseEntity.accepted().build();
	}

	public Map<String, Object> protectedResourceMetadata() {
		java.util.Map<String, Object> metadata = new java.util.HashMap<>();
		metadata.put("resource", properties.getAuth().getResource());
		metadata.put("authorization_servers", java.util.Collections.singletonList(properties.getAuth().getAuthorizationServer()));
		return metadata;
	}

	private boolean acceptsMcpResponse(String accept) {
		if (accept == null) {
			return false;
		}
		return accept.contains("application/json") || accept.contains("text/event-stream") || accept.contains("*/*");
	}

	private boolean isTrustedOrigin(String origin) {
		return origin == null || properties.getTrustedOrigins().isEmpty() || properties.getTrustedOrigins().contains(origin);
	}

	ResponseEntity<?> authenticateOnly(HttpServletRequest request) {
		if (authService == null) {
			return null;
		}
		try {
			authService.authenticate(request);
			return null;
		} catch (McpAuthorizationException e) {
			return ResponseEntity.status(e.getStatus())
					.header("WWW-Authenticate", "Bearer resource=\"" + properties.getAuth().getResource() + "\"")
					.build();
		}
	}
}
