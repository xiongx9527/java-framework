package com.xiongx9527.fw.plus.ai.mcp.transport.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 按实例路径把 {@link McpServletEndpoint} 注册进 Spring MVC 的
 * {@link RequestMappingHandlerMapping}
 *
 * <p>采用编程式注册而非注解固定路径，以支撑"多实例、每实例独立路径"（spec 5）。
 * 注册时机为 {@link SmartInitializingSingleton#afterSingletonsInstantiated()}，
 * 此时上下文 bean 已就绪、内嵌 Web 容器尚未开始接收请求。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public class McpServletEndpointRegistrar implements SmartInitializingSingleton {

	private static final Logger logger = LoggerFactory.getLogger(McpServletEndpointRegistrar.class);

	private final RequestMappingHandlerMapping handlerMapping;
	private final List<McpServletEndpoint> endpoints;

	public McpServletEndpointRegistrar(RequestMappingHandlerMapping handlerMapping, List<McpServletEndpoint> endpoints) {
		this.handlerMapping = handlerMapping;
		this.endpoints = endpoints == null ? Collections.<McpServletEndpoint>emptyList() : new ArrayList<>(endpoints);
	}

	@Override
	public void afterSingletonsInstantiated() {
		for (McpServletEndpoint endpoint : endpoints) {
			register(endpoint);
		}
	}

	private void register(McpServletEndpoint endpoint) {
		String path = endpoint.getPath();
		Method postMethod = method("handlePost", HttpServletRequest.class);
		Method getMethod = method("handleGet", HttpServletRequest.class);
		Method deleteMethod = method("handleDelete", HttpServletRequest.class, HttpServletResponse.class);

		// 复用 handler mapping 自身的构建配置：Spring Boot 2.6+ 默认使用
		// PathPatternParser，必须由同一套配置解析路径，否则匹配时会因缺少
		// lookupPath 请求属性而抛 IllegalArgumentException
		RequestMappingInfo.BuilderConfiguration options = handlerMapping.getBuilderConfiguration();
		try {
			handlerMapping.registerMapping(
					RequestMappingInfo.paths(path).methods(RequestMethod.POST).options(options).build(),
					endpoint, postMethod);
			handlerMapping.registerMapping(
					RequestMappingInfo.paths(path).methods(RequestMethod.GET)
							.produces(MediaType.TEXT_EVENT_STREAM_VALUE).options(options).build(),
					endpoint, getMethod);
			handlerMapping.registerMapping(
					RequestMappingInfo.paths(path).methods(RequestMethod.DELETE).options(options).build(),
					endpoint, deleteMethod);
		} catch (IllegalStateException e) {
			throw new IllegalStateException("Failed to register MCP endpoint mapping on path '" + path
					+ "', please make sure each MCP instance has a unique 'fw-plus.mcp.instances[*].path'", e);
		}
		logger.info("Registered MCP Streamable HTTP endpoint (Spring MVC) at path '{}' for instance '{}'",
				path, endpoint.getTransport().getName());
	}

	private static Method method(String name, Class<?>... parameterTypes) {
		try {
			return McpServletEndpoint.class.getMethod(name, parameterTypes);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("Missing MCP endpoint method: " + name, e);
		}
	}
}
