package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.RedisWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.SimpleWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.redis.WsRedisMessageListenerContainer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.autoconfigure.RefreshAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.Subscription;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 非 magic 应用在 Redis 可用 / 不可用两种情形下，WS 地基的装配验收测试。
 *
 * <p>Redis 可用时：处理器切换为 {@link RedisWebSocketHandler}，并注册把广播监听器
 * 绑定到 {@code fw.plus.ws.redis.channel.broadcast} 频道的监听容器，使跨实例广播生效；
 * Redis 缺失时退化为单机处理器。测试走完整 Spring 容器装配路径，不依赖真实 Redis 实例。</p>
 *
 * @author zzy
 */
class WsRedisConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RefreshAutoConfiguration.class,
                    WsAutoConfiguration.class, WsRedisConfiguration.class));

    /**
     * 组装一个"Redis 可用"的上下文。
     *
     * <p>本地无真实 Redis 实例，监听容器一旦自动启动会阻塞等待订阅注册直至超时，
     * 因此用 {@link NonStartingContainerConfiguration} 提供一个不自动启动的容器实现。
     * 监听器绑定发生在 Bean 初始化阶段（{@code setApplicationContext}），与是否启动无关，
     * 仍被完整断言。</p>
     *
     * @return 装配好的上下文运行器
     */
    private ApplicationContextRunner redisPresentRunner() {
        return runner
                .withBean(StringRedisTemplate.class, () -> mock(StringRedisTemplate.class))
                .withBean(RedisConnectionFactory.class, WsRedisConfigurationTest::mockConnectionFactory)
                .withUserConfiguration(NonStartingContainerConfiguration.class);
    }

    @Test
    void redisPresentShouldSwapHandlerAndBindBroadcastChannel() {
        redisPresentRunner().run(context -> {
            assertThat(context).hasSingleBean(AbstractWebSocketHandler.class);
            AbstractWebSocketHandler handler = context.getBean(AbstractWebSocketHandler.class);
            assertThat(handler).isInstanceOf(RedisWebSocketHandler.class);
            assertThat(handler).isNotInstanceOf(SimpleWebSocketHandler.class);

            assertThat(context).hasSingleBean(WsRedisMessageListenerContainer.class);
            RedisMessageListenerContainer container =
                    context.getBean(WsRedisMessageListenerContainer.class);

            assertThat(boundTopics(container)).containsExactly("websocket:broadcast");
            assertThat(boundListeners(container))
                    .containsExactly(((RedisWebSocketHandler) handler).getRedisMessageListener());
        });
    }

    @Test
    void configuredBroadcastChannelShouldDriveSubscription() {
        redisPresentRunner().withPropertyValues("fw.plus.ws.redis.channel.broadcast=app:ws:broadcast")
                .run(context -> {
                    RedisMessageListenerContainer container =
                            context.getBean(WsRedisMessageListenerContainer.class);

                    assertThat(boundTopics(container)).containsExactly("app:ws:broadcast");
                });
    }

    @Test
    void redisAbsentShouldFallBackToSingleNodeHandler() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(AbstractWebSocketHandler.class);
            assertThat(context.getBean(AbstractWebSocketHandler.class)).isInstanceOf(SimpleWebSocketHandler.class);
            assertThat(context).doesNotHaveBean(WsRedisMessageListenerContainer.class);
        });
    }

    @Test
    void hostSuppliedHandlerShouldNotBeReplacedByRedisHandler() {
        AbstractWebSocketHandler custom = mock(AbstractWebSocketHandler.class);
        redisPresentRunner().withBean(AbstractWebSocketHandler.class, () -> custom).run(context -> {
            assertThat(context.getBean(AbstractWebSocketHandler.class)).isSameAs(custom);
            RedisMessageListenerContainer container =
                    context.getBean(WsRedisMessageListenerContainer.class);
            assertThat(boundTopics(container)).isEmpty();
        });
    }

    /**
     * 提供一个关闭自动启动的监听容器实现：本地无 Redis，start() 会阻塞等待订阅注册直至超时。
     */
    @Configuration(proxyBeanMethods = false)
    static class NonStartingContainerConfiguration {

        @Bean
        WsRedisMessageListenerContainer redisMessageListenerContainer(WsConfig wsConfig,
                                                                      RedisConnectionFactory connectionFactory,
                                                                      ObjectProvider<AbstractWebSocketHandler> handlerProvider) {
            return new NonStartingContainer(wsConfig, connectionFactory, handlerProvider);
        }
    }

    /**
     * 覆写 {@code isAutoStartup()} 的监听容器，仅供无 Redis 环境下的装配测试使用。
     */
    static class NonStartingContainer extends WsRedisMessageListenerContainer {

        NonStartingContainer(WsConfig wsConfig,
                             RedisConnectionFactory connectionFactory,
                             ObjectProvider<AbstractWebSocketHandler> handlerProvider) {
            super(wsConfig, connectionFactory, handlerProvider);
        }

        @Override
        public boolean isAutoStartup() {
            return false;
        }
    }

    private static RedisConnectionFactory mockConnectionFactory() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);
        RedisConnection connection = mock(RedisConnection.class);
        when(connectionFactory.getConnection()).thenReturn(connection);
        when(connection.getSubscription()).thenReturn(mock(Subscription.class));
        return connectionFactory;
    }

    private static List<String> boundTopics(RedisMessageListenerContainer container) throws Exception {
        List<String> topics = new ArrayList<>();
        for (Set<Topic> bound : listenerTopics(container).values()) {
            for (Topic topic : bound) {
                topics.add(topic.getTopic());
            }
        }
        return topics;
    }

    private static List<Object> boundListeners(RedisMessageListenerContainer container) throws Exception {
        return new ArrayList<>(listenerTopics(container).keySet());
    }

    @SuppressWarnings("unchecked")
    private static Map<Object, Set<Topic>> listenerTopics(RedisMessageListenerContainer container)
            throws Exception {
        Class<?> type = container.getClass();
        while (type != null) {
            try {
                Field field = type.getDeclaredField("listenerTopics");
                field.setAccessible(true);
                return (Map<Object, Set<Topic>>) field.get(container);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        throw new NoSuchFieldException("listenerTopics");
    }
}
