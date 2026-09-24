package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.WsHandshakeInterceptor;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBasePathProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistration;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 把 WebSocket 处理器注册到端点路径。
 *
 * <p>端点路径 = {@link WsBasePathProvider#getBasePath()} + {@code fw.plus.ws.websocket.path}，
 * 路径拼接与跨域、子协议、握手拦截器的处理规则与拆分前保持一致。</p>
 *
 * @author zzy
 */
@Slf4j
public class WsEndpointRegistrar implements WebSocketConfigurer, WsBaseService {

    private final WsConfig wsConfig;

    private final AbstractWebSocketHandler webSocketHandler;

    private final WsBasePathProvider basePathProvider;

    private final Map<String, WsHandshakeInterceptor> handshakeInterceptorMap;

    public WsEndpointRegistrar(WsConfig wsConfig,
                               AbstractWebSocketHandler webSocketHandler,
                               WsBasePathProvider basePathProvider,
                               Map<String, WsHandshakeInterceptor> handshakeInterceptorMap) {
        this.wsConfig = wsConfig;
        this.webSocketHandler = webSocketHandler;
        this.basePathProvider = basePathProvider;
        this.handshakeInterceptorMap = handshakeInterceptorMap;
    }

    /**
     * 构造注入的配置优先，避免依赖宿主容器的静态工具 Bean。
     */
    @Override
    public Map<String, Object> getConfig() {
        return wsConfig == null ? WsBaseService.super.getConfig() : wsConfig.flatten();
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
            if (webSocketHandler == null) {
                log.warn("WebSocket handler bean is not available.");
                return;
            }
            WebSocketHandlerRegistration registration = registry.addHandler(webSocketHandler, fullPath);
            registration.setAllowedOrigins(getAllowedOrigins());
            String[] subProtocols = getSubProtocols();
            if (subProtocols.length > 0) {
                DefaultHandshakeHandler handshakeHandler = new DefaultHandshakeHandler();
                handshakeHandler.setSupportedProtocols(subProtocols);
                registration.setHandshakeHandler(handshakeHandler);
            }
            if (MapUtils.isNotEmpty(this.handshakeInterceptorMap)) {
                Collection<WsHandshakeInterceptor> interceptors = this.handshakeInterceptorMap.values();
                if (CollectionUtils.isNotEmpty(interceptors)) {
                    registration.addInterceptors(interceptors.toArray(new WsHandshakeInterceptor[0]));
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
        String webBasePath = basePathProvider != null ? basePathProvider.getBasePath() : null;
        if (webBasePath == null) {
            webBasePath = "";
        }

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
        List<String> cleanedDomains = new ArrayList<>();
        for (String domain : domains) {
            String trimmedDomain = domain.trim();
            if (StringUtils.isNotBlank(trimmedDomain)) {
                cleanedDomains.add(trimmedDomain);
            }
        }

        // 如果清理后没有有效域名，则返回空数组并记录警告
        if (cleanedDomains.isEmpty()) {
            log.warn("WebSocket受信任域名配置存在问题，默认不允许跨域访问");
            return new String[0];
        }

        return cleanedDomains.toArray(new String[cleanedDomains.size()]);
    }
}
