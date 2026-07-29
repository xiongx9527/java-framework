package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.webflux.WebFluxMagicAPIRequestAccess;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.webflux.WebFluxMagicAPIResponseAccess;
import org.springframework.core.MethodParameter;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.server.ServerWebExchange;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletRequest;
import org.ssssssss.magicapi.core.servlet.MagicHttpServletResponse;
import reactor.core.publisher.Mono;

public class WebFluxMagicAPIArgumentResolver implements HandlerMethodArgumentResolver {

	static final String RESPONSE_ATTRIBUTE = WebFluxMagicAPIArgumentResolver.class.getName() + ".response";

	@Override
	public boolean supportsParameter(MethodParameter parameter) {
		Class<?> type = parameter.getParameterType();
		return MagicHttpServletRequest.class.isAssignableFrom(type)
				|| MagicHttpServletResponse.class.isAssignableFrom(type);
	}

	@Override
	public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext,
										ServerWebExchange exchange) {
		if (MagicHttpServletResponse.class.isAssignableFrom(parameter.getParameterType())) {
			WebFluxMagicAPIResponseAccess response = exchange.getAttribute(RESPONSE_ATTRIBUTE);
			if (response == null) {
				response = new WebFluxMagicAPIResponseAccess(exchange);
				exchange.getAttributes().put(RESPONSE_ATTRIBUTE, response);
			}
			return Mono.just(response);
		}
		return Mono.just(new WebFluxMagicAPIRequestAccess(exchange));
	}
}
