package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpAuthService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpAuthorizationException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSession;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionNotFoundException;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.session.McpSessionState;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class McpSseStreamService {

	private final MagicAPIMcpProperties properties;

	private final McpAuthService authService;

	private final EmitterFactory emitterFactory;

	private final McpSessionService sessionService;

	private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

	public McpSseStreamService(MagicAPIMcpProperties properties) {
		this(properties, null);
	}

	public McpSseStreamService(MagicAPIMcpProperties properties, McpAuthService authService) {
		this(properties, authService, SseEmitter::new);
	}

	public McpSseStreamService(MagicAPIMcpProperties properties, McpAuthService authService,
								   EmitterFactory emitterFactory) {
		this(properties, authService, null, emitterFactory);
	}

	public McpSseStreamService(MagicAPIMcpProperties properties, McpAuthService authService,
								   McpSessionService sessionService) {
		this(properties, authService, sessionService, null);
	}

	public McpSseStreamService(MagicAPIMcpProperties properties, McpAuthService authService,
								   McpSessionService sessionService, EmitterFactory emitterFactory) {
		this.properties = properties;
		this.authService = authService;
		this.sessionService = sessionService;
		this.emitterFactory = emitterFactory == null ? SseEmitter::new : emitterFactory;
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
		ResponseEntity<?> sessionFailure = requireReadySession(sessionId);
		if (sessionFailure != null) {
			return sessionFailure;
		}
		SseEmitter emitter = emitterFactory.create();
		if (sessionId != null) {
			emitters.put(sessionId, emitter);
			emitter.onCompletion(() -> remove(sessionId));
			emitter.onTimeout(() -> remove(sessionId));
			emitter.onError(error -> remove(sessionId));
		}
		return ResponseEntity.ok(emitter);
	}

	public boolean send(String sessionId, Map<String, Object> message) {
		SseEmitter emitter = emitters.get(sessionId);
		if (emitter == null) {
			return false;
		}
		try {
			emitter.send(message);
			return true;
		} catch (IOException | IllegalStateException e) {
			remove(sessionId);
			return false;
		}
	}

	public void broadcast(Map<String, Object> message) {
		for (String sessionId : emitters.keySet()) {
			send(sessionId, message);
		}
	}

	public void remove(String sessionId) {
		if (sessionId != null) {
			emitters.remove(sessionId);
		}
	}

	public boolean hasEmitter(String sessionId) {
		return sessionId != null && emitters.containsKey(sessionId);
	}

	private ResponseEntity<?> requireReadySession(String sessionId) {
		if (sessionService == null) {
			return null;
		}
		try {
			McpSession session = sessionService.requireSession(sessionId);
			if (session.getState() != McpSessionState.READY) {
				return ResponseEntity.status(HttpStatus.CONFLICT).build();
			}
			return null;
		} catch (McpSessionNotFoundException e) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
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

	public interface EmitterFactory {

		SseEmitter create();
	}
}
