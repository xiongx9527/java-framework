package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.SimpleWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.WsHandshakeInterceptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBasePathProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBusinessDispatcher;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

import java.util.Map;

/**
 * WebSocket 通用地基自动配置。
 *
 * <p>本配置位于 {@code com.xiongx9527.fw.plus.boot.autoconfigure.web.ws} 包内，启用后即可获得
 * WebSocket 能力（端点注册、握手鉴权、会话管理、心跳、广播）。
 * 引入业务的 {@link WsBusinessDispatcher} 实现后，自动接入订阅解析与脚本执行；
 * 未提供实现时退化为纯 WebSocket 通道。</p>
 *
 * @author zzy
 */
@Configuration
@ConditionalOnProperty(prefix = "fw.plus.ws", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"org.springframework.web.socket.WebSocketHandler"})
@EnableConfigurationProperties(WsConfig.class)
@AutoConfigureAfter(name = {"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"})
public class WsAutoConfiguration {

    /**
     * 单机 WebSocket 处理器：容器内没有 Redis 时使用。
     *
     * @param wsConfig WebSocket 通用配置
     * @return WebSocket 处理器
     */
    @Bean
    @ConditionalOnMissingBean(value = AbstractWebSocketHandler.class,
            type = {"org.springframework.data.redis.core.StringRedisTemplate"})
    public AbstractWebSocketHandler webSocketHandler(WsConfig wsConfig) {
        return new SimpleWebSocketHandler(wsConfig);
    }

    /**
     * 端点注册器：把处理器挂到 {@code websocket.path} 上。
     *
     * <p>基础路径优先取宿主提供的 {@link WsBasePathProvider}（宿主可在自身配置中声明该 Bean），
     * 未提供时回落到 {@code fw.plus.ws.websocket.basePath}。</p>
     *
     * @param webSocketHandler         WebSocket 处理器
     * @param wsBasePathProvider       基础路径提供者（可选）
     * @param wsConfig                 WebSocket 通用配置
     * @param wsHandshakeInterceptors  握手拦截器（可为空）
     * @return 端点注册器
     */
    @Bean
    public WebSocketConfigurer wsEndpointRegistrar(AbstractWebSocketHandler webSocketHandler,
                                                   ObjectProvider<WsBasePathProvider> wsBasePathProvider,
                                                   WsConfig wsConfig,
                                                   Map<String, WsHandshakeInterceptor> wsHandshakeInterceptors) {
        return new WsEndpointRegistrar(wsConfig, webSocketHandler,
                wsBasePathProvider.getIfAvailable(), wsHandshakeInterceptors);
    }
}
