package com.xiongx9527.fw.plus.ai.mcp.transport.reactive;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 按实例路径把 {@link McpReactiveEndpoint} 注册进 Spring WebFlux 的
 * {@link RequestMappingHandlerMapping}
 *
 * <p>与 MVC 版注册器对称：编程式注册以支撑"多实例、每实例独立路径"，
 * 注册时机为上下文单例就绪、响应式容器开始接收请求之前。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public class McpReactiveEndpointRegistrar implements SmartInitializingSingleton {

	private static final Logger logger = LoggerFactory.getLogger(McpReactiveEndpointRegistrar.class);

	private final RequestMappingHandlerMapping handlerMapping;
	private final List<McpReactiveEndpoint> endpoints;

	public McpReactiveEndpointRegistrar(RequestMappingHandlerMapping handlerMapping,
			List<McpReactiveEndpoint> endpoints) {
		this.handlerMapping = handlerMapping;
		this.endpoints = endpoints == null ? Collections.<McpReactiveEndpoint>emptyList() : new ArrayList<>(endpoints);
	}

	@Override
	public void afterSingletonsInstantiated() {
		for (McpReactiveEndpoint endpoint : endpoints) {
			register(endpoint);
		}
	}

	private void register(McpReactiveEndpoint endpoint) {
		String path = endpoint.getPath();
		Method postMethod = method("handlePost", ServerWebExchange.class);
		Method getMethod = method("handleGet", ServerWebExchange.class);
		Method deleteMethod = method("handleDelete", ServerWebExchange.class);
		try {
			handlerMapping.registerMapping(
					RequestMappingInfo.paths(path).methods(RequestMethod.POST).build(), endpoint, postMethod);
			handlerMapping.registerMapping(
					RequestMappingInfo.paths(path).methods(RequestMethod.GET)
							.produces(MediaType.TEXT_EVENT_STREAM_VALUE).build(), endpoint, getMethod);
			handlerMapping.registerMapping(
					RequestMappingInfo.paths(path).methods(RequestMethod.DELETE).build(), endpoint, deleteMethod);
		} catch (IllegalStateException e) {
			throw new IllegalStateException("Failed to register MCP endpoint mapping on path '" + path
					+ "', please make sure each MCP instance has a unique 'fw-plus.mcp.instances[*].path'", e);
		}
		logger.info("Registered MCP Streamable HTTP endpoint (Spring WebFlux) at path '{}' for instance '{}'",
				path, endpoint.getTransport().getName());
	}

	private static Method method(String name, Class<?>... parameterTypes) {
		try {
			return McpReactiveEndpoint.class.getMethod(name, parameterTypes);
		} catch (NoSuchMethodException e) {
			throw new IllegalStateException("Missing MCP endpoint method: " + name, e);
		}
	}
}
