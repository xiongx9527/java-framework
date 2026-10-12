package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.jwt.SaJwtUtil;
import cn.dev33.satoken.stp.StpUtil;
import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.satoken.provider.SaTokenJwtAuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SaTokenJwtAuthServiceTest {

    private static final String SECRET = "mcp-jwt-test-secret";

    private final AuthService service = new SaTokenJwtAuthService();

    private String originalSecretKey;

    @BeforeEach
    void rememberSecretKey() {
        originalSecretKey = SaManager.getConfig().getJwtSecretKey();
    }

    @AfterEach
    void restoreSecretKey() {
        SaManager.getConfig().setJwtSecretKey(originalSecretKey);
    }

    @Test
    void shouldRejectWhenBearerHeaderMissing() {
        assertThat(service.authenticate(request(null))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldFailClosedWhenJwtSecretKeyNotConfigured() {
        SaManager.getConfig().setJwtSecretKey(null);

        assertThat(service.authenticate(request("Bearer " + token(SECRET))))
                .isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectWhenTokenSignedWithDifferentKey() {
        SaManager.getConfig().setJwtSecretKey(SECRET);

        assertThat(service.authenticate(request("Bearer " + token("another-secret"))))
                .isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectMalformedToken() {
        SaManager.getConfig().setJwtSecretKey(SECRET);

        assertThat(service.authenticate(request("Bearer not-a-jwt"))).isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldAuthenticateValidJwtCarryingScopesClaim() {
        SaManager.getConfig().setJwtSecretKey(SECRET);
        Map<String, Object> extraData = new HashMap<>();
        extraData.put("scopes", Arrays.asList("tools/list", "tools/call"));

        AuthPrincipal principal = service.authenticate(
                request("Bearer " + token(SECRET, extraData)));

        assertThat(principal.isAuthenticated()).isTrue();
        assertThat(principal.getSubject()).isEqualTo("10001");
        // scopes 取自 JWT 原生 claim
        assertThat(principal.getScopes()).containsExactly("tools/list", "tools/call");
    }

    @Test
    void shouldTreatMissingScopesClaimAsEmptySet() {
        SaManager.getConfig().setJwtSecretKey(SECRET);

        AuthPrincipal principal = service.authenticate(request("Bearer " + token(SECRET)));

        assertThat(principal.isAuthenticated()).isTrue();
        assertThat(principal.getScopes()).isEmpty();
    }

    private String token(String keyt) {
        return token(keyt, null);
    }

    private String token(String keyt, Map<String, Object> extraData) {
        // timeout=-1：EFF=-1 永不过期，通过 getPayloads 的有效期校验
        return SaJwtUtil.createToken(StpUtil.getLoginType(), 10001, "unit-test", -1, extraData, keyt);
    }

    private AuthRequest request(String authorization) {
        Map<String, String> headers = new HashMap<>();
        if (authorization != null) {
            headers.put("Authorization", authorization);
        }
        return new AuthRequest("session-1", "http-sse", "/mcp", "{}", headers);
    }
}