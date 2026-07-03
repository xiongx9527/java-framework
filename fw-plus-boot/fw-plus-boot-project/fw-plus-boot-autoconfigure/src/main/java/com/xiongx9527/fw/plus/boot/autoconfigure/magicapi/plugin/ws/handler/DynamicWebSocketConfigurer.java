package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.starter.MagicAPIWsConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Slf4j
public class DynamicWebSocketConfigurer implements WebSocketConfigurer , BaseService {

    /**
     * WebSocket处理器实例，用于处理WebSocket连接和消息
     */
    private final BaseDynamicWebSocketHandler dynamicWebSocketHandler;

    /**
     * WebSocket动态配置器构造函数
     * 
     * @param dynamicWebSocketHandler WebSocket处理器实例
     * @param config WebSocket配置信息
     */
    public DynamicWebSocketConfigurer(BaseDynamicWebSocketHandler dynamicWebSocketHandler, MagicAPIWsConfig config) {
        this.dynamicWebSocketHandler = dynamicWebSocketHandler;
    }

    /**
     * 注册WebSocket处理器及访问路径
     * 
     * @param registry WebSocket处理器注册器，用于注册WebSocket处理器和配置访问路径
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String wsEndpoint = getWsEndpointPath();
        registry.addHandler(dynamicWebSocketHandler, wsEndpoint)
                .setAllowedOriginPatterns("*");
    }
    
    /**
     * 从配置中获取WebSocket端点路径
     * 
     * @return WebSocket端点路径
     */
    private String getWsEndpointPath() {
        try {
            Object endpointObj = getWebsocketEndpoint();
            if (endpointObj != null && !endpointObj.toString().isEmpty()) {
                String endpoint = endpointObj.toString();
                // 确保路径以"/"开头
                if (!endpoint.startsWith("/")) {
                    endpoint = "/" + endpoint;
                }
                return endpoint;
            }
        } catch (Exception e) {
            log.warn("Failed to get WebSocket endpoint from config, using default: /ws/execute");
        }
        return "/ws/execute";
    }
}
