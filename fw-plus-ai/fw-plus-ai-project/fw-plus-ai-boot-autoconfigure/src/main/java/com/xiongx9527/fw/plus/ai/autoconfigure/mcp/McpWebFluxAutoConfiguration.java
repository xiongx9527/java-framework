package com.xiongx9527.fw.plus.ai.autoconfigure.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.McpAutoConfiguration.McpServerInstanceRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpInstanceProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import com.xiongx9527.fw.plus.ai.mcp.transport.McpStreamableTransport;
import com.xiongx9527.fw.plus.ai.mcp.transport.reactive.McpReactiveEndpoint;
import com.xiongx9527.fw.plus.ai.mcp.transport.reactive.McpReactiveEndpointRegistrar;
import com.xiongx9527.fw.plus.ai.mcp.transport.reactive.McpReactiveStreamableTransport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.reactive.WebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.reactive.DispatcherHandler;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring WebFlux（Reactive）MCP Streamable HTTP 自动配置
 *
 * <p>对齐 spec 5.3 / 8.3：
 * <ul>
 *   <li>仅在响应式 Web 应用且存在 reactor/spring-webflux 类时装配，与 MVC 配置互斥；</li>
 *   <li>WebFlux 依赖在 Starter 中为 optional，未引入时本配置不生效；</li>
 *   <li>为每个 enabled 实例创建 {@link McpReactiveStreamableTransport} 并按实例路径注册端点。</li>
 * </ul>
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({DispatcherHandler.class, ServerSentEvent.class, Flux.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@AutoConfigureAfter(WebFluxAutoConfiguration.class)
public class McpWebFluxAutoConfiguration {

	/**
	 * 创建响应式传输、绑定 server，并按实例路径注册 WebFlux 端点映射
	 */
	@Bean
	public McpReactiveEndpointRegistrar mcpReactiveEndpointRegistrar(
			McpServerInstanceRegistry registry,
			RequestMappingHandlerMapping requestMappingHandlerMapping,
			ObjectProvider<ObjectMapper> objectMapperProvider,
			McpProperties properties) {

		final ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);

		registry.bindAndStart(config -> createTransport(config, objectMapper, properties));

		List<McpReactiveEndpoint> endpoints = new ArrayList<>();
		for (McpStreamableTransport transport : registry.getAllTransports().values()) {
			endpoints.add(new McpReactiveEndpoint((McpReactiveStreamableTransport) transport));
		}
		return new McpReactiveEndpointRegistrar(requestMappingHandlerMapping, endpoints);
	}

	private McpReactiveStreamableTransport createTransport(McpInstanceProperties config, ObjectMapper objectMapper,
			McpProperties properties) {
		McpReactiveStreamableTransport transport = new McpReactiveStreamableTransport(
				config.getName(),
				config.getPath(),
				config.getSessionTimeoutSeconds() * 1000L,
				objectMapper);
		transport.setAllowedOrigins(properties.getAllowedOrigins());
		return transport;
	}
}
