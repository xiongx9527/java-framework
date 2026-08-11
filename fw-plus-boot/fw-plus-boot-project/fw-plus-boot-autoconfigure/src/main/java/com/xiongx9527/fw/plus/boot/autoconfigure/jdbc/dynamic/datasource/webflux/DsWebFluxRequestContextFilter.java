package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsRequestContext;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DsWebFluxRequestContextFilter implements WebFilter, Ordered {

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		Map<String, List<String>> headers = copyHeaders(exchange.getRequest().getHeaders());
		return exchange.getSession().flatMap(session -> {
			DsRequestContext snapshot = new DsRequestContext(headers, session.getAttributes());
			return chain.filter(exchange)
					.contextWrite(context -> context.put(DsWebFluxContextHook.CONTEXT_KEY, snapshot));
		});
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	private Map<String, List<String>> copyHeaders(HttpHeaders source) {
		Map<String, List<String>> copy = new LinkedHashMap<>();
		source.forEach((name, values) -> copy.put(name, new ArrayList<>(values)));
		return copy;
	}

}
