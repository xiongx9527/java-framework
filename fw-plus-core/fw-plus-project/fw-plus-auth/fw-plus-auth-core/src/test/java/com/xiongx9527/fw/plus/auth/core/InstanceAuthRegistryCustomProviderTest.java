package com.xiongx9527.fw.plus.auth.core;

import com.xiongx9527.fw.plus.auth.core.config.InstanceAuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * InstanceAuthRegistry 自定义 provider 注册/解析行为。
 */
@DisplayName("InstanceAuthRegistry 自定义 provider")
class InstanceAuthRegistryCustomProviderTest {

    private static InstanceAuthProperties auth(String provider) {
        InstanceAuthProperties props = new InstanceAuthProperties();
        props.setProvider(provider);
        return props;
    }

    @Test
    @DisplayName("注册自定义 provider 后按 key 解析业务实现")
    void resolvesRegisteredCustomProvider() {
        InstanceAuthRegistry registry = new InstanceAuthRegistry();
        StubCustomAuthService service = new StubCustomAuthService("my-company-oidc");
        registry.registerProvider(service.getProviderKey(), service);

        AuthService resolved = registry.resolve("default", auth("my-company-oidc"));

        assertThat(resolved).isSameAs(service);
    }

    @Test
    @DisplayName("自定义 provider 未注册时抛 IllegalArgumentException（fail-closed）")
    void throwsWhenCustomProviderNotRegistered() {
        InstanceAuthRegistry registry = new InstanceAuthRegistry();

        assertThatThrownBy(() -> registry.resolve("default", auth("my-company-oidc")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("my-company-oidc");
    }

    @Test
    @DisplayName("未知 provider 同样抛 IllegalArgumentException")
    void throwsWhenProviderUnknown() {
        InstanceAuthRegistry registry = new InstanceAuthRegistry();

        assertThatThrownBy(() -> registry.resolve("default", auth("bogus")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bogus");
    }

    @Test
    @DisplayName("内置 permit-all 行为不变")
    void builtInPermitAllUnchanged() {
        InstanceAuthRegistry registry = new InstanceAuthRegistry();

        AuthService resolved = registry.resolve("default", auth(InstanceAuthRegistry.PROVIDER_PERMIT_ALL));

        assertThat(resolved).isInstanceOf(PermitAllAuthService.class);
        assertThat(resolved.authenticate(null)).isEqualTo(AuthPrincipal.PERMIT_ALL);
    }

    @Test
    @DisplayName("自定义 key 与内置 key 冲突时内置优先")
    void builtInKeyWinsOverCustom() {
        InstanceAuthRegistry registry = new InstanceAuthRegistry();
        registry.registerProvider(InstanceAuthRegistry.PROVIDER_PERMIT_ALL, new StubCustomAuthService("shadow"));

        AuthService resolved = registry.resolve("default", auth(InstanceAuthRegistry.PROVIDER_PERMIT_ALL));

        assertThat(resolved).isInstanceOf(PermitAllAuthService.class);
    }

    @Test
    @DisplayName("重复注册同一 key 时首次为准")
    void duplicateKeyKeepsFirstRegistration() {
        InstanceAuthRegistry registry = new InstanceAuthRegistry();
        StubCustomAuthService first = new StubCustomAuthService("dup");
        StubCustomAuthService second = new StubCustomAuthService("dup");
        registry.registerProvider("dup", first);
        registry.registerProvider("dup", second);

        AuthService resolved = registry.resolve("default", auth("dup"));

        assertThat(resolved).isSameAs(first);
    }

    @Test
    @DisplayName("availableProviders 追加已注册自定义 key")
    void availableProvidersIncludesCustomKeys() {
        InstanceAuthRegistry registry = new InstanceAuthRegistry();
        registry.registerProvider("my-company-oidc", new StubCustomAuthService("my-company-oidc"));

        List<String> providers = registry.availableProviders();

        assertThat(providers).contains(InstanceAuthRegistry.PROVIDER_PERMIT_ALL, "my-company-oidc");
    }

    @Test
    @DisplayName("provider 为空时返回 null（未认证）")
    void blankProviderReturnsNull() {
        InstanceAuthRegistry registry = new InstanceAuthRegistry();

        assertThat(registry.resolve("default", null)).isNull();
        assertThat(registry.resolve("default", auth("  "))).isNull();
    }

    private static final class StubCustomAuthService implements CustomAuthService {

        private final String providerKey;

        private StubCustomAuthService(String providerKey) {
            this.providerKey = providerKey;
        }

        @Override
        public String getProviderKey() {
            return providerKey;
        }

        @Override
        public AuthPrincipal authenticate(AuthRequest request) {
            return new AuthPrincipal("custom:" + providerKey);
        }
    }
}
