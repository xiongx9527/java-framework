package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.satoken.provider.SaTokenSsoAuthService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SaTokenSsoAuthServiceTest {

    private final AuthService service = new SaTokenSsoAuthService();

    @Test
    void shouldRejectWhenNoTicketPresent() {
        assertThat(service.authenticate(request(new HashMap<>()))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectUnknownTicketHeader() {
        // 默认模式二：由客户端按共享存储校验 ticket（不联网），ticket 不存在即 fail-closed
        Map<String, String> headers = new HashMap<>();
        headers.put("ticket", "not-a-real-ticket");

        assertThat(service.authenticate(request(headers))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectUnknownTicketFromAuthorizationHeader() {
        assertThat(service.authenticate(request("Bearer not-a-real-ticket"))).isSameAs(AuthPrincipal.ANONYMOUS);
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