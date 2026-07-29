package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux;

import org.apache.commons.lang3.StringUtils;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.StringValueResolver;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.result.method.RequestMappingInfo;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.ssssssss.magicapi.utils.PathUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.stream.Stream;

public class MagicAPIWebFluxMapping {

	private static final String DEFAULT_WEB_PATH = "/magic-api/web";

	private final RequestMappingHandlerMapping methodMapping;
	private final String base;
	private final StringValueResolver embeddedValueResolver;

	public MagicAPIWebFluxMapping(RequestMappingHandlerMapping methodMapping, String base) {
		this(methodMapping, base, null);
	}

	public MagicAPIWebFluxMapping(RequestMappingHandlerMapping methodMapping, String base,
								  StringValueResolver embeddedValueResolver) {
		this.methodMapping = methodMapping;
		this.base = StringUtils.defaultIfBlank(base, DEFAULT_WEB_PATH);
		this.embeddedValueResolver = embeddedValueResolver;
	}

	public RequestMappingInfo.Builder paths(String... paths) {
		return RequestMappingInfo.paths(paths);
	}

	public MagicAPIWebFluxMapping register(RequestMappingInfo requestMappingInfo, Object handler, Method method) {
		if (this.methodMapping.getHandlerMethods().containsKey(requestMappingInfo)) {
			return this;
		}
		this.methodMapping.registerMapping(requestMappingInfo, handler, method);
		return this;
	}

	public Map<RequestMappingInfo, org.springframework.web.method.HandlerMethod> getHandlerMethods() {
		return this.methodMapping.getHandlerMethods();
	}

	public MagicAPIWebFluxMapping unregister(RequestMappingInfo info) {
		this.methodMapping.unregisterMapping(info);
		return this;
	}

	public MagicAPIWebFluxMapping registerController(Object target) {
		return registerController(target, this.base);
	}

	public MagicAPIWebFluxMapping registerController(Object target, String base) {
		Method[] methods = target.getClass().getDeclaredMethods();
		String controllerBase = StringUtils.defaultString(base);
		for (Method method : methods) {
			RequestMapping requestMapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
			if (requestMapping != null) {
				String[] values = requestMapping.value().length == 0 ? new String[]{""} : requestMapping.value();
				String[] paths = Stream.of(values)
						.map(this::resolveEmbeddedValue)
						.map(value -> PathUtils.replaceSlash(controllerBase + value))
						.toArray(String[]::new);
				this.register(paths(paths)
								.methods(requestMapping.method())
								.params(requestMapping.params())
								.headers(requestMapping.headers())
								.consumes(requestMapping.consumes())
								.produces(requestMapping.produces())
								.build(),
						target,
						method);
			}
		}
		return this;
	}

	private String resolveEmbeddedValue(String value) {
		if (embeddedValueResolver == null || value == null) {
			return value;
		}
		return embeddedValueResolver.resolveStringValue(value);
	}
}
