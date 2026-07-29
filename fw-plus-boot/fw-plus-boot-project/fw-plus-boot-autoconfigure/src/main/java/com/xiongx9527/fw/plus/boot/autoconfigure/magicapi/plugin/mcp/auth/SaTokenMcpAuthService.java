package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter.MagicAPIMcpProperties;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

public class SaTokenMcpAuthService implements McpAuthService {

	private final MagicAPIMcpProperties properties;

	private final McpAccessTokenVerifier accessTokenVerifier;

	public SaTokenMcpAuthService(MagicAPIMcpProperties properties, McpAccessTokenVerifier accessTokenVerifier) {
		this.properties = properties;
		this.accessTokenVerifier = accessTokenVerifier;
	}

	@Override
	public McpPrincipal authenticate(HttpServletRequest request) {
		return authenticate(request == null ? null : request.getHeader("Authorization"));
	}

	@Override
	public McpPrincipal authenticate(String authorization) {
		if (!properties.getAuth().isEnabled()) {
			return new McpPrincipal("anonymous", Collections.emptySet());
		}
		if (authorization == null || !authorization.startsWith("Bearer ")) {
			throw new McpAuthorizationException(401, "Missing bearer token");
		}
		return verifyToken(authorization.substring("Bearer ".length()).trim());
	}

	public McpPrincipal verifyToken(String token) {
		McpPrincipal principal;
		try {
			principal = accessTokenVerifier.verify(token);
		} catch (RuntimeException e) {
			throw new McpAuthorizationException(401, "Invalid bearer token");
		}
		if (principal == null) {
			throw new McpAuthorizationException(401, "Invalid bearer token");
		}
		return principal;
	}

	public void requireScope(McpPrincipal principal, String scope) {
		if (principal == null || principal.getScopes() == null || !principal.getScopes().contains(scope)) {
			throw new McpAuthorizationException(403, "Insufficient scope");
		}
	}
}
