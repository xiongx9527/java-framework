package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.auth;

import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.CustomAuthService;
import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.auth.core.config.InstanceAuthProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter.WsAutoConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;

import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * 业务自定义 provider 在 websocket 侧的装配验收：
 * {@link CustomAuthService} 注册到 {@link InstanceAuthRegistry} 后，握手按自定义 key 走业务实现；
 * 未注册自定义 key 时启动期校验 fail-closed；内置 provider 行为不变。
 */
@DisplayName("Ws 自定义 provider 装配")
class WsCustomAuthProviderTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("fw.plus.ws.enabled=true")
            .withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class, WsAutoConfiguration.class));

    @Test
    @DisplayName("注册自定义 provider 后握手按 key 走业务实现")
    void shouldAuthenticateHandshakeByCustomProvider() {
        InstanceAuthRegistry registry = new InstanceAuthRegistry();
        registry.registerProvider("my-company-oidc", new StubCustomAuthService("my-company-oidc"));
        WsConfig config = new WsConfig();
        config.setPath("/ws/api");
        config.getAuth().setProvider("my-company-oidc");
        WsAuthSupport support = new WsAuthSupport(config, registry);

        assertThat(support.isAuthenticated("/ws/api", new LinkedHashMap<>())).isTrue();
    }

    @Test
    @DisplayName("自定义 provider 未注册时启动期校验 fail-closed")
    void shouldFailValidationWhenCustomProviderNotRegistered() {
        InstanceAuthRegistry registry = new InstanceAuthRegistry();
        WsConfig config = new WsConfig();
        config.getAuth().setProvider("my-company-oidc");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> WsAuthSupport.validateProviders(config, registry))
                .withMessageContaining("my-company-oidc");
    }

    @Test
    @DisplayName("存在自定义 provider 时内置 permit-all 行为不变")
    void shouldKeepBuiltInProviderWhenCustomProviderPresent() {
        InstanceAuthRegistry registry = new InstanceAuthRegistry();
        registry.registerProvider("my-company-oidc", new StubCustomAuthService("my-company-oidc"));
        WsConfig config = new WsConfig();
        config.setPath("/ws/api");
        config.getAuth().setProvider(InstanceAuthRegistry.PROVIDER_PERMIT_ALL);
        WsAuthSupport support = new WsAuthSupport(config, registry);

        assertThat(support.isAuthenticated("/ws/api", new LinkedHashMap<>())).isTrue();
        assertThat(registry.availableProviders())
                .contains(InstanceAuthRegistry.PROVIDER_PERMIT_ALL, "my-company-oidc");
    }

    @Test
    @DisplayName("自动配置把业务 CustomAuthService 注册进 InstanceAuthRegistry")
    void shouldRegisterCustomAuthServiceFromContainer() {
        runner
                .withBean(CustomAuthService.class, () -> new StubCustomAuthService("my-company-oidc"))
                .run(context -> {
                    InstanceAuthRegistry registry = context.getBean(InstanceAuthRegistry.class);
                    assertThat(registry.availableProviders()).contains("my-company-oidc");

                    InstanceAuthProperties auth = new InstanceAuthProperties();
                    auth.setProvider("my-company-oidc");
                    assertThat(registry.resolve("default", auth))
                            .isSameAs(context.getBean(CustomAuthService.class));
                });
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
