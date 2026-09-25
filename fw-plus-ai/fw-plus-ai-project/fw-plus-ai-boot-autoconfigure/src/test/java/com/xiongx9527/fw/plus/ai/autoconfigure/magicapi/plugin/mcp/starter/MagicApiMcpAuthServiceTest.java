package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.starter;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpPrincipal;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.auth.McpRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MagicApiMcpAuthServiceTest {

    private MagicApiMcpAuthService service;

    @BeforeEach
    void setUp() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        service = new MagicApiMcpAuthService(properties.getAuth());
    }

    @Test
    void shouldPermitWhenAuthDisabled() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getAuth().setEnabled(false);
        service = new MagicApiMcpAuthService(properties.getAuth());

        McpPrincipal principal = service.authenticate(request(null));

        assertThat(principal).isSameAs(McpPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectMissingAuthorizationHeader() {
        McpPrincipal principal = service.authenticate(request(null));

        assertThat(principal).isSameAs(McpPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectMalformedAuthorizationHeader() {
        McpPrincipal principal = service.authenticate(request("Basic abc"));

        assertThat(principal).isSameAs(McpPrincipal.ANONYMOUS);
    }

    @Test
    void shouldParseSubjectAndScopes() {
        McpPrincipal principal = service.authenticate(request("Bearer user-1.mcp:tools,mcp:resources"));

        assertThat(principal.getSubject()).isEqualTo("user-1");
        assertThat(principal.getScopes()).containsExactly("mcp:tools", "mcp:resources");
        assertThat(service.hasScope(principal, "mcp:tools")).isTrue();
        assertThat(service.hasScope(principal, "mcp:prompts")).isFalse();
    }

    @Test
    void shouldTreatTokenWithoutScopePartAsEmptyScopes() {
        McpPrincipal principal = service.authenticate(request("Bearer user-1"));

        assertThat(principal.getSubject()).isEqualTo("user-1");
        assertThat(principal.getScopes()).isEmpty();
    }

    @Test
    void shouldExposeConfiguredScopes() {
        assertThat(service.toolsScope()).isEqualTo("mcp:tools");
        assertThat(service.resourcesScope()).isEqualTo("mcp:resources");
        assertThat(service.promptsScope()).isEqualTo("mcp:prompts");
    }

    private McpRequest request(String authorization) {
        Map<String, String> headers = new HashMap<>();
        if (authorization != null) {
            headers.put("Authorization", authorization);
        }
        return new McpRequest("session-1", "http-sse", "/mcp", "{\"id\":1}", headers);
    }
}
