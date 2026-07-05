package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler.BaseDynamicWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler.MagicAPIHandshakeInterceptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler.SimpleDynamicWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module.WsModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeMagicDynamicRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.web.MagicWsController;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.*;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zzy
 */
@Slf4j
@ConditionalOnProperty(prefix = "magic-api.plugin.ws", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
@EnableConfigurationProperties(MagicAPIWsConfig.class)
@ConditionalOnClass(name={"org.springframework.web.socket.WebSocketHandler"})
@Import(MagicAPIWsHandlerConfiguration.class)
public class MagicAPIWsConfiguration implements MagicPluginConfiguration , BaseService , WebSocketConfigurer {

    /**
     * MagicAPI基础配置属性
     */
    private final MagicAPIProperties properties;

    /**
     * 存储魔法API握手拦截器的映射表
     *
     * 该字段用于保存所有已注册的MagicAPIHandshakeInterceptor拦截器实例，
     * 通过字符串键名进行索引，便于在握手过程中快速查找和使用对应的拦截器。
     *
     * key: 拦截器的唯一标识符
     * value: 对应的MagicAPIHandshakeInterceptor拦截器实例
     */
    private final Map<String, MagicAPIHandshakeInterceptor> magicAPIHandshakeInterceptorMap;


    /**
     * WebSocket配置类的构造函数
     *
     * @param properties WebSocket配置属性对象，用于初始化WebSocket相关配置
     * @param magicAPIHandshakeInterceptorMap WebSocket握手拦截器映射表，用于存储和管理握手拦截器
     */
    public MagicAPIWsConfiguration(MagicAPIProperties properties, Map<String, MagicAPIHandshakeInterceptor> magicAPIHandshakeInterceptorMap) {
        this.properties = properties;
        // 初始化握手拦截器映射表，如果传入参数为null则创建新的HashMap
        if (magicAPIHandshakeInterceptorMap == null) {
            this.magicAPIHandshakeInterceptorMap = new HashMap<>();
        } else {
            this.magicAPIHandshakeInterceptorMap = magicAPIHandshakeInterceptorMap;
        }
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
     * 创建简单的WebSocket处理器Bean（不依赖Redis）
     *
     * @param magicDataSourceStorage WebSocket数据源信息存储
     * @param wsSubscribeInfoMagicResourceStorage WebSocket订阅信息存储
     * @return SimpleDynamicWebSocketHandler实例
     */
    @Bean
    @DependsOn(value = {"wsDataSourceInfoMagicResourceStorage", "wsSubscribeInfoMagicResourceStorage"})
    @ConditionalOnMissingBean(type = {"org.springframework.data.redis.core.StringRedisTemplate"})
    public BaseDynamicWebSocketHandler simpleDynamicWebSocketHandler(WsDataSourceInfoMagicResourceStorage magicDataSourceStorage
            , WsSubscribeInfoMagicResourceStorage wsSubscribeInfoMagicResourceStorage) {
        return new SimpleDynamicWebSocketHandler(magicDataSourceStorage, wsSubscribeInfoMagicResourceStorage);
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

    /**
     * 创建WebSocket模块Bean
     *
     * @param baseDynamicWebSocketHandler WebSocket处理器
     * @return WsModule实例
     */
    @Bean
    public WsModule wsModule(BaseDynamicWebSocketHandler baseDynamicWebSocketHandler) {
        return new WsModule(baseDynamicWebSocketHandler,getDefaultKey());
    }


    /**
     * 注册WebSocket处理器及访问路径
     *
     * @param registry WebSocket处理器注册器
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String wsPath = getWebsocketPath();
        if (StringUtils.isNotBlank(wsPath) && !wsPath.startsWith("/")) {
            wsPath = "/" + wsPath;
        }

        // 构建完整的WebSocket路径，确保路径格式正确
        String fullPath = buildFullPath(wsPath);

        try {
            BaseDynamicWebSocketHandler handler = getWebSocketHandler();
            if (handler == null) {
                log.warn("WebSocket handler bean is not available.");
                return;
			}
			WebSocketHandlerRegistration registration = registry.addHandler(handler, fullPath);
			registration.setAllowedOrigins(getAllowedOrigins());
			String[] subProtocols = getSubProtocols();
			if (subProtocols.length > 0) {
				org.springframework.web.socket.server.support.DefaultHandshakeHandler handshakeHandler =
						new org.springframework.web.socket.server.support.DefaultHandshakeHandler();
				handshakeHandler.setSupportedProtocols(subProtocols);
				registration.setHandshakeHandler(handshakeHandler);
			}
			if (MapUtils.isNotEmpty(this.magicAPIHandshakeInterceptorMap)) {
                Collection<MagicAPIHandshakeInterceptor> interceptors = this.magicAPIHandshakeInterceptorMap.values();
                if (CollectionUtils.isNotEmpty(interceptors)) {
                    registration.addInterceptors(interceptors.toArray(new MagicAPIHandshakeInterceptor[0]));
                }
            }
        } catch (BeansException e) {
            log.error("Failed to retrieve WebSocket handler from Spring context for path: {}", fullPath, e);
        } catch (Exception e) {
            log.error("Failed to register WebSocket handler for path: {}", fullPath, e);
        }
    }

    /**
     * 构建完整的路径地址
     *
     * @param wsPath 工作路径，需要拼接到基础路径后面的相对路径
     * @return 返回拼接后的完整路径字符串
     */
    private String buildFullPath(String wsPath) {
        String webBasePath = properties != null && properties.getWeb() != null ? properties.getWeb() : "";

        // 标准化基础路径结尾是否带 "/"
        boolean baseEndsWithSlash = webBasePath.endsWith("/");
        boolean wsStartsWithSlash = wsPath.startsWith("/");

        StringBuilder sb = new StringBuilder(webBasePath.length() + wsPath.length() + 1);
        sb.append(webBasePath);

        // 处理路径连接处的斜杠，避免出现双斜杠或缺少斜杠的情况
        if (!baseEndsWithSlash && !wsStartsWithSlash) {
            sb.append('/');
        } else if (baseEndsWithSlash && wsStartsWithSlash) {
            sb.setLength(sb.length() - 1); // 移除重复斜杠
        }

        sb.append(wsPath);
        return sb.toString();
    }


    /**
     * 获取WebSocket处理器实例
     *
     * @return BaseDynamicWebSocketHandler WebSocket处理器实例
     * @throws BeansException 当无法从Spring容器中获取Bean时抛出此异常
     */
    private BaseDynamicWebSocketHandler getWebSocketHandler() throws BeansException {
        return SpringUtils.getBean(BaseDynamicWebSocketHandler.class);
    }

    
    /**
     * 获取允许访问WebSocket的域名列表
     * 
     * @return 允许访问的域名数组
     */
    private String[] getAllowedOrigins() {
        // 从配置中读取受信任的域名列表，支持多个域名用逗号分隔
        String trustedDomain = getTrustedDomain();
		if (StringUtils.isBlank(trustedDomain)) {
			log.warn("WebSocket未配置受信任域名，默认不允许跨域访问");
			return new String[0];
		}
		if ("*".equals(trustedDomain.trim())) {
			log.warn("WebSocket允许所有来源访问，生产环境建议配置具体的受信任域名");
			return new String[]{"*"};
		}
        
        // 将逗号分隔的域名列表拆分为数组，并清理空白元素
        String[] domains = trustedDomain.split(",");
        java.util.List<String> cleanedDomains = new java.util.ArrayList<>();
        for (String domain : domains) {
            String trimmedDomain = domain.trim();
            if (StringUtils.isNotBlank(trimmedDomain)) {
                cleanedDomains.add(trimmedDomain);
            }
        }
        
		// 如果清理后没有有效域名，则返回通配符并记录警告
		if (cleanedDomains.isEmpty()) {
			log.warn("WebSocket受信任域名配置存在问题，默认不允许跨域访问");
			return new String[0];
		}

        return cleanedDomains.toArray(new String[cleanedDomains.size()]);
    }

}
