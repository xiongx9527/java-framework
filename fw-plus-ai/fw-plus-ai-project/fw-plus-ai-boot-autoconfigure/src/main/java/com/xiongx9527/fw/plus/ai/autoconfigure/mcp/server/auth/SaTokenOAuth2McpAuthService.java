package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import cn.dev33.satoken.oauth2.data.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.template.SaOAuth2Util;

import java.util.Collections;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

/**
 * Sa-Token OAuth2 认证：从 {@code Authorization: Bearer <token>} 取 access token，
 * 由 Sa-Token 校验后转为 MCP 主体，scope 直接来自 token 的 scopes。
 *
 * <p>从 Boot 层同名实现迁移而来；未引入 sa-token-oauth2 时本类不会被实例化，
 * 因此可作为 optional 依赖存在。</p>
 */
public class SaTokenOAuth2McpAuthService implements McpAuthService {

	@Override
	public McpPrincipal authenticate(McpRequest request) {
		String token = bearerToken(request);
		if (token == null) {
			return McpPrincipal.ANONYMOUS;
		}

		try {
			AccessTokenModel accessToken = SaOAuth2Util.checkAccessToken(token);
			Object loginId = accessToken.getLoginId();
			String subject = loginId == null ? accessToken.getClientId() : String.valueOf(loginId);
			if (subject == null || subject.trim().isEmpty()) {
				return McpPrincipal.ANONYMOUS;
			}
			Set<String> scopes = accessToken.getScopes() == null
					? Collections.<String>emptySet()
					: new HashSet<>(accessToken.getScopes());
			return new McpPrincipal(subject, scopes);
		} catch (RuntimeException e) {
			return McpPrincipal.ANONYMOUS;
		}
	}

	private String bearerToken(McpRequest request) {
		if (request == null || request.getHeaders() == null) {
			return null;
		}
		for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase("Authorization")) {
				String value = entry.getValue();
				if (value == null) {
					return null;
				}
				String trimmed = value.trim();
				int space = trimmed.indexOf(' ');
				if (space > 0 && "Bearer".equalsIgnoreCase(trimmed.substring(0, space))) {
					String token = trimmed.substring(space + 1).trim();
					return token.isEmpty() ? null : token;
				}
				return null;
			}
		}
		return null;
	}
}
