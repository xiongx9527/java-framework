package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.satoken.provider.SaTokenOAuth2AuthService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SaTokenOAuth2AuthServiceTest {

    private final AuthService service = new SaTokenOAuth2AuthService();

    @Test
    void shouldRejectWhenAuthorizationHeaderMissing() {
        assertThat(service.authenticate(request(null))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectWhenAuthorizationIsNotBearer() {
        assertThat(service.authenticate(request("Basic abc"))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectWhenBearerTokenBlank() {
        assertThat(service.authenticate(request("Bearer    "))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectWhenSaTokenRejectsToken() {
        // 未初始化的 Sa-Token 上下文下 checkAccessToken 抛异常，转匿名而不是向外抛
        assertThat(service.authenticate(request("Bearer not-a-real-token"))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    private AuthRequest request(String authorization) {
        Map<String, String> headers = new HashMap<>();
        if (authorization != null) {
            headers.put("Authorization", authorization);
        }
        return new AuthRequest("session-1", "http-sse", "/mcp", "{\"id\":1}", headers);
    }
}