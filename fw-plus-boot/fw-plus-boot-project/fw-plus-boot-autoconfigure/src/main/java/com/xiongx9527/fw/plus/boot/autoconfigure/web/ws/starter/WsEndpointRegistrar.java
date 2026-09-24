package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.starter;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsEndpointPathResolver;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 把 WebSocket 处理器注册到端点路径。
 *
 * <p>端点路径 = {@link WsBasePathProvider#getBasePath()} + {@code fw.plus.ws.websocket.path}，
 * 另外可按 {@code fw.plus.ws.websocket.paths} 在同一个处理器上追加若干路径，
 * 供多应用、多入口复用一套 WS 地基。容器内存在多个基础路径提供者时，
 * 每个基础路径与每条相对路径都会注册。路径拼接与跨域、子协议、握手拦截器的处理规则与拆分前保持一致。</p>
 *
 * <p>本类是公开类，宿主可以自行实例化并作为自己的 {@link WebSocketConfigurer} Bean 注册；
 * Spring 的 {@code DelegatingWebSocketConfiguration} 会收集容器内全部 {@link WebSocketConfigurer}，
 * 宿主的注册器与地基注册器因此并存生效，互不覆盖。</p>
 *
 * @author zzy
 */
@Slf4j
public class WsEndpointRegistrar implements WebSocketConfigurer, WsBaseService {

    private final WsConfig wsConfig;

    private final AbstractWebSocketHandler webSocketHandler;

    private final List<WsBasePathProvider> basePathProviders;

    private final Map<String, WsHandshakeInterceptor> handshakeInterceptorMap;

    /**
     * 单个基础路径提供者时的构造入口，等价于把该提供者作为唯一候选。
     *
     * @param wsConfig                WebSocket 通用配置
     * @param webSocketHandler        WebSocket 处理器
     * @param basePathProvider        基础路径提供者，可为空
     * @param handshakeInterceptorMap 握手拦截器
     */
    public WsEndpointRegistrar(WsConfig wsConfig,
                               AbstractWebSocketHandler webSocketHandler,
                               WsBasePathProvider basePathProvider,
                               Map<String, WsHandshakeInterceptor> handshakeInterceptorMap) {
        this(wsConfig, webSocketHandler,
                basePathProvider == null ? Collections.<WsBasePathProvider>emptyList()
                        : Collections.singletonList(basePathProvider),
                handshakeInterceptorMap);
    }

    /**
     * 多个基础路径提供者时的构造入口：候选按容器定序（{@code @Primary} 优先，其余按
     * {@code @Order}、{@code Ordered}），逐个与全部相对路径展开成端点。
     *
     * @param wsConfig                WebSocket 通用配置
     * @param webSocketHandler        WebSocket 处理器
     * @param basePathProviders       基础路径提供者候选，可为空
     * @param handshakeInterceptorMap 握手拦截器
     */
    public WsEndpointRegistrar(WsConfig wsConfig,
                               AbstractWebSocketHandler webSocketHandler,
                               List<WsBasePathProvider> basePathProviders,
                               Map<String, WsHandshakeInterceptor> handshakeInterceptorMap) {
        this.wsConfig = wsConfig;
        this.webSocketHandler = webSocketHandler;
        this.basePathProviders = basePathProviders == null
                ? Collections.<WsBasePathProvider>emptyList() : basePathProviders;
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
        if (webSocketHandler == null) {
            log.warn("WebSocket handler bean is not available.");
            return;
        }
        for (String fullPath : resolveFullPaths()) {
            registerEndpoint(registry, fullPath);
        }
    }

    /**
     * 注册单个端点：跨域、子协议与握手拦截器对每条路径都同等生效。
     *
     * @param registry WebSocket处理器注册器
     * @param fullPath 完整端点路径
     */
    private void registerEndpoint(WebSocketHandlerRegistry registry, String fullPath) {
        try {
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
     * 待注册的相对路径：主路径取 {@code websocket.path}，追加路径取 {@code websocket.paths}，按顺序去重。
     *
     * @return 相对路径列表
     */
    private List<String> resolveRelativePaths() {
        List<String> additionalPaths = wsConfig == null ? null : wsConfig.getWebsocket().getPaths();
        return WsEndpointPathResolver.resolveRelativePaths(getWebsocketPath(), additionalPaths);
    }

    /**
     * 待注册的完整路径：相对路径与全部基础路径候选做笛卡尔积后去重，与 webflux 栈口径一致。
     *
     * @return 完整端点路径列表
     */
    private List<String> resolveFullPaths() {
        return WsEndpointPathResolver.resolveFullPaths(resolveRelativePaths(), collectBasePaths());
    }

    /**
     * 收集基础路径候选：空值（空串）的提供者视为不提供基础路径，不参与展开。
     *
     * @return 基础路径列表，容器内没有提供者时为空列表
     */
    private List<String> collectBasePaths() {
        List<String> basePaths = new ArrayList<>();
        for (WsBasePathProvider provider : basePathProviders) {
            if (provider == null) {
                continue;
            }
            String basePath = WsEndpointPathResolver.normalizeBasePath(provider.getBasePath());
            if (!basePath.isEmpty()) {
                basePaths.add(basePath);
            }
        }
        return basePaths;
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
