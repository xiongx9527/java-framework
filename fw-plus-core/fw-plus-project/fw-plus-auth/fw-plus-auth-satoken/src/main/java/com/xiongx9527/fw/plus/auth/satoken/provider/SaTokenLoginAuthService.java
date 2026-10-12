package com.xiongx9527.fw.plus.auth.satoken.provider;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpUtil;
import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.AuthHeaderExtractor;

import java.util.Collections;

/**
 * Sa-Token 登录 token 认证：校验既有登录态（{@code StpUtil.getLoginIdByToken}），
 * 不签发令牌、不保存凭据。
 * <p>
 * 凭据读取：Sa-Token tokenName header（默认 {@code satoken}，取 {@code SaManager.getConfig()}），
 * 其次 {@code Authorization: Bearer}。登录态 token 不携带 scopes，认证主体 scopes 为空集。
 * </p>
 */
public class SaTokenLoginAuthService implements AuthService {

    @Override
    public AuthPrincipal authenticate(AuthRequest request) {
        try {
            String token = loginToken(request);
            if (token == null) {
                return AuthPrincipal.ANONYMOUS;
            }
            Object loginId = StpUtil.getLoginIdByToken(token);
            if (loginId == null) {
                return AuthPrincipal.ANONYMOUS;
            }
            String subject = String.valueOf(loginId);
            if (subject.trim().isEmpty()) {
                return AuthPrincipal.ANONYMOUS;
            }
            return new AuthPrincipal(subject, Collections.<String>emptySet());
        } catch (RuntimeException e) {
            // 无效 token、Sa-Token 未初始化等一律 fail-closed
            return AuthPrincipal.ANONYMOUS;
        }
    }

    private String loginToken(AuthRequest request) {
        String tokenName = SaManager.getConfig().getTokenName();
        String token = AuthHeaderExtractor.header(request, tokenName);
        if (token == null && tokenName != null && !"satoken".equalsIgnoreCase(tokenName)) {
            token = AuthHeaderExtractor.header(request, "satoken");
        }
        if (token == null) {
            token = AuthHeaderExtractor.bearerToken(request);
        }
        return token;
    }
}