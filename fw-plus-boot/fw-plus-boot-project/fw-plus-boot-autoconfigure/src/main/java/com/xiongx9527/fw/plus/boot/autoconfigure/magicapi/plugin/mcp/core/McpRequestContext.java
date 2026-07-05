package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.core;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth.McpPrincipal;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class McpRequestContext {

	private McpPrincipal principal;

	private String sessionId;

	public McpRequestContext(McpPrincipal principal) {
		this.principal = principal;
	}
}
