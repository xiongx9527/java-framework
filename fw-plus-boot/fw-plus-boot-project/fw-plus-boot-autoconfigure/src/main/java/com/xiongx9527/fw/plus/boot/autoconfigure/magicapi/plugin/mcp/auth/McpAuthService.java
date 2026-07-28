package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth;

import javax.servlet.http.HttpServletRequest;

public interface McpAuthService {

	McpPrincipal authenticate(HttpServletRequest request);

	default McpPrincipal authenticate(String authorization) {
		return authenticate((HttpServletRequest) null);
	}
}
