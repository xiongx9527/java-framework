package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web.McpHttpTransportService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.web.McpSseStreamService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ServerWebExchange;

public class MagicMcpWebFluxController {

	private final MagicAPIMcpProperties properties;

	private final McpHttpTransportService transportService;

	private final McpSseStreamService sseStreamService;

	public MagicMcpWebFluxController(MagicAPIMcpProperties properties,
									 McpHttpTransportService transportService,
									 McpSseStreamService sseStreamService) {
		this.properties = properties;
		this.transportService = transportService;
		this.sseStreamService = sseStreamService;
	}

	@PostMapping("${magic-api.plugin.mcp.endpoint:/mcp}")
	@ResponseBody
	public ResponseEntity<?> post(@RequestBody Object body, ServerWebExchange exchange) {
		return transportService.post(header(exchange, properties.getHeaders().getAccept()),
				header(exchange, properties.getHeaders().getProtocolVersion()),
				header(exchange, properties.getHeaders().getSessionId()),
				header(exchange, properties.getHeaders().getOrigin()),
				body, authorization(exchange));
	}

	@GetMapping("${magic-api.plugin.mcp.endpoint:/mcp}")
	@ResponseBody
	public ResponseEntity<?> get(ServerWebExchange exchange) {
		return sseStreamService.open(header(exchange, properties.getHeaders().getAccept()),
				header(exchange, properties.getHeaders().getSessionId()), authorization(exchange));
	}

	@DeleteMapping("${magic-api.plugin.mcp.endpoint:/mcp}")
	@ResponseBody
	public ResponseEntity<?> delete(ServerWebExchange exchange) {
		return transportService.delete(header(exchange, properties.getHeaders().getSessionId()),
				authorization(exchange));
	}

	@GetMapping("${magic-api.plugin.mcp.endpoint:/mcp}/.well-known/oauth-protected-resource")
	@ResponseBody
	public ResponseEntity<?> protectedResourceMetadata() {
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_JSON)
				.body(transportService.protectedResourceMetadata());
	}

	private String authorization(ServerWebExchange exchange) {
		return header(exchange, "Authorization");
	}

	private String header(ServerWebExchange exchange, String name) {
		return exchange == null || name == null ? null : exchange.getRequest().getHeaders().getFirst(name);
	}
}
