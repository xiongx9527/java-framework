package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpAuthorizationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpPrincipal;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core.McpRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.protocol.McpJsonRpcResponse;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionNotFoundException;
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

	private final McpSseStreamService sseStreamService;

	public McpHttpTransportService(MagicAPIMcpProperties properties, McpJsonRpcDispatcher dispatcher,
								   McpSessionService sessionService) {
		this(properties, dispatcher, sessionService, null);
	}

	public McpHttpTransportService(MagicAPIMcpProperties properties, McpJsonRpcDispatcher dispatcher,
								   McpSessionService sessionService, McpAuthService authService) {
		this(properties, dispatcher, sessionService, authService, null);
	}

	public McpHttpTransportService(MagicAPIMcpProperties properties, McpJsonRpcDispatcher dispatcher,
								   McpSessionService sessionService, McpAuthService authService,
								   McpSseStreamService sseStreamService) {
		this.properties = properties;
		this.dispatcher = dispatcher;
		this.sessionService = sessionService;
		this.authService = authService;
		this.sseStreamService = sseStreamService;
	}

	public ResponseEntity<?> post(String accept, String protocolVersion, String sessionId, Object body) {
		return post(accept, protocolVersion, sessionId, null, body);
	}

	public ResponseEntity<?> post(String accept, String protocolVersion, String sessionId, String origin, Object body) {
		return post(accept, protocolVersion, sessionId, origin, body, null);
	}

	public ResponseEntity<?> post(String accept, String protocolVersion, String sessionId, String origin,
								  Object body, HttpServletRequest request) {
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
		ResponseEntity<?> sessionFailure = validatePostSession(body, sessionId);
		if (sessionFailure != null) {
			return sessionFailure;
		}
		Object response = dispatcher.dispatchMessage(context, body);
		if (response == null) {
			return ResponseEntity.accepted().build();
		}
		ResponseEntity.BodyBuilder builder = ResponseEntity.ok();
		if (context.getSessionId() != null) {
			builder.header(properties.getHeaders().getSessionId(), context.getSessionId());
		}
		builder.header(properties.getHeaders().getProtocolVersion(), protocolVersion(protocolVersion));
		return builder.body(response);
	}

	@SuppressWarnings("unchecked")
	private ResponseEntity<?> validatePostSession(Object body, String sessionId) {
		if (!(body instanceof Map)) {
			return null;
		}
		Object method = ((Map<String, Object>) body).get("method");
		if ("initialize".equals(method) || sessionId == null) {
			return null;
		}
		try {
			sessionService.requireSession(sessionId);
			return null;
		} catch (McpSessionNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
	}

	private String protocolVersion(String requestedProtocolVersion) {
		return requestedProtocolVersion == null ? properties.getProtocol().getDefaultVersion() : requestedProtocolVersion;
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
			if (sseStreamService != null) {
				sseStreamService.remove(sessionId);
			}
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
