package com.xiongx9527.fw.plus.auth.satoken.provider;

import cn.dev33.satoken.oauth2.data.model.AccessTokenModel;
import cn.dev33.satoken.oauth2.template.SaOAuth2Util;
import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.AuthHeaderExtractor;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Sa-Token OAuth2 认证：从 {@code Authorization: Bearer <token>} 取 access token，
 * 由 Sa-Token 校验后转为主体，scope 直接来自 token 的 scopes。
 */
public class SaTokenOAuth2AuthService implements AuthService {

    @Override
    public AuthPrincipal authenticate(AuthRequest request) {
        String token = bearerToken(request);
        if (token == null) {
            return AuthPrincipal.ANONYMOUS;
        }

        try {
            AccessTokenModel accessToken = SaOAuth2Util.checkAccessToken(token);
            Object loginId = accessToken.getLoginId();
            String subject = loginId == null ? accessToken.getClientId() : String.valueOf(loginId);
            if (subject == null || subject.trim().isEmpty()) {
                return AuthPrincipal.ANONYMOUS;
            }
            Set<String> scopes = accessToken.getScopes() == null
                    ? Collections.<String>emptySet()
                    : new HashSet<>(accessToken.getScopes());
            return new AuthPrincipal(subject, scopes);
        } catch (RuntimeException e) {
            return AuthPrincipal.ANONYMOUS;
        }
    }

    private String bearerToken(AuthRequest request) {
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