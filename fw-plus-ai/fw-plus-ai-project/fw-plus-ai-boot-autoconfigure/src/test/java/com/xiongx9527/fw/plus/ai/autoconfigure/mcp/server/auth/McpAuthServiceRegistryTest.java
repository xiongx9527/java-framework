package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.auth;

import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.auth.core.config.InstanceAuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class McpAuthServiceRegistryTest {

    private final InstanceAuthRegistry registry = new InstanceAuthRegistry();

    @Test
    void shouldReturnNullWhenAuthUnconfigured() {
        assertThat(registry.resolve("default", null)).isNull();
        assertThat(registry.resolve("default", auth(null))).isNull();
        assertThat(registry.resolve("default", auth("  "))).isNull();
    }

    @Test
    void shouldResolvePermitAllWithoutModuleGuard() {
        AuthService service = registry.resolve("default", auth(InstanceAuthRegistry.PROVIDER_PERMIT_ALL));
        assertThat(service).isNotNull();
        assertThat(service.authenticate(new AuthRequest("default", "session", "http-sse", "/mcp", null, null)))
                .isSameAs(AuthPrincipal.PERMIT_ALL);
    }

    @Test
    void shouldResolveEveryClasspathAvailableProvider() {
        // 测试 classpath 含全部 sa-token optional 模块：清单与解析结果一一对应
        assertThat(registry.availableProviders()).containsExactly(
                InstanceAuthRegistry.PROVIDER_PERMIT_ALL,
                InstanceAuthRegistry.PROVIDER_SA_TOKEN_LOGIN,
                InstanceAuthRegistry.PROVIDER_SA_TOKEN_OAUTH2,
                InstanceAuthRegistry.PROVIDER_SA_TOKEN_JWT,
                InstanceAuthRegistry.PROVIDER_SA_TOKEN_APIKEY,
                InstanceAuthRegistry.PROVIDER_SA_TOKEN_SSO);

        assertThat(registry.resolve("default", auth(InstanceAuthRegistry.PROVIDER_SA_TOKEN_LOGIN)))
                .isNotNull();
        assertThat(registry.resolve("default", auth(InstanceAuthRegistry.PROVIDER_SA_TOKEN_OAUTH2)))
                .isNotNull();
        assertThat(registry.resolve("default", auth(InstanceAuthRegistry.PROVIDER_SA_TOKEN_JWT)))
                .isNotNull();
        assertThat(registry.resolve("default", auth(InstanceAuthRegistry.PROVIDER_SA_TOKEN_APIKEY)))
                .isNotNull();
        assertThat(registry.resolve("default", auth(InstanceAuthRegistry.PROVIDER_SA_TOKEN_SSO)))
                .isNotNull();
    }

    @Test
    void shouldFailClosedWhenConfiguredProviderModuleMissing() {
        InstanceAuthRegistry filtered = new InstanceAuthRegistry(
                new FilteredClassLoader("cn.dev33.satoken.jwt.SaJwtUtil"));

        assertThat(filtered.availableProviders())
                .doesNotContain(InstanceAuthRegistry.PROVIDER_SA_TOKEN_JWT)
                .contains(InstanceAuthRegistry.PROVIDER_PERMIT_ALL);

        // 已配置但模块缺失：解析不抛错、不回退，而是返回 fail-closed 拒绝服务
        AuthService service = filtered.resolve("default",
                auth(InstanceAuthRegistry.PROVIDER_SA_TOKEN_JWT));
        assertThat(service).isNotNull();
        assertThat(service.authenticate(
                new AuthRequest("default", "session", "http-sse", "/mcp", null, null)))
                .isSameAs(AuthPrincipal.ANONYMOUS);
    }

    @Test
    void shouldRejectUnknownProviderIncludingLegacyCustom() {
        assertThatThrownBy(() -> registry.resolve("default", auth("custom")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported authentication provider 'custom'");
        assertThatThrownBy(() -> registry.resolve("default", auth("unknown")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsupported authentication provider 'unknown'");
    }

    private InstanceAuthProperties auth(String provider) {
        InstanceAuthProperties a = new InstanceAuthProperties();
        a.setProvider(provider);
        return a;
    }
}