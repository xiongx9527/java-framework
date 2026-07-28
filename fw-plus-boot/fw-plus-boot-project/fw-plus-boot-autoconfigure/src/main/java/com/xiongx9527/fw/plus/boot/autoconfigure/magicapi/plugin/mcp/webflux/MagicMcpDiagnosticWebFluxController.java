package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web.McpHttpTransportService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ServerWebExchange;

public class MagicMcpDiagnosticWebFluxController {

	private final MagicAPIMcpProperties properties;

	private final McpHttpTransportService transportService;

	public MagicMcpDiagnosticWebFluxController(MagicAPIMcpProperties properties,
											   McpHttpTransportService transportService) {
		this.properties = properties;
		this.transportService = transportService;
	}

	@PostMapping("${magic-api.plugin.mcp.endpoint:/mcp}/diagnostics/execute")
	@ResponseBody
	public ResponseEntity<?> execute(@RequestBody Object body, ServerWebExchange exchange) {
		return transportService.post(header(exchange, properties.getHeaders().getAccept()),
				header(exchange, properties.getHeaders().getProtocolVersion()),
				header(exchange, properties.getHeaders().getSessionId()), null, body,
				header(exchange, "Authorization"));
	}

	private String header(ServerWebExchange exchange, String name) {
		return exchange == null || name == null ? null : exchange.getRequest().getHeaders().getFirst(name);
	}
}
