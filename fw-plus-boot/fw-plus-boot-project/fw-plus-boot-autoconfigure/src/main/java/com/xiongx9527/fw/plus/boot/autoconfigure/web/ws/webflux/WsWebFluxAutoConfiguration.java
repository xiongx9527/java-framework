package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.webflux;

import com.xiongx9527.fw.plus.auth.core.InstanceAuthRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.auth.WsAuthSupport;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsEndpointPathResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsInstanceValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBasePathProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * WebFlux 栈的 WebSocket 自动配置，由 {@code WsAutoConfiguration} 的 {@code @Import} 收编。
 *
 * <p>仅在响应式 Web 应用中生效；Servlet 栈由 {@code WsAutoConfiguration} 负责。
 * 两个栈共用同一套 {@code fw.plus.ws.*} 配置与订阅 SPI，行为保持一致。</p>
 *
 * <p>{@code fw.plus.ws.enabled} 总开关由 {@code WsAutoConfiguration} 负责：它不通过时本类不会被解析，
 * 因此这里不再重复声明。</p>
 *
 * <p>{@code WebSocketHandlerAdapter} 由 Spring Boot 的响应式 WebSocket 自动配置提供，
 * 这里只负责把处理器挂到端点路径上。</p>
 *
 * @author zzy
 */
@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@ConditionalOnClass(name = {"org.springframework.web.reactive.socket.WebSocketHandler"})
@EnableConfigurationProperties(WsConfig.class)
public class WsWebFluxAutoConfiguration {

	/**
	 * 响应式 WebSocket 处理器。宿主已自行提供时不再创建。
	 *
	 * <p>注入实例认证注册表，并在启动期校验全部实例 provider 是否受支持（未知 provider 抛
	 * {@link IllegalArgumentException}）。</p>
	 *
	 * @param wsConfig        WebSocket 通用配置
	 * @param authRegistries  实例认证注册表候选（未提供时回落默认注册表）
	 * @return 响应式处理器
	 */
	@Bean
	@ConditionalOnMissingBean(ReactiveWsHandler.class)
	public ReactiveWsHandler reactiveWsHandler(WsConfig wsConfig,
											   ObjectProvider<InstanceAuthRegistry> authRegistries) {
		InstanceAuthRegistry registry = authRegistries.getIfAvailable(InstanceAuthRegistry::new);
		WsAuthSupport.validateProviders(wsConfig, registry);
		ReactiveWsHandler handler = new ReactiveWsHandler(wsConfig);
		handler.setAuthRegistry(registry);
		return handler;
	}

	/**
	 * 把处理器挂到 {@code fw.plus.ws.path} 上，外加 {@code fw.plus.ws.paths}
	 * 与容器内多个 {@link WsBasePathProvider} 展开出的其余路径。
	 *
	 * @param handler           响应式处理器
	 * @param wsConfig          WebSocket 通用配置
	 * @param basePathProviders 基础路径提供者候选（可为空）
	 * @return URL 映射
	 */
	@Bean
	public SimpleUrlHandlerMapping wsReactiveHandlerMapping(ReactiveWsHandler handler,
															WsConfig wsConfig,
															ObjectProvider<WsBasePathProvider> basePathProviders) {
		Map<String, WebSocketHandler> urlMap = new HashMap<>(8);
		for (String fullPath : resolveFullPaths(wsConfig, basePathProviders)) {
			urlMap.put(fullPath, handler);
		}
		SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
		mapping.setUrlMap(urlMap);
		mapping.setOrder(Ordered.LOWEST_PRECEDENCE - 100);
		return mapping;
	}

	/**
	 * 按与 webmvc 栈相同的规则展开端点：全部基础路径提供者 ×（默认实例端点 + 命名实例端点）。
	 *
	 * @param wsConfig          WebSocket 通用配置
	 * @param basePathProviders 基础路径提供者候选
	 * @return 完整端点路径列表
	 */
	private static List<String> resolveFullPaths(WsConfig wsConfig, ObjectProvider<WsBasePathProvider> basePathProviders) {
		List<String> basePaths = new ArrayList<>();
		basePathProviders.orderedStream().forEach(provider -> {
			String basePath = WsEndpointPathResolver.normalizeBasePath(provider.getBasePath());
			if (!basePath.isEmpty()) {
				basePaths.add(basePath);
			}
		});
		List<String> relativePaths = WsEndpointPathResolver.resolveRelativePaths(wsConfig);
		return WsEndpointPathResolver.resolveFullPaths(relativePaths, basePaths);
	}

	/**
	 * 命名实例配置校验：与 webmvc 栈 {@code WsAutoConfiguration} 共用同一份规则，
	 * {@code fw.plus.ws.servers} 的 key 必填且唯一、端点全局唯一，任一不满足都启动失败。
	 *
	 * @param wsConfig WebSocket 通用配置
	 * @return 校验器实例
	 */
	@Bean
	public WsInstanceValidator wsReactiveInstanceValidator(WsConfig wsConfig,
														   ObjectProvider<InstanceAuthRegistry> authRegistries) {
		WsInstanceValidator.validate(wsConfig);
		WsAuthSupport.validateProviders(wsConfig, authRegistries.getIfAvailable(InstanceAuthRegistry::new));
		return new WsInstanceValidator();
	}
}
