package com.xiongx9527.fw.plus.boot.common.web.flux.filter;

import com.xiongx9527.fw.plus.boot.common.web.BaseFilter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * @author liufeng
 * @date 2021/8/24 15:22
 */
public abstract class WebFluxBaseRequestFilter implements WebFilter, BaseFilter {
    private final List<String> excludeUrlPatterns;

    public WebFluxBaseRequestFilter() {
        this.excludeUrlPatterns = null;
    }

    public WebFluxBaseRequestFilter(List<String> excludeUrlPatterns) {
        this.excludeUrlPatterns = excludeUrlPatterns;
    }


    protected abstract Mono<Void> doFilterInternal(ServerWebExchange exchange, WebFilterChain chain) ;

    final public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        return chain.filter(exchange);
    }
}
