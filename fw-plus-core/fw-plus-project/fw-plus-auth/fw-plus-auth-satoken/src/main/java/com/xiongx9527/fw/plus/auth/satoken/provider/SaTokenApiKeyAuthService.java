package com.xiongx9527.fw.plus.auth.satoken.provider;

import cn.dev33.satoken.apikey.SaApiKeyManager;
import cn.dev33.satoken.apikey.model.ApiKeyModel;
import cn.dev33.satoken.apikey.template.SaApiKeyUtil;
import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.AuthHeaderExtractor;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Sa-Token API Key 认证：以 {@code SaApiKeyUtil.checkApiKey} 校验请求携带的 API Key，
 * 不签发、不保存任何 Key。
 * <p>
 * 凭据读取顺序对齐 Sa-Token 模板：apikey namespace header（默认 {@code apikey}，
 * 取自 {@code SaApiKeyManager} 的模板配置），其次 {@code Authorization}。
 * 主体与 scopes 取自 Key 对应的 {@link ApiKeyModel}，缺失 scopes 为空集。
 * </p>
 */
public class SaTokenApiKeyAuthService implements AuthService {

    @Override
    public AuthPrincipal authenticate(AuthRequest request) {
        try {
            String apiKey = readApiKey(request);
            if (apiKey == null) {
                return AuthPrincipal.ANONYMOUS;
            }
            ApiKeyModel model = SaApiKeyUtil.checkApiKey(apiKey);
            if (model == null || model.getLoginId() == null) {
                return AuthPrincipal.ANONYMOUS;
            }
            String subject = String.valueOf(model.getLoginId());
            if (subject.trim().isEmpty()) {
                return AuthPrincipal.ANONYMOUS;
            }
            Set<String> scopes = model.getScopes() == null
                    ? Collections.<String>emptySet()
                    : new LinkedHashSet<>(model.getScopes());
            return new AuthPrincipal(subject, scopes);
        } catch (RuntimeException e) {
            // 无效/过期/禁用 Key、存储未初始化等一律 fail-closed
            return AuthPrincipal.ANONYMOUS;
        }
    }

    private String readApiKey(AuthRequest request) {
        String namespace = SaApiKeyManager.getSaApiKeyTemplate().namespace;
        String value = AuthHeaderExtractor.header(request, namespace);
        if (value == null) {
            value = AuthHeaderExtractor.authorizationValue(request);
        }
        return value;
    }
}