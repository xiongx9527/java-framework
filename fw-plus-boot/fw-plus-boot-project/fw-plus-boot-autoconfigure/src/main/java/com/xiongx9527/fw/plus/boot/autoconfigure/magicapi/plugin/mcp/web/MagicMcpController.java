package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

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
	public ResponseEntity<?> post(@RequestHeader(value = "Accept", required = false) String accept,
								  @RequestHeader(value = "MCP-Protocol-Version", required = false) String protocolVersion,
								  @RequestHeader(value = "Mcp-Session-Id", required = false) String sessionId,
								  @RequestHeader(value = "Origin", required = false) String origin,
								  @RequestBody Map<String, Object> body,
								  HttpServletRequest request) {
		return transportService.post(accept, protocolVersion, sessionId, origin, body, request);
	}

	@GetMapping("${magic-api.plugin.mcp.endpoint:/mcp}")
	@ResponseBody
	public ResponseEntity<?> get(@RequestHeader(value = "Accept", required = false) String accept,
								 @RequestHeader(value = "Mcp-Session-Id", required = false) String sessionId,
								 HttpServletRequest request) {
		return sseStreamService.open(accept, sessionId, request);
	}

	@DeleteMapping("${magic-api.plugin.mcp.endpoint:/mcp}")
	@ResponseBody
	public ResponseEntity<?> delete(@RequestHeader(value = "Mcp-Session-Id", required = false) String sessionId,
									HttpServletRequest request) {
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
}
