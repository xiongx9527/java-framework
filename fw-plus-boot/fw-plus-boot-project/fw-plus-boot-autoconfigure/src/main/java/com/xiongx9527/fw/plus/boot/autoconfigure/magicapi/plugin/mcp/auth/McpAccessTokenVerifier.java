package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth;

public interface McpAccessTokenVerifier {

	McpPrincipal verify(String token);
}
