package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;

public class MagicMcpController {

	private final MagicAPIMcpProperties properties;

	private final McpHttpTransportService transportService;

	private final McpSseStreamService sseStreamService;

	public MagicMcpController(MagicAPIMcpProperties properties, McpHttpTransportService transportService,
							  McpSseStreamService sseStreamService) {
		this.properties = properties;
		this.transportService = transportService;
		this.sseStreamService = sseStreamService;
	}

	@PostMapping("${magic-api.plugin.mcp.endpoint:/mcp}")
	@ResponseBody
	public ResponseEntity<?> post(@RequestBody Object body, HttpServletRequest request) {
		return post(header(request, properties.getHeaders().getAccept()),
				header(request, properties.getHeaders().getProtocolVersion()),
				header(request, properties.getHeaders().getSessionId()),
				header(request, properties.getHeaders().getOrigin()),
				body, request);
	}

	public ResponseEntity<?> post(String accept, String protocolVersion, String sessionId, String origin,
							  Object body, HttpServletRequest request) {
		return transportService.post(accept, protocolVersion, sessionId, origin, body, request);
	}

	@GetMapping("${magic-api.plugin.mcp.endpoint:/mcp}")
	@ResponseBody
	public ResponseEntity<?> get(HttpServletRequest request) {
		return get(header(request, properties.getHeaders().getAccept()),
				header(request, properties.getHeaders().getSessionId()), request);
	}

	public ResponseEntity<?> get(String accept, String sessionId, HttpServletRequest request) {
		return sseStreamService.open(accept, sessionId, request);
	}

	@DeleteMapping("${magic-api.plugin.mcp.endpoint:/mcp}")
	@ResponseBody
	public ResponseEntity<?> delete(HttpServletRequest request) {
		return delete(header(request, properties.getHeaders().getSessionId()), request);
	}

	public ResponseEntity<?> delete(String sessionId, HttpServletRequest request) {
		return transportService.delete(sessionId, request);
	}

	@GetMapping("${magic-api.plugin.mcp.endpoint:/mcp}/.well-known/oauth-protected-resource")
	@ResponseBody
	public ResponseEntity<?> protectedResourceMetadata() {
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(transportService.protectedResourceMetadata());
	}

	public MagicAPIMcpProperties getProperties() {
		return properties;
	}

	private String header(HttpServletRequest request, String name) {
		return request == null || name == null ? null : request.getHeader(name);
	}
}
