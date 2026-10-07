package com.xiongx9527.fw.plus.auth.satoken.provider;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.jwt.SaJwtUtil;
import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.json.JSONObject;
import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.AuthHeaderExtractor;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Sa-Token JWT 认证：校验 Bearer JWT 的签名、loginType 与有效期（{@code SaJwtUtil.getPayloads}），
 * 不签发令牌。
 * <p>
 * 主体取 {@code loginId} claim；scopes 取 {@code scopes}（或 {@code scope}）claim，缺失为空集。
 * 未配置 {@code jwt-secret-key} 时 fail-closed 拒绝全部请求。
 * </p>
 */
public class SaTokenJwtAuthService implements AuthService {

    @Override
    public AuthPrincipal authenticate(AuthRequest request) {
        String token = AuthHeaderExtractor.bearerToken(request);
        if (token == null) {
            return AuthPrincipal.ANONYMOUS;
        }
        try {
            String keyt = SaManager.getConfig().getJwtSecretKey();
            if (keyt == null || keyt.trim().isEmpty()) {
                // 未配置签名密钥：不接受任何 JWT，fail-closed
                return AuthPrincipal.ANONYMOUS;
            }
            JSONObject payloads = SaJwtUtil.getPayloads(token, StpUtil.getLoginType(), keyt);
            if (payloads == null) {
                return AuthPrincipal.ANONYMOUS;
            }
            Object loginId = payloads.get(SaJwtUtil.LOGIN_ID);
            if (loginId == null) {
                return AuthPrincipal.ANONYMOUS;
            }
            String subject = String.valueOf(loginId);
            if (subject.trim().isEmpty()) {
                return AuthPrincipal.ANONYMOUS;
            }
            return new AuthPrincipal(subject, scopes(payloads));
        } catch (RuntimeException e) {
            // 签名不匹配、loginType 不匹配、已过期等一律 fail-closed
            return AuthPrincipal.ANONYMOUS;
        }
    }

    private Set<String> scopes(JSONObject payloads) {
        Object value = payloads.get("scopes");
        if (value == null) {
            value = payloads.get("scope");
        }
        return scopesOf(value);
    }

    private Set<String> scopesOf(Object value) {
        Set<String> scopes = new LinkedHashSet<>();
        if (value == null) {
            return scopes;
        }
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                addScope(scopes, item);
            }
        } else if (value instanceof Object[]) {
            for (Object item : (Object[]) value) {
                addScope(scopes, item);
            }
        } else {
            addScope(scopes, value);
        }
        return scopes;
    }

    private void addScope(Set<String> scopes, Object item) {
        if (item == null) {
            return;
        }
        String text = String.valueOf(item).trim();
        if (text.isEmpty()) {
            return;
        }
        for (String part : text.split("[\\s,]+")) {
            if (!part.isEmpty()) {
                scopes.add(part);
            }
        }
    }
}