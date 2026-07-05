package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpAuthorizationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;

public class McpSseStreamService {

	private final MagicAPIMcpProperties properties;

	private final McpAuthService authService;

	public McpSseStreamService(MagicAPIMcpProperties properties) {
		this(properties, null);
	}

	public McpSseStreamService(MagicAPIMcpProperties properties, McpAuthService authService) {
		this.properties = properties;
		this.authService = authService;
	}

	public ResponseEntity<?> open(String accept, String sessionId) {
		return open(accept, sessionId, null);
	}

	public ResponseEntity<?> open(String accept, String sessionId, HttpServletRequest request) {
		ResponseEntity<?> authFailure = authenticate(request);
		if (authFailure != null) {
			return authFailure;
		}
		if (!properties.isSseStreamEnabled()) {
			return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
		}
		if (accept == null || !accept.contains("text/event-stream")) {
			return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
		}
		return ResponseEntity.ok(new SseEmitter());
	}

	private ResponseEntity<?> authenticate(HttpServletRequest request) {
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
