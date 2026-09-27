package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.SimpleWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBasePathProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBusinessDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsInvocationContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsSubscribeScript;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.lang.reflect.Field;
import java.util.Collections;
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
 * 非 magic 应用只引入 WS 地基模块时的验收测试：
 * endpoint 按 {@code fw.plus.ws.path} 与 {@code fw.plus.ws.paths} 注册，
 * 处理器与 SPI 自动装配；容器内多个基础路径提供者、宿主自带的 {@link WebSocketConfigurer}
 * 都与地基注册器并存。
 *
 * @author zzy
 */
class WsAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class, WsAutoConfiguration.class));

    @Test
    void shouldRegisterFoundationHandlerByDefault() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(AbstractWebSocketHandler.class);
            assertThat(context.getBean(AbstractWebSocketHandler.class)).isInstanceOf(SimpleWebSocketHandler.class);
            assertThat(context).hasSingleBean(WebSocketConfigurer.class);
        });
    }

    @Test
    void shouldNotRegisterAnythingWhenDisabled() {
        runner.withPropertyValues("fw.plus.ws.enabled=false").run(context -> {
            assertThat(context).doesNotHaveBean(AbstractWebSocketHandler.class);
            assertThat(context).doesNotHaveBean(WebSocketConfigurer.class);
        });
    }

    @Test
    void endpointPathShouldFollowConfiguredWebsocketPath() {
        runner.withPropertyValues("fw.plus.ws.path=/ws/custom").run(context -> {
            WebSocketHandlerRegistry registry = mockRegistry();

            context.getBean(WebSocketConfigurer.class).registerWebSocketHandlers(registry);

            assertThat(registeredPaths(registry)).containsExactly("/ws/custom");
        });
    }

    @Test
    void endpointPathShouldDefaultToSlashWsSlashApi() {
        runner.run(context -> {
            WebSocketHandlerRegistry registry = mockRegistry();

            context.getBean(WebSocketConfigurer.class).registerWebSocketHandlers(registry);

            assertThat(registeredPaths(registry)).containsExactly("/ws/api");
        });
    }

    @Test
    void endpointPathShouldBePrefixedByHostBasePathProvider() {
        runner.withBean(WsBasePathProvider.class, () -> () -> "/magic-api")
                .withPropertyValues("fw.plus.ws.path=ws/api")
                .run(context -> {
                    WebSocketHandlerRegistry registry = mockRegistry();

                    context.getBean(WebSocketConfigurer.class).registerWebSocketHandlers(registry);

                    assertThat(registeredPaths(registry)).containsExactly("/magic-api/ws/api");
                });
    }

    /**
     * A1：追加路径与主路径去重后逐条注册到同一个处理器上。
     */
    @Test
    void additionalPathsShouldRegisterEveryUniquePathOnSameHandler() {
        runner.withPropertyValues(
                "fw.plus.ws.path=/ws/api",
                "fw.plus.ws.paths=/ws/api2,/ws/api, ws/api3 ").run(context -> {
            WebSocketHandlerRegistry registry = mockRegistry();

            context.getBean(WebSocketConfigurer.class).registerWebSocketHandlers(registry);

            assertThat(registeredPaths(registry)).containsExactly("/ws/api", "/ws/api2", "/ws/api3");
        });
    }

    /**
     * A2：容器内存在多个基础路径提供者时，逐个与每条相对路径展开，
     * 不再因 {@code getIfAvailable()} 抛 {@code NoUniqueBeanDefinitionException}。
     */
    @Test
    void multipleBasePathProvidersShouldRegisterEveryCombination() {
        runner.withBean("magicApiBasePathProvider", WsBasePathProvider.class, () -> () -> "/magic-api")
                .withBean("bffBasePathProvider", WsBasePathProvider.class, () -> () -> "/bff")
                .withPropertyValues("fw.plus.ws.path=/ws/api")
                .run(context -> {
                    WebSocketHandlerRegistry registry = mockRegistry();

                    context.getBean(WebSocketConfigurer.class).registerWebSocketHandlers(registry);

                    assertThat(registeredPaths(registry))
                            .containsExactly("/magic-api/ws/api", "/bff/ws/api");
                });
    }

    /**
     * A3：宿主自带 {@link WebSocketConfigurer} 与地基注册器由 Spring 统一收集，两条路径都注册、互不覆盖。
     */
    @Test
    void hostSuppliedConfigurerShouldCoexistWithFoundationRegistrar() {
        runner.withUserConfiguration(HostWebSocketConfiguration.class).run(context -> {
            Map<String, WebSocketConfigurer> configurers = context.getBeansOfType(WebSocketConfigurer.class);
            assertThat(configurers).hasSize(2);

            WebSocketHandlerRegistry registry = mockRegistry();
            for (WebSocketConfigurer configurer : configurers.values()) {
                configurer.registerWebSocketHandlers(registry);
            }

            assertThat(registeredPaths(registry))
                    .containsExactlyInAnyOrder("/ws/api", "/host/echo");
        });
    }

    @Test
    void hostSuppliedHandlerShouldWinOverDefault() {
        AbstractWebSocketHandler custom = mock(AbstractWebSocketHandler.class);
        runner.withBean(AbstractWebSocketHandler.class, () -> custom).run(context -> {
            assertThat(context).hasSingleBean(AbstractWebSocketHandler.class);
            assertThat(context.getBean(AbstractWebSocketHandler.class)).isSameAs(custom);
        });
    }

    @Test
    void businessDispatcherShouldBeInjectedIntoFoundationHandler() throws Exception {
        WsBusinessDispatcher dispatcher = new WsBusinessDispatcher() {
            @Override
            public List<? extends WsSubscribeScript> findEnabledSubscribes(String appId, String businessType) {
                return Collections.emptyList();
            }

            @Override
            public Object execute(WsSubscribeScript subscribe, WsInvocationContext context) {
                return null;
            }
        };
        runner.withBean(WsBusinessDispatcher.class, () -> dispatcher).run(context -> {
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            Field field = AbstractWebSocketHandler.class.getDeclaredField("businessDispatcher");
            field.setAccessible(true);
            assertThat(field.get(handler)).isSameAs(dispatcher);
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

    @Configuration(proxyBeanMethods = false)
    static class HostWebSocketConfiguration {

        @Bean
        WebSocketConfigurer hostWebSocketConfigurer() {
            return registry -> registry.addHandler(new TextWebSocketHandler(), "/host/echo");
        }
    }
}
