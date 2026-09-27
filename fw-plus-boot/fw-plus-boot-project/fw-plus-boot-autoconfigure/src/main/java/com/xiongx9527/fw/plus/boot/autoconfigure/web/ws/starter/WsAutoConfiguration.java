package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsInstanceValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.SimpleWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.WsHandshakeInterceptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBasePathProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBusinessDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.webflux.WsWebFluxAutoConfiguration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;

import java.util.ArrayList;
import java.util.Map;
import java.util.List;

/**
 * WebSocket 通用地基自动配置，WS 能力在 {@code spring.factories} 与
 * {@code AutoConfiguration.imports} 中的唯一登记入口。
 *
 * <p>Redis 跨实例广播（{@link WsRedisConfiguration}）与 webflux 栈端点
 * （{@link WsWebFluxAutoConfiguration}）由本类 {@link Import} 收编：两者仍保留各自的条件与
 * {@code fw.plus.ws.enabled} 总开关，装配结果与分别登记时一致，只是登记项从三个收敛为一个。</p>
 *
 * <p>本配置位于 {@code com.xiongx9527.fw.plus.boot.autoconfigure.web.ws} 包内，启用后即可获得
 * WebSocket 能力（端点注册、握手鉴权、会话管理、心跳、广播）。
 * 引入业务的 {@link WsBusinessDispatcher} 实现后，自动接入订阅解析与脚本执行；
 * 未提供实现时退化为纯 WebSocket 通道。</p>
 *
 * @author zzy
 */
@Configuration
@ConditionalOnProperty(prefix = "fw.plus.ws", name = "enabled", havingValue = "true")
@ConditionalOnClass(name = {"org.springframework.web.socket.WebSocketHandler"})
@EnableConfigurationProperties(WsConfig.class)
@AutoConfigureAfter(name = {"org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"})
@Import({WsRedisConfiguration.class, WsWebFluxAutoConfiguration.class})
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
     * 端点注册器：把处理器挂到 {@code fw.plus.ws.path} 上。
     *
     * <p>基础路径取容器内全部 {@link WsBasePathProvider}（宿主可在自身配置中声明该 Bean，
     * 也可声明多个），按 {@code @Primary} 与 {@code @Order}/{@code Ordered} 定序后逐个展开；
     * 一个都没有时按无基础路径处理。容器内存在多个候选时不再抛
     * {@code NoUniqueBeanDefinitionException}。</p>
     *
     * @param webSocketHandler         WebSocket 处理器
     * @param wsBasePathProviders      基础路径提供者候选（可为空）
     * @param wsConfig                 WebSocket 通用配置
     * @param wsHandshakeInterceptors  握手拦截器（可为空）
     * @return 端点注册器
     */
    @Bean
    public WebSocketConfigurer wsEndpointRegistrar(AbstractWebSocketHandler webSocketHandler,
                                                   ObjectProvider<WsBasePathProvider> wsBasePathProviders,
                                                   WsConfig wsConfig,
                                                   Map<String, WsHandshakeInterceptor> wsHandshakeInterceptors) {
        List<WsBasePathProvider> basePathProviders = new ArrayList<>();
        wsBasePathProviders.orderedStream().forEach(basePathProviders::add);
        return new WsEndpointRegistrar(wsConfig, webSocketHandler,
                basePathProviders, wsHandshakeInterceptors);
    }

    /**
     * 命名实例配置校验：{@code fw.plus.ws.servers} 的 key 必填且唯一、端点全局唯一，
     * 任一不满足都启动失败。
     *
     * <p>放在装配路径而不是 {@code registerWebSocketHandlers}：宿主未开启
     * {@code @EnableWebSocket} 时后者不会执行，做不到启动即失败。webflux 栈由
     * {@code WsWebFluxAutoConfiguration} 按同一方法各自校验一次。</p>
     *
     * @param wsConfig WebSocket 通用配置
     * @return 校验器实例
     */
    @Bean
    public WsInstanceValidator wsInstanceValidator(WsConfig wsConfig) {
        WsInstanceValidator.validate(wsConfig);
        return new WsInstanceValidator();
    }
}
