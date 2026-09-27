package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler.BaseDynamicWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module.WsModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.web.MagicWsController;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBasePathProvider;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBusinessDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

/**
 * magic-api WS 插件装配。
 *
 * <p>连接/鉴权/会话/心跳/广播与端点注册由本模块
 * {@code com.xiongx9527.fw.plus.boot.autoconfigure.web.ws} 包承担；本配置只装配
 * magic 侧资源（订阅与数据源存储、动态注册器、magic 模块与控制器），并以
 * {@link WsBusinessDispatcher} Bean 的形式把 magic 语义接回地基。</p>
 *
 * @author zzy
 */
@Slf4j
@ConditionalOnProperty(prefix = "fw.plus.ws", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@ConditionalOnClass(name = {"org.springframework.web.socket.WebSocketHandler"})
public class MagicAPIWsConfiguration implements MagicPluginConfiguration, BaseService {

    /**
     * MagicAPI基础配置属性
     */
    private final MagicAPIProperties properties;

    /**
     * WebSocket配置类的构造函数
     *
     * @param properties WebSocket配置属性对象，用于初始化WebSocket相关配置
     */
    public MagicAPIWsConfiguration(MagicAPIProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建WebSocket数据源信息存储Bean
     *
     * @return WsDataSourceInfoMagicResourceStorage实例
     */
    @Bean
    public WsDataSourceInfoMagicResourceStorage wsDataSourceInfoMagicResourceStorage() {
        return new WsDataSourceInfoMagicResourceStorage();
    }

    /**
     * 创建WebSocket订阅信息存储Bean
     *
     * @return WsSubscribeInfoMagicResourceStorage实例
     */
    @Bean
    public WsSubscribeInfoMagicResourceStorage wsSubscribeInfoMagicResourceStorage() {
        return new WsSubscribeInfoMagicResourceStorage();
    }

    /**
     * 创建 magic 侧业务调度 Bean，由 WS 地基的处理器自动注入。
     *
     * @param magicDataSourceStorage           WebSocket数据源信息存储
     * @param wsSubscribeInfoMagicResourceStorage WebSocket订阅信息存储
     * @return 业务调度实现
     */
    @Bean
    @DependsOn(value = {"wsDataSourceInfoMagicResourceStorage", "wsSubscribeInfoMagicResourceStorage"})
    public WsBusinessDispatcher wsBusinessDispatcher(WsDataSourceInfoMagicResourceStorage magicDataSourceStorage,
                                                     WsSubscribeInfoMagicResourceStorage wsSubscribeInfoMagicResourceStorage) {
        return new BaseDynamicWebSocketHandler(magicDataSourceStorage, wsSubscribeInfoMagicResourceStorage);
    }

    /**
     * 创建基础路径提供者，沿用 magic-api 的 web 上下文路径（{@code magic-api.web}）。
     *
     * <p>端点本身由 WS 地基的注册器按「基础路径 + {@code fw.plus.ws.path}」拼接，
     * 与拆分前的路径规则一致。</p>
     *
     * @return 基础路径提供者
     */
    @Bean
    public WsBasePathProvider wsBasePathProvider() {
        return () -> properties != null ? properties.getWeb() : "";
    }

    /**
     * 创建WebSocket数据源信息动态注册器Bean
     *
     * @param wsDataSourceInfoMagicResourceStorage WebSocket数据源信息存储
     * @return WsDataSourceMagicDynamicRegistry实例
     */
    @Bean
    @DependsOn("wsDataSourceInfoMagicResourceStorage")
    public WsDataSourceMagicDynamicRegistry wsDataSourceMagicDynamicRegistry(
            WsDataSourceInfoMagicResourceStorage wsDataSourceInfoMagicResourceStorage) {
        return new WsDataSourceMagicDynamicRegistry(wsDataSourceInfoMagicResourceStorage);
    }

    /**
     * 创建WebSocket订阅信息动态注册器Bean
     *
     * @param wsSubscribeInfoMagicResourceStorage WebSocket订阅信息存储
     * @return WsSubscribeMagicDynamicRegistry实例
     */
    @Bean
    @DependsOn("wsDataSourceMagicDynamicRegistry")
    public WsSubscribeMagicDynamicRegistry wsSubscribeMagicDynamicRegistry(
            WsSubscribeInfoMagicResourceStorage wsSubscribeInfoMagicResourceStorage) {
        return new WsSubscribeMagicDynamicRegistry(wsSubscribeInfoMagicResourceStorage);
    }

    /**
     * 创建WebSocket模块Bean
     *
     * <p>依赖 {@code springUtils}：默认数据源 key 由 {@link #getDefaultKey()} 从配置读取，
     * 而配置读取走 {@code SpringUtils.getBean}。拆分前该顺序由 handler Bean 的
     * {@code @DependsOn("springUtils")} 间接保证，handler 下沉到公共模块后需在此显式声明。</p>
     *
     * @param webSocketHandler WS 地基处理器
     * @return WsModule实例
     */
    @Bean
    @DependsOn("springUtils")
    public WsModule wsModule(AbstractWebSocketHandler webSocketHandler) {
        return new WsModule(webSocketHandler, getDefaultKey());
    }

    /**
     * 获取WebSocket插件信息
     *
     * @return Plugin实例，包含插件的标识、名称和JavaScript文件路径
     */
    @Override
    public Plugin plugin() {
        return new Plugin("ws", "MagicWs", "magic-ws.1.0.0.iife.js");
    }

    /**
     * 获取MagicAPI控制器注册器
     *
     * @return MagicControllerRegister实例，用于注册Magic WebSocket控制器
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) -> mapping.registerController(new MagicWsController(configuration));
    }

}
