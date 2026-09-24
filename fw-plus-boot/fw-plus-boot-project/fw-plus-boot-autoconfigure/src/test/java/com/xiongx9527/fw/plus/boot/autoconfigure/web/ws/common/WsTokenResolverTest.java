package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig.TokenRule;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 按端点路径解析 token 规则：「一个实例对应一条路径 + 一个 token」的匹配语义。
 */
class WsTokenResolverTest {

    @Test
    void relativeRulePathShouldMatchSamePath() {
        WsConfig config = config(rule("/ws/api2", "app2-secret", null));

        assertThat(WsTokenResolver.resolveSecret(config, "/ws/api2")).isEqualTo("app2-secret");
    }

    @Test
    void fullRulePathShouldMatchRequestWithBasePath() {
        WsConfig config = config(rule("/magic-api/web/ws/api2", "app2-secret", null));

        assertThat(WsTokenResolver.resolveSecret(config, "/magic-api/web/ws/api2")).isEqualTo("app2-secret");
        // 规则写完整路径时，不同基础路径下的同名端点不应命中
        assertThat(WsTokenResolver.resolveSecret(config, "/other/web/ws/api2")).isNull();
    }

    @Test
    void rulePathShouldMatchRequestPrefixedByBasePath() {
        WsConfig config = config(rule("/ws/api2", "app2-secret", null));

        assertThat(WsTokenResolver.resolveSecret(config, "/magic-api/web/ws/api2")).isEqualTo("app2-secret");
    }

    @Test
    void suffixMatchShouldRequireSlashSegmentBoundary() {
        WsConfig config = config(rule("/api", "app2-secret", null));

        // /my-api 的尾段是 -api，并非以 /api 结尾，不得命中
        assertThat(WsTokenResolver.resolveSecret(config, "/my-api")).isNull();
        assertThat(WsTokenResolver.resolveSecret(config, "/web/ws/api")).isEqualTo("app2-secret");
    }

    @Test
    void exactMatchShouldWinOverSuffixMatch() {
        WsConfig config = config(
                rule("/api", "suffix-secret", null),
                rule("/web/api", "exact-secret", null));

        // 精确规则即使排在后面，也应优先于先配置的后缀规则命中
        assertThat(WsTokenResolver.resolveSecret(config, "/web/api")).isEqualTo("exact-secret");
    }

    @Test
    void unmatchedPathShouldResolveToNullSoCallerFallsBackToGlobalSecret() {
        WsConfig config = config(rule("/ws/api2", "app2-secret", 4L));

        assertThat(WsTokenResolver.resolveSecret(config, "/ws/api")).isNull();
        assertThat(WsTokenResolver.resolveExpireHours(config, "/ws/api")).isNull();
        assertThat(WsTokenResolver.resolveSecret(config, null)).isNull();
        assertThat(WsTokenResolver.resolveSecret(config, "  ")).isNull();
    }

    @Test
    void blankRuleSecretShouldResolveToNull() {
        WsConfig config = config(rule("/ws/api2", "   ", null));

        // 规则密钥留空表示回落全局密钥，按未命中处理
        assertThat(WsTokenResolver.resolveSecret(config, "/ws/api2")).isNull();
        assertThat(WsTokenResolver.resolve(config, "/ws/api2").getExpireHours()).isNull();
    }

    @Test
    void trailingSlashShouldHitSameRule() {
        WsConfig config = config(rule("/ws/api2", "app2-secret", null));

        assertThat(WsTokenResolver.resolveSecret(config, "/ws/api2/")).isEqualTo("app2-secret");
        assertThat(WsTokenResolver.resolveSecret(config, "/ws/api2?token=x")).isEqualTo("app2-secret");
    }

    @Test
    void noTokensShouldResolveToNull() {
        WsConfig config = new WsConfig();

        assertThat(WsTokenResolver.resolveSecret(config, "/ws/api2")).isNull();
        assertThat(WsTokenResolver.resolveSecret(null, "/ws/api2")).isNull();
    }

    @Test
    void expireHoursShouldBeExposedWhenConfigured() {
        WsConfig config = config(rule("/ws/api2", "app2-secret", 6L));

        assertThat(WsTokenResolver.resolveExpireHours(config, "/ws/api2")).isEqualTo(6L);
        // 未配置有效期时返回 null，由调用方继承全局值
        assertThat(WsTokenResolver.resolveExpireHours(config(rule("/ws/api3", "s", null)), "/ws/api3")).isNull();
    }

    @Test
    void firstConfiguredRuleShouldWinForAmbiguousSuffix() {
        WsConfig config = config(
                rule("/ws/api", "first-secret", null),
                rule("/api", "second-secret", null));

        assertThat(WsTokenResolver.resolveSecret(config, "/bff/ws/api")).isEqualTo("first-secret");
    }

    private static WsConfig config(TokenRule... rules) {
        WsConfig config = new WsConfig();
        if (rules != null && rules.length > 0) {
            config.setTokens(Arrays.asList(rules));
        } else {
            config.setTokens(Collections.<TokenRule>emptyList());
        }
        return config;
    }

    private static TokenRule rule(String path, String secret, Long expireHours) {
        TokenRule rule = new TokenRule();
        rule.setPath(path);
        rule.setSecret(secret);
        rule.setExpireHours(expireHours);
        return rule;
    }
}
