package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter;

import com.xiongx9527.fw.plus.auth.core.CustomAuthService;
import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.auth.WsAuthHandshakeInterceptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.auth.WsAuthSupport;
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
// auth-core 是 optional 依赖：缺少时 WS 基建不可用（握手鉴权强依赖 InstanceAuthRegistry），
// 且 @Bean 签名里的 InstanceAuthRegistry 会在配置类内省阶段触发 NoClassDefFoundError，
// 因此随 ws.enabled 一并纳入 classpath 条件
@ConditionalOnClass(name = {"org.springframework.web.socket.WebSocketHandler",
        "com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry"})
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
    public WsInstanceValidator wsInstanceValidator(WsConfig wsConfig, InstanceAuthRegistry instanceAuthRegistry) {
        WsInstanceValidator.validate(wsConfig);
        WsAuthSupport.validateProviders(wsConfig, instanceAuthRegistry);
        return new WsInstanceValidator();
    }

    /**
     * 实例认证内核：容器已有（如同时启用 MCP）时不重复创建，供 WS 握手按实例解析 provider。
     * <p>
     * 业务提供的 {@link CustomAuthService} Bean 在此注册为自定义 provider：
     * {@code fw.plus.ws.servers[].auth.provider} 指向其 {@code getProviderKey()} 时，
     * {@link WsAuthSupport} 经 {@link InstanceAuthRegistry#resolve} 走业务实现。
     * 本 Bean 与 MCP 侧互斥共享（{@code @ConditionalOnMissingBean}），自定义 provider 只注册一次。
     * </p>
     *
     * @param customAuthServices 业务自定义认证实现（可为空）
     * @return 实例认证注册表
     */
    @Bean
    @ConditionalOnMissingBean
    public InstanceAuthRegistry instanceAuthRegistry(ObjectProvider<CustomAuthService> customAuthServices) {
        InstanceAuthRegistry registry = new InstanceAuthRegistry();
        customAuthServices.orderedStream()
                .forEach(service -> registry.registerProvider(service.getProviderKey(), service));
        return registry;
    }

    /**
     * webmvc 握手认证拦截器：升级前按命中实例的 provider 校验凭据，未通过置 HTTP 401 不升级。
     *
     * <p>启动期顺带校验全部实例 provider 是否受支持，未知 provider 直接抛
     * {@link IllegalArgumentException} 使应用启动失败。</p>
     *
     * @param wsConfig              WebSocket 通用配置
     * @param instanceAuthRegistry  实例认证注册表
     * @return 握手认证拦截器
     */
    @Bean
    @ConditionalOnMissingBean(name = "wsAuthHandshakeInterceptor")
    public WsHandshakeInterceptor wsAuthHandshakeInterceptor(WsConfig wsConfig,
                                                             InstanceAuthRegistry instanceAuthRegistry) {
        WsAuthSupport.validateProviders(wsConfig, instanceAuthRegistry);
        return new WsAuthHandshakeInterceptor(new WsAuthSupport(wsConfig, instanceAuthRegistry));
    }
}
