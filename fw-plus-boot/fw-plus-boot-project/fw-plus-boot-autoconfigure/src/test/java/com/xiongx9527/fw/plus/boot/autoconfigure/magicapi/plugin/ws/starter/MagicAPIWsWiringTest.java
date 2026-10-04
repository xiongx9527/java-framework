package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler.BaseDynamicWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler.MagicAPIHandshakeInterceptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module.WsModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.SimpleWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBusinessDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter.WsAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter.WsEndpointRegistrar;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * magic 侧端到端接线验收：地基由公共模块提供，magic 只通过 SPI 接入，且行为语义不变。
 *
 * @author zzy
 */
class MagicAPIWsWiringTest {

    private final MagicAPIProperties properties = new MagicAPIProperties();

    // fw.plus.ws.enabled 无 matchIfMissing：未配置时装配关闭，测试统一显式开启。
    //
    // 注册顺序必须让 WS 地基先于 magic 侧插件配置被解析：MagicAPIWsConfiguration 用
    // @ConditionalOnBean(AbstractWebSocketHandler.class) 决定是否装配，而该条件只能看到
    // "此刻已解析完的配置"里声明过的 @Bean。生产里这条顺序由
    // MagicAPIBaseAutoConfiguration 上的 @AutoConfigureAfter(WsAutoConfiguration.class) 保证；
    // 这里用 withUserConfiguration 的先后顺序复现同一前提。若把 WsAutoConfiguration 放进
    // AutoConfigurations.of(...)，它会成为延迟自动配置（在用户配置之后解析），
    // MagicAPIWsConfiguration 会被静默跳过，wsModule bean 数为 0。
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withPropertyValues("fw.plus.ws.enabled=true")
            .withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class))
            .withUserConfiguration(WsAutoConfiguration.class, MagicAPIWsConfiguration.class)
            .withBean(MagicAPIProperties.class, () -> properties)
            .withBean(SpringUtils.class);

    @Test
    void magicShouldOnlySupplyBusinessDispatcher() throws Exception {
        runner.run(context -> {
            assertThat(context).hasSingleBean(WsBusinessDispatcher.class);
            assertThat(context.getBean(WsBusinessDispatcher.class)).isInstanceOf(BaseDynamicWebSocketHandler.class);

            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            Field field = AbstractWebSocketHandler.class.getDeclaredField("businessDispatcher");
            field.setAccessible(true);
            assertThat(field.get(handler)).isSameAs(context.getBean(WsBusinessDispatcher.class));
        });
    }

    @Test
    void foundationHandlerShouldNotBeReplacedByMagic() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(AbstractWebSocketHandler.class);
            assertThat(context.getBean(AbstractWebSocketHandler.class)).isInstanceOf(SimpleWebSocketHandler.class);
        });
    }

    @Test
    void magicResourcesAndModuleShouldStillBeRegistered() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(WsDataSourceInfoMagicResourceStorage.class);
            assertThat(context).hasSingleBean(WsSubscribeInfoMagicResourceStorage.class);
            assertThat(context).hasSingleBean(WsDataSourceMagicDynamicRegistry.class);
            assertThat(context).hasSingleBean(WsSubscribeMagicDynamicRegistry.class);
            assertThat(context).hasSingleBean(WsModule.class);
        });
    }

    @Test
    void endpointPathShouldUseMagicWebBasePath() {
        properties.setWeb("/magic-api");
        runner.run(context -> {
            WebSocketHandlerRegistry registry = mockRegistry();

            context.getBean(WebSocketConfigurer.class).registerWebSocketHandlers(registry);

            assertThat(registeredPaths(registry)).containsExactly("/magic-api/ws/api");
        });
    }

	/**
	 * A1 的 magic 侧证据：追加路径与 {@code magic.api.web} 推导出的基础路径共同展开。
	 */
	@Test
	void additionalPathsShouldRegisterAlongsideMagicWebBasePath() {
		properties.setWeb("/magic-api");
		runner.withPropertyValues("fw.plus.ws.paths=/ws/api2,/ws/api")
				.run(context -> {
					WebSocketHandlerRegistry registry = mockRegistry();

					context.getBean(WebSocketConfigurer.class).registerWebSocketHandlers(registry);

					assertThat(registeredPaths(registry))
							.containsExactly("/magic-api/ws/api", "/magic-api/ws/api2");
				});
	}

	/**
	 * 命名实例是默认实例之外的纯增量：实例端点与默认端点一起按基础路径展开注册。
	 */
	@Test
	void instanceEndpointShouldRegisterAlongsideDefaultPath() {
		properties.setWeb("/magic-api");
		runner.withPropertyValues(
						"fw.plus.ws.servers[0].key=report",
						"fw.plus.ws.servers[0].path=/ws/api2",
						"fw.plus.ws.servers[0].paths=/ws/api3",
						"fw.plus.ws.servers[0].appId=report")
				.run(context -> {
					WebSocketHandlerRegistry registry = mockRegistry();

					context.getBean(WebSocketConfigurer.class).registerWebSocketHandlers(registry);

					assertThat(registeredPaths(registry)).containsExactly(
							"/magic-api/ws/api", "/magic-api/ws/api2", "/magic-api/ws/api3");
				});
	}

	@Test
	void magicHandshakeInterceptorShouldBeAttachedToEndpoint() throws Exception {
        properties.setWeb("");
        HandshakeInterceptor interceptor = mock(MagicAPIHandshakeInterceptor.class);
        runner.withBean(MagicAPIHandshakeInterceptor.class, () -> (MagicAPIHandshakeInterceptor) interceptor)
                .run(context -> {
                    WebSocketConfigurer registrar = context.getBean(WebSocketConfigurer.class);
                    Field field = WsEndpointRegistrar.class.getDeclaredField("handshakeInterceptorMap");
                    field.setAccessible(true);
                    Map<String, ?> interceptorMap = (Map<String, ?>) field.get(registrar);

                    assertThat(interceptorMap.values()).containsExactly(interceptor);
                });
    }

    private static WebSocketHandlerRegistry mockRegistry() {
        WebSocketHandlerRegistry registry = mock(WebSocketHandlerRegistry.class);
        WebSocketHandlerRegistration registration = mock(WebSocketHandlerRegistration.class);
        when(registration.setAllowedOrigins(any())).thenReturn(registration);
        when(registry.addHandler(any(), anyString())).thenReturn(registration);
        return registry;
    }

    private static List<String> registeredPaths(WebSocketHandlerRegistry registry) {
        ArgumentCaptor<String> pathCaptor = ArgumentCaptor.forClass(String.class);
        verify(registry, atLeastOnce()).addHandler(any(), pathCaptor.capture());
        return pathCaptor.getAllValues();
    }
}
