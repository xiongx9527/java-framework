package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.auth;

import cn.dev33.satoken.oauth2.data.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.template.SaOAuth2Util;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class SaTokenMcpAccessTokenVerifier implements McpAccessTokenVerifier {

	@Override
	public McpPrincipal verify(String token) {
		AccessTokenModel accessToken = SaOAuth2Util.checkAccessToken(token);
		String subject = accessToken.getLoginId() == null ? accessToken.getClientId() : String.valueOf(accessToken.getLoginId());
		List<String> scopes = accessToken.getScopes() == null ? Collections.emptyList() : accessToken.getScopes();
		return new McpPrincipal(subject, new HashSet<>(scopes));
	}
}
