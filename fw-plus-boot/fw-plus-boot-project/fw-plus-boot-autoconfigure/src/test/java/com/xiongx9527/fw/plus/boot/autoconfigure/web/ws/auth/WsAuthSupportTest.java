package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.auth;

import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * WS 握手认证内核（{@link WsAuthSupport}）验收：逐实例 provider 选择、不继承、fail-closed、
 * 未知 provider 启动期报错，以及 query 与请求头凭据合并。
 *
 * @author zzy
 */
class WsAuthSupportTest {

    private final InstanceAuthRegistry registry = new InstanceAuthRegistry();

    @Test
    void 未配置provider的实例握手一律拒绝() {
        WsConfig config = new WsConfig();
        config.setPath("/ws/api");
        WsAuthSupport support = new WsAuthSupport(config, registry);

        assertThat(support.isAuthenticated("/ws/api", new LinkedHashMap<>()))
                .as("未配置 provider 即 fail-closed").isFalse();
    }

    @Test
    void permit_all显式选择时放行() {
        WsConfig config = new WsConfig();
        config.setPath("/ws/api");
        config.getAuth().setProvider(InstanceAuthRegistry.PROVIDER_PERMIT_ALL);
        WsAuthSupport support = new WsAuthSupport(config, registry);

        assertThat(support.isAuthenticated("/ws/api", new LinkedHashMap<>())).isTrue();
    }

    @Test
    void 命名实例不从顶层auth继承() {
        WsConfig config = new WsConfig();
        config.setPath("/ws/api");
        config.getAuth().setProvider(InstanceAuthRegistry.PROVIDER_PERMIT_ALL);
        WsConfig.WsInstance instance = new WsConfig.WsInstance();
        instance.setKey("report");
        instance.setPath("/ws/api2");
        // 命名实例刻意不配置 provider
        config.getServers().add(instance);

        WsAuthSupport support = new WsAuthSupport(config, registry);

        assertThat(support.isAuthenticated("/ws/api", new LinkedHashMap<>()))
                .as("默认实例 permit-all").isTrue();
        assertThat(support.isAuthenticated("/ws/api2", new LinkedHashMap<>()))
                .as("命名实例未配置 provider，即便顶层为 permit-all 也不继承，一律拒绝").isFalse();
    }

    @Test
    void 命名实例的provider只作用于该实例() {
        WsConfig config = new WsConfig();
        config.setPath("/ws/api");
        // 默认实例刻意不配置 provider
        WsConfig.WsInstance instance = new WsConfig.WsInstance();
        instance.setKey("report");
        instance.setPath("/ws/api2");
        instance.getAuth().setProvider(InstanceAuthRegistry.PROVIDER_PERMIT_ALL);
        config.getServers().add(instance);

        WsAuthSupport support = new WsAuthSupport(config, registry);

        assertThat(support.isAuthenticated("/ws/api", new LinkedHashMap<>()))
                .as("默认实例未配置 provider，仍拒绝").isFalse();
        assertThat(support.isAuthenticated("/ws/api2", new LinkedHashMap<>()))
                .as("命名实例显式 permit-all，放行").isTrue();
    }

    @Test
    void provider模块缺失时fail_closed() {
        InstanceAuthRegistry filtered = new InstanceAuthRegistry(
                new FilteredClassLoader("cn.dev33.satoken.stp.StpUtil"));
        WsConfig config = new WsConfig();
        config.getAuth().setProvider(InstanceAuthRegistry.PROVIDER_SA_TOKEN_LOGIN);

        assertThat(filtered.availableProviders())
                .doesNotContain(InstanceAuthRegistry.PROVIDER_SA_TOKEN_LOGIN);
        assertThat(new WsAuthSupport(config, filtered).isAuthenticated("/ws/api", new LinkedHashMap<>()))
                .as("已配置但模块缺失：不回退，握手拒绝").isFalse();
    }

    @Test
    void 未知provider启动期校验抛非法参数() {
        WsConfig config = new WsConfig();
        config.getAuth().setProvider("custom");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> WsAuthSupport.validateProviders(config, registry))
                .withMessageContaining("unsupported authentication provider 'custom'");
    }

    @Test
    void 未知provider在命名实例上同样启动期报错() {
        WsConfig config = new WsConfig();
        WsConfig.WsInstance instance = new WsConfig.WsInstance();
        instance.setKey("report");
        instance.setPath("/ws/api2");
        instance.getAuth().setProvider("unknown");
        config.getServers().add(instance);

        assertThatIllegalArgumentException()
                .isThrownBy(() -> WsAuthSupport.validateProviders(config, registry))
                .withMessageContaining("unsupported authentication provider 'unknown'");
    }

    @Test
    void 空provider与空配置在启动期校验不报错() {
        WsConfig config = new WsConfig();
        config.setServers(Collections.singletonList(new WsConfig.WsInstance()));

        WsAuthSupport.validateProviders(config, registry);
        WsAuthSupport.validateProviders(null, registry);
    }

    @Test
    void query与header凭据合并且header优先() {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("satoken", "from-header");
        headers.put("Authorization", "Bearer abc");

        Map<String, String> merged = WsAuthSupport.mergeCredentials(
                "clientId=c1&satoken=from-query&apikey=qk", headers);

        assertThat(merged)
                .containsEntry("clientId", "c1")
                .containsEntry("apikey", "qk")
                .containsEntry("satoken", "from-header")
                .containsEntry("Authorization", "Bearer abc");
    }

    @Test
    void query凭据可单独读取() {
        assertThat(WsAuthSupport.mergeCredentials("token=q1", null)).containsEntry("token", "q1");
        assertThat(WsAuthSupport.mergeCredentials(null, null)).isEmpty();
        assertThat(WsAuthSupport.mergeCredentials("", null)).isEmpty();
    }
}