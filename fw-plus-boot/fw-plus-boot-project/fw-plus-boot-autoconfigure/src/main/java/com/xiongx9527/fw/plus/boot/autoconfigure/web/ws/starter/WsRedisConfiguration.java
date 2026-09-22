package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.RedisWebSocketHandler;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.Map;

/**
 * Redis 跨实例广播自动配置。
 *
 * <p>容器内存在 {@code StringRedisTemplate} 时启用：WebSocket 处理器换成
 * {@link RedisWebSocketHandler}，并注册 Redis 频道监听容器，使广播跨实例生效。
 * 未引入 Redis 时该配置不生效，退化为单机广播。</p>
 *
 * @author zzy
 */
@Configuration
@ConditionalOnProperty(prefix = "fw.plus.ws", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.springframework.data.redis.core.StringRedisTemplate"})
@ConditionalOnBean(type = {"org.springframework.data.redis.core.StringRedisTemplate"})
@AutoConfigureAfter(name = {"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"})
public class WsRedisConfiguration {

    /**
     * 基于 Redis 的 WebSocket 处理器。宿主已自行提供处理器时不再创建。
     *
     * @param wsConfig WebSocket 通用配置
     * @return WebSocket 处理器
     */
    @Bean
    @ConditionalOnMissingBean(AbstractWebSocketHandler.class)
    public RedisWebSocketHandler redisWebSocketHandler(WsConfig wsConfig) {
        return new RedisWebSocketHandler(wsConfig);
    }

    /**
     * Redis 消息监听容器，用于处理其它实例广播过来的 WebSocket 消息。宿主已自行提供容器时不再创建。
     *
     * <p>处理器由 {@code redisWebSocketHandler} 提供时，容器在初始化阶段绑定其广播监听器；
     * 宿主自行提供处理器（不参与 Redis 广播）时容器不绑定监听器，因此这里不声明对
     * {@code redisWebSocketHandler} 的硬依赖。</p>
     *
     * @param wsConfig WebSocket 通用配置
     * @return 消息监听容器
     */
    @Bean
    @ConditionalOnMissingBean(WsRedisMessageListenerContainer.class)
    public WsRedisMessageListenerContainer redisMessageListenerContainer(WsConfig wsConfig) {
        return new WsRedisMessageListenerContainer(wsConfig);
    }

    /**
     * Redis 消息监听容器实现。
     */
    public static class WsRedisMessageListenerContainer extends RedisMessageListenerContainer
            implements ApplicationContextAware, WsBaseService {

        private final WsConfig wsConfig;

        private org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor taskExecutor;

        public WsRedisMessageListenerContainer(WsConfig wsConfig) {
            this.wsConfig = wsConfig;
        }

        @Override
        public Map<String, Object> getConfig() {
            return wsConfig == null ? WsBaseService.super.getConfig() : wsConfig.flatten();
        }

        /**
         * 设置应用程序上下文，并初始化Redis连接和消息监听器
         *
         * @param applicationContext 应用程序上下文对象
         */
        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            // 获取Redis连接工厂并设置连接
            RedisConnectionFactory redisConnectionFactory = applicationContext.getBean(RedisConnectionFactory.class);
            this.setConnectionFactory(redisConnectionFactory);

            // 使用有界线程池替代默认的SimpleAsyncTaskExecutor，避免集群高流量下线程爆炸
            this.taskExecutor = new org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor();
            this.taskExecutor.setCorePoolSize(4);
            this.taskExecutor.setMaxPoolSize(16);
            this.taskExecutor.setThreadNamePrefix("redis-ws-");
            this.taskExecutor.initialize();
            this.setTaskExecutor(this.taskExecutor);

            // 添加Redis消息监听器用于WebSocket消息处理
            // 宿主可能提供自定义处理器（此时不再创建 RedisWebSocketHandler），
            // 这类处理器不参与 Redis 广播，故仅在处理器确实为 Redis 实现时绑定监听器，避免启动期取 Bean 失败。
            AbstractWebSocketHandler webSocketHandler =
                    applicationContext.getBeanProvider(AbstractWebSocketHandler.class).getIfAvailable();
            if (webSocketHandler instanceof RedisWebSocketHandler) {
                this.addMessageListener(((RedisWebSocketHandler) webSocketHandler).getRedisMessageListener(),
                        new ChannelTopic(getBroadcastChannelName()));
            }
        }

        @Override
        public void destroy() throws Exception {
            super.destroy();
            if (this.taskExecutor != null) {
                this.taskExecutor.shutdown();
            }
        }
    }
}
