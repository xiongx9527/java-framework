package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import cn.dev33.satoken.apikey.SaApiKeyManager;
import cn.dev33.satoken.apikey.model.ApiKeyModel;
import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.satoken.provider.SaTokenApiKeyAuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SaTokenApiKeyAuthServiceTest {

    private static final String API_KEY = "mcp-test-api-key-12345";

    private final AuthService service = new SaTokenApiKeyAuthService();

    @BeforeEach
    void saveApiKey() {
        ApiKeyModel model = new ApiKeyModel();
        model.setApiKey(API_KEY);
        model.setLoginId(20001);
        model.setScopes(Arrays.asList("tools/list", "resources/read"));
        model.setExpiresTime(-1);
        SaApiKeyManager.getSaApiKeyTemplate().saveApiKey(model);
    }

    @Test
    void shouldRejectWhenNoCredentialPresent() {
        assertThat(service.authenticate(request(new HashMap<>()))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectUnknownApiKeyInNamespaceHeader() {
        Map<String, String> headers = new HashMap<>();
        headers.put("apikey", "unknown-key");

        assertThat(service.authenticate(request(headers))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectUnknownApiKeyFromAuthorizationHeader() {
        assertThat(service.authenticate(request("Bearer unknown-key"))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldAuthenticateApiKeyFromNamespaceHeaderWithNativeScopes() {
        Map<String, String> headers = new HashMap<>();
        headers.put("APIKEY", API_KEY);

        AuthPrincipal principal = service.authenticate(request(headers));

        assertThat(principal.isAuthenticated()).isTrue();
        assertThat(principal.getSubject()).isEqualTo("20001");
        // scopes 取 Key 对应凭据的原生授权范围
        assertThat(principal.getScopes()).containsExactly("tools/list", "resources/read");
    }

    @Test
    void shouldAuthenticateApiKeyFromAuthorizationHeader() {
        AuthPrincipal principal = service.authenticate(request("Bearer " + API_KEY));

        assertThat(principal.isAuthenticated()).isTrue();
        assertThat(principal.getSubject()).isEqualTo("20001");
    }

    private AuthRequest request(String authorization) {
        Map<String, String> headers = new HashMap<>();
        if (authorization != null) {
            headers.put("Authorization", authorization);
        }
        return request(headers);
    }

    private AuthRequest request(Map<String, String> headers) {
        return new AuthRequest("session-1", "http-sse", "/mcp", "{}", headers);
    }
}