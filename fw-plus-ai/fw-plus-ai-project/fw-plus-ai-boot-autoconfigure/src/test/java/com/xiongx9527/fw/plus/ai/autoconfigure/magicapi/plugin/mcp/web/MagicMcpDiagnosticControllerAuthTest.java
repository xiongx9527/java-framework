package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpPrincipal;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth.McpRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicMcpDiagnosticControllerAuthTest {

    @Test
    void shouldRejectResourceDiagnosticWithoutMatchingAuthService() throws Exception {
        McpServerRuntime runtime = runtimeWithoutAuth();
        MagicMcpDiagnosticController controller = controller(runtime);
        Map<String, String> body = new HashMap<>();
        body.put("key", "diagnostics");
        body.put("uri", "resource://private");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.execute(body, new MockHttpServletRequest(), response);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(runtime, never()).getFeatureMgr();
    }

    @Test
    void shouldRejectPromptDiagnosticWithoutMatchingAuthService() throws Exception {
        McpServerRuntime runtime = runtimeWithoutAuth();
        MagicMcpDiagnosticController controller = controller(runtime);
        Map<String, String> body = new HashMap<>();
        body.put("key", "diagnostics");
        body.put("name", "private-prompt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.executePrompt(body, new MockHttpServletRequest(), response);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(runtime, never()).getFeatureMgr();
    }

    @Test
    void shouldRejectCapabilityListingWithoutMatchingAuthService() throws Exception {
        McpServerRuntime runtime = runtimeWithoutAuth();
        MagicMcpDiagnosticController controller = controller(runtime);
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.capabilities("diagnostics", new MockHttpServletRequest(), response);

        assertThat(response.getStatus()).isEqualTo(401);
        verify(runtime, never()).getFeatureMgr();
    }

    @Test
    void shouldRejectResourceDiagnosticWhenAuthenticatedPrincipalHasNoScopes() throws Exception {
        McpServerRuntime runtime = runtimeWithoutScopes();
        MagicMcpDiagnosticController controller = controller(runtime);
        Map<String, String> body = new HashMap<>();
        body.put("key", "diagnostics");
        body.put("uri", "resource://private");
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.execute(body, new MockHttpServletRequest(), response);

        assertThat(response.getStatus()).isEqualTo(403);
        verify(runtime, never()).getFeatureMgr();
    }

    private MagicMcpDiagnosticController controller(McpServerRuntime runtime) {
        return new MagicMcpDiagnosticController(mock(org.ssssssss.magicapi.core.config.MagicConfiguration.class),
                () -> java.util.Collections.singletonList(runtime));
    }

    private McpServerRuntime runtimeWithoutAuth() {
        McpServerRuntime runtime = mock(McpServerRuntime.class);
        when(runtime.getKey()).thenReturn("diagnostics");
        when(runtime.getTransport()).thenReturn("http-sse");
        when(runtime.getEndpoint()).thenReturn("/mcp/diagnostics");
        return runtime;
    }

    private McpServerRuntime runtimeWithoutScopes() {
        McpServerRuntime runtime = runtimeWithoutAuth();
        com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties properties = new com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.config.McpProperties();
        properties.setScopeCheckEnabled(true);
        when(runtime.getProperties()).thenReturn(properties);
        when(runtime.getAuthService()).thenReturn(new com.xiongx9527.fw.plus.auth.core.AuthService() {
            @Override
            public com.xiongx9527.fw.plus.auth.core.AuthPrincipal authenticate(com.xiongx9527.fw.plus.auth.core.AuthRequest request) {
                return new com.xiongx9527.fw.plus.auth.core.AuthPrincipal("tester", java.util.Collections.emptySet());
            }
        });
        return runtime;
    }
}