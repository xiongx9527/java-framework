package com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server;

import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.CustomAuthService;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.server.runtime.McpServerRuntime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 业务自定义 provider 在 mcp server 侧的装配验收：
 * {@link CustomAuthService} Bean 注册后，runtime 的 auth.provider 指向其 key 时走业务实现；
 * 未注册自定义 key 时启动即失败（fail-closed）；内置 provider 行为不受影响。
 */
@DisplayName("Mcp 自定义 provider 装配")
class McpCustomAuthProviderTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(McpAutoConfiguration.class));

    @Test
    @DisplayName("CustomAuthService Bean 注册后按 key 解析业务实现")
    void shouldUseCustomAuthServiceForConfiguredProvider() {
        contextRunner
                .withBean(CustomAuthService.class, () -> new StubCustomAuthService("my-company-oidc"))
                .withPropertyValues("fw-plus.ai.mcp.auth.provider=my-company-oidc")
                .run(context -> {
                    List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
                    assertThat(runtimes.get(0).getAuthService())
                            .isSameAs(context.getBean(CustomAuthService.class));
                });
    }

    @Test
    @DisplayName("命名 runtime 同样支持自定义 provider")
    void shouldUseCustomAuthServiceForNamedRuntime() {
        contextRunner
                .withBean(CustomAuthService.class, () -> new StubCustomAuthService("my-company-oidc"))
                .withPropertyValues(
                        "fw-plus.ai.mcp.servers[0].key=weather",
                        "fw-plus.ai.mcp.servers[0].endpoint=/mcp/weather",
                        "fw-plus.ai.mcp.servers[0].auth.provider=my-company-oidc")
                .run(context -> {
                    List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
                    assertThat(runtimes.get(0).getAuthService()).isNull();
                    assertThat(runtimes.get(1).getAuthService())
                            .isSameAs(context.getBean(CustomAuthService.class));
                });
    }

    @Test
    @DisplayName("自定义 provider 未注册时启动失败（fail-closed）")
    void shouldFailToStartWhenCustomProviderNotRegistered() {
        contextRunner
                .withPropertyValues("fw-plus.ai.mcp.auth.provider=my-company-oidc")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("my-company-oidc");
                });
    }

    @Test
    @DisplayName("存在 CustomAuthService Bean 时内置 provider 行为不变")
    void shouldKeepBuiltInProviderWhenCustomBeanPresent() {
        contextRunner
                .withBean(CustomAuthService.class, () -> new StubCustomAuthService("my-company-oidc"))
                .withPropertyValues("fw-plus.ai.mcp.auth.provider=permit-all")
                .run(context -> {
                    List<McpServerRuntime> runtimes = context.getBean("mcpServerRuntimes", List.class);
                    assertThat(runtimes.get(0).getAuthService())
                            .isNotSameAs(context.getBean(CustomAuthService.class));
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
