package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.auth;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.service.KbSettingService;
import com.xiongx9527.fw.plus.auth.core.AuthPrincipal;
import com.xiongx9527.fw.plus.auth.core.AuthRequest;
import com.xiongx9527.fw.plus.auth.core.AuthService;
import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.auth.core.config.InstanceAuthProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 知识库对外 API 认证校验验收（A4/A5）：provider 由设置页选择、默认 {@code sa-token-login}、
 * 模块缺失或未知取值 fail-closed，并保留既有 {@code Authorization}/{@code kb-token} 凭据兼容。
 *
 * @author zzy
 */
class KbSaTokenAuthCheckerTest {

    @Test
    void permit_all显式选择时放行() {
        KbSaTokenAuthChecker checker = new KbSaTokenAuthChecker(
                new InstanceAuthRegistry(), setting("permit-all"));

        assertThat(checker.check(new LinkedHashMap<>())).isEqualTo("permit-all");
    }

    @Test
    void 默认provider为sa_token_login且无凭证时拒绝() {
        KbSaTokenAuthChecker checker = new KbSaTokenAuthChecker(
                new InstanceAuthRegistry(), setting("sa-token-login"));

        assertThatThrownBy(() -> checker.check(new LinkedHashMap<>()))
                .isInstanceOf(KbSaTokenAuthChecker.KbUnauthorizedException.class);
    }

    @Test
    void setting为空时回落默认provider() {
        // 真实 KbSettingService 不会返回 null，这里兜底校验 check 侧的默认回落
        KbSaTokenAuthChecker checker = new KbSaTokenAuthChecker(new InstanceAuthRegistry(), setting(null));

        assertThatThrownBy(() -> checker.check(new LinkedHashMap<>()))
                .isInstanceOf(KbSaTokenAuthChecker.KbUnauthorizedException.class);
    }

    @Test
    void 未知provider时fail_closed不回退() {
        KbSaTokenAuthChecker checker = new KbSaTokenAuthChecker(
                new InstanceAuthRegistry(), setting("custom"));

        assertThatThrownBy(() -> checker.check(new LinkedHashMap<>()))
                .isInstanceOf(KbSaTokenAuthChecker.KbUnauthorizedException.class)
                .hasMessageContaining("custom");
    }

    @Test
    void provider模块缺失时fail_closed() {
        InstanceAuthRegistry filtered = new InstanceAuthRegistry(
                new FilteredClassLoader("cn.dev33.satoken.stp.StpUtil"));
        KbSaTokenAuthChecker checker = new KbSaTokenAuthChecker(filtered, setting("sa-token-login"));

        assertThat(filtered.availableProviders())
                .doesNotContain(InstanceAuthRegistry.PROVIDER_SA_TOKEN_LOGIN);
        assertThatThrownBy(() -> checker.check(new LinkedHashMap<>()))
                .isInstanceOf(KbSaTokenAuthChecker.KbUnauthorizedException.class);
    }

    @Test
    void login下把Authorization的Bearer归一化到tokenName头() {
        CapturingRegistry registry = new CapturingRegistry();
        KbSaTokenAuthChecker checker = new KbSaTokenAuthChecker(registry, setting("sa-token-login"));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer abc");

        checker.check(headers);

        assertThat(registry.captured.getHeaders()).containsEntry("satoken", "abc");
    }

    @Test
    void login下kb_token可作为凭据来源() {
        CapturingRegistry registry = new CapturingRegistry();
        KbSaTokenAuthChecker checker = new KbSaTokenAuthChecker(registry, setting("sa-token-login"));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("kb-token", "kb-xyz");

        checker.check(headers);

        assertThat(registry.captured.getHeaders()).containsEntry("satoken", "kb-xyz");
    }

    @Test
    void 已存在tokenName头时不覆盖() {
        CapturingRegistry registry = new CapturingRegistry();
        KbSaTokenAuthChecker checker = new KbSaTokenAuthChecker(registry, setting("sa-token-login"));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("satoken", "direct");
        headers.put("Authorization", "Bearer abc");

        checker.check(headers);

        assertThat(registry.captured.getHeaders()).containsEntry("satoken", "direct");
    }

    @Test
    void 非login_provider不改写请求头() {
        CapturingRegistry registry = new CapturingRegistry();
        KbSaTokenAuthChecker checker = new KbSaTokenAuthChecker(registry, setting("permit-all"));
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Authorization", "Bearer abc");

        checker.check(headers);

        assertThat(registry.captured.getHeaders()).doesNotContainKey("satoken");
    }

    private static KbSettingService setting(String provider) {
        KbSettingService service = mock(KbSettingService.class);
        when(service.openApiAuthProvider()).thenReturn(provider);
        return service;
    }

    /** 捕获认证请求并恒放行的注册表桩件，用于观察 provider 解析与凭据归一化。 */
    private static final class CapturingRegistry extends InstanceAuthRegistry {

        private AuthRequest captured;

        @Override
        public AuthService resolve(String instanceKey, InstanceAuthProperties auth) {
            return request -> {
                captured = request;
                return AuthPrincipal.PERMIT_ALL;
            };
        }
    }
}