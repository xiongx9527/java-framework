package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.satoken.provider.SaTokenLoginAuthService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SaTokenLoginAuthServiceTest {

    private final AuthService service = new SaTokenLoginAuthService();

    @Test
    void shouldRejectWhenNoCredentialHeadersPresent() {
        assertThat(service.authenticate(request(new HashMap<>()))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectWhenTokenHeaderIsNotAValidLoginToken() {
        // 不存在的 token：getLoginIdByToken 返回 null 或抛异常一律转匿名
        Map<String, String> headers = new HashMap<>();
        headers.put("SATOKEN", "not-a-real-token");
        assertThat(service.authenticate(request(headers))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectWhenBearerTokenIsNotAValidLoginToken() {
        assertThat(service.authenticate(request("Bearer not-a-real-token"))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectNonBearerAuthorization() {
        assertThat(service.authenticate(request("Basic abc"))).isSameAs(AuthPrincipal.ANONYMOUS);
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