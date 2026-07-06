package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

public class MagicMcpDiagnosticController {

	private final MagicAPIMcpProperties properties;

	private final McpHttpTransportService transportService;

	public MagicMcpDiagnosticController(MagicAPIMcpProperties properties, McpHttpTransportService transportService) {
		this.properties = properties;
		this.transportService = transportService;
	}

	@PostMapping("${magic-api.plugin.mcp.endpoint:/mcp}/diagnostics/execute")
	@ResponseBody
	public ResponseEntity<?> execute(@RequestBody Object body, HttpServletRequest request) {
		return execute(header(request, properties.getHeaders().getAccept()),
				header(request, properties.getHeaders().getProtocolVersion()),
				header(request, properties.getHeaders().getSessionId()), body, request);
	}

	public ResponseEntity<?> execute(String accept, String protocolVersion, String sessionId, Object body,
								 HttpServletRequest request) {
		return transportService.post(accept, protocolVersion, sessionId, null, body, request);
	}

	private String header(HttpServletRequest request, String name) {
		return request == null || name == null ? null : request.getHeader(name);
	}
}
