package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.PermitAllAuthService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PermitAllAuthServiceTest {

    private final AuthService service = new PermitAllAuthService();

    @Test
    void shouldAlwaysAuthenticateToPermitAll() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer evil-token");

        AuthPrincipal principal = service.authenticate(
                new AuthRequest("session-1", "http-sse", "/mcp", "{\"id\":1}", headers));

        assertThat(principal).isSameAs(AuthPrincipal.PERMIT_ALL);
        assertThat(principal).isNotSameAs(AuthPrincipal.ANONYMOUS);
        assertThat(principal.getSubject()).isEqualTo("permit-all");
        assertThat(principal.getScopes()).isEmpty();
    }

    @Test
    void shouldAcceptEveryRequestShape() {
        // 空 body / 空 headers / 空 sessionId 均不抛异常
        AuthPrincipal principal = service.authenticate(new AuthRequest(null, "streamable-http", "/mcp",
                "", Collections.emptyMap()));

        assertThat(principal.getSubject()).isEqualTo("permit-all");
    }
}