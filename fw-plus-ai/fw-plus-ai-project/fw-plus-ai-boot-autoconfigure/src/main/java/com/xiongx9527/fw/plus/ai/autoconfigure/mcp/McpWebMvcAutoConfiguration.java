package com.xiongx9527.fw.plus.ai.autoconfigure.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.McpAutoConfiguration.McpServerInstanceRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpInstanceProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import com.xiongx9527.fw.plus.ai.mcp.transport.McpStreamableTransport;
import com.xiongx9527.fw.plus.ai.mcp.transport.servlet.McpServletEndpoint;
import com.xiongx9527.fw.plus.ai.mcp.transport.servlet.McpServletEndpointRegistrar;
import com.xiongx9527.fw.plus.ai.mcp.transport.servlet.McpServletStreamableTransport;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring MVC（Servlet）MCP Streamable HTTP 自动配置
 *
 * <p>对齐 spec 5.2 / 8.3：
 * <ul>
 *   <li>仅在 Servlet Web 应用且存在 spring-webmvc 类时装配，与 WebFlux 配置互斥；</li>
 *   <li>为每个 enabled 实例创建一个 {@link McpServletStreamableTransport} 并绑定 server；</li>
 *   <li>按实例路径注册 {@link McpServletEndpoint}（POST/GET/DELETE）。</li>
 * </ul>
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({DispatcherServlet.class, SseEmitter.class, RequestMappingHandlerMapping.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@AutoConfigureAfter(WebMvcAutoConfiguration.class)
public class McpWebMvcAutoConfiguration {

	/**
	 * 创建 Servlet 传输、绑定 server，并按实例路径注册 MVC 端点映射
	 */
	@Bean
	public McpServletEndpointRegistrar mcpServletEndpointRegistrar(
			McpServerInstanceRegistry registry,
			RequestMappingHandlerMapping requestMappingHandlerMapping,
			ObjectProvider<ObjectMapper> objectMapperProvider,
			McpProperties properties) {

		final ObjectMapper objectMapper = objectMapperProvider.getIfAvailable(ObjectMapper::new);

		registry.bindAndStart(config -> createTransport(config, objectMapper, properties));

		List<McpServletEndpoint> endpoints = new ArrayList<>();
		for (McpStreamableTransport transport : registry.getAllTransports().values()) {
			endpoints.add(new McpServletEndpoint((McpServletStreamableTransport) transport));
		}
		return new McpServletEndpointRegistrar(requestMappingHandlerMapping, endpoints);
	}

	private McpServletStreamableTransport createTransport(McpInstanceProperties config, ObjectMapper objectMapper,
			McpProperties properties) {
		McpServletStreamableTransport transport = new McpServletStreamableTransport(
				config.getName(),
				config.getPath(),
				config.getSessionTimeoutSeconds() * 1000L,
				objectMapper);
		transport.setAllowedOrigins(properties.getAllowedOrigins());
		return transport;
	}
}
