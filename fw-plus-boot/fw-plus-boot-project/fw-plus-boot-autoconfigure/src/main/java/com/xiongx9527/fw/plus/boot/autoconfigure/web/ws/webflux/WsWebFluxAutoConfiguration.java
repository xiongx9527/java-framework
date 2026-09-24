package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBasePathProvider;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.Map;

/**
 * WebFlux 栈的 WebSocket 自动配置。
 *
 * <p>仅在响应式 Web 应用中生效；Servlet 栈由 {@code WsAutoConfiguration} 负责。
 * 两个栈共用同一套 {@code fw.plus.ws.*} 配置与订阅 SPI，行为保持一致。</p>
 *
 * <p>{@code WebSocketHandlerAdapter} 由 Spring Boot 的响应式 WebSocket 自动配置提供，
 * 这里只负责把处理器挂到端点路径上。</p>
 *
 * @author zzy
 */
@Configuration
@ConditionalOnProperty(prefix = "fw.plus.ws", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(name = {"org.springframework.web.reactive.socket.WebSocketHandler"})
@EnableConfigurationProperties(WsConfig.class)
public class WsWebFluxAutoConfiguration {

	/**
	 * 响应式 WebSocket 处理器。宿主已自行提供时不再创建。
	 *
	 * @param wsConfig WebSocket 通用配置
	 * @return 响应式处理器
	 */
	@Bean
	@ConditionalOnMissingBean(ReactiveWsHandler.class)
	public ReactiveWsHandler reactiveWsHandler(WsConfig wsConfig) {
		return new ReactiveWsHandler(wsConfig);
	}

	/**
	 * 把处理器挂到 {@code fw.plus.ws.websocket.path} 上。
	 *
	 * @param handler           响应式处理器
	 * @param wsConfig          WebSocket 通用配置
	 * @param basePathProvider  基础路径提供者（可选）
	 * @return URL 映射
	 */
	@Bean
	public SimpleUrlHandlerMapping wsReactiveHandlerMapping(ReactiveWsHandler handler,
															WsConfig wsConfig,
															ObjectProvider<WsBasePathProvider> basePathProvider) {
		String path = normalizePath(wsConfig, basePathProvider.getIfAvailable());
		Map<String, WebSocketHandler> urlMap = new HashMap<>(4);
		urlMap.put(path, handler);
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setUrlMap(urlMap);
		mapping.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
		return mapping;
	}

	private static String normalizePath(WsConfig wsConfig, WsBasePathProvider basePathProvider) {
		String basePath = basePathProvider == null ? null : basePathProvider.getBasePath();
		if (basePath == null) {
			basePath = "";
		}
		String path = wsConfig.getWebsocket().getPath();
		if (StringUtils.isBlank(path)) {
			path = "/ws/api";
		}
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		boolean baseEndsWithSlash = basePath.endsWith("/");
		boolean pathStartsWithSlash = path.startsWith("/");
		if (!baseEndsWithSlash && !pathStartsWithSlash) {
			return basePath + "/" + path;
		}
		if (baseEndsWithSlash && pathStartsWithSlash) {
			return basePath.substring(0, basePath.length() - 1) + path;
		}
		return basePath + path;
	}
}
