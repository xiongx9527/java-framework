package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineCredentialExtractor;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineAccessDeniedException;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineCredential;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnauthenticatedException;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUserSystemService;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

public final class TinyEngineReactiveAuthenticationFilter implements WebFilter, Ordered {

	private final TinyEngineCredentialExtractor credentialExtractor;
	private final TinyEngineUserSystemService userSystemService;
	private final TinyEngineReactiveExecutor executor;

	public TinyEngineReactiveAuthenticationFilter(TinyEngineCredentialExtractor credentialExtractor,
			TinyEngineUserSystemService userSystemService, TinyEngineReactiveExecutor executor) {
		this.credentialExtractor = credentialExtractor;
		this.userSystemService = userSystemService;
		this.executor = executor;
	}

	@Override
	public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
		if (!requiresAuthentication(exchange)) {
			return chain.filter(exchange);
		}
		Mono<TinyEnginePrincipal> authentication = executor.submitUnscoped(() -> authenticate(exchange))
				.onErrorResume(ex -> writeError(exchange, ex).then(Mono.empty()));
		return authentication.flatMap(principal -> chain.filter(exchange)
				.contextWrite(context -> context.put(TinyEngineReactiveExecutor.PRINCIPAL_CONTEXT_KEY, principal)));
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 10;
	}

	private TinyEnginePrincipal authenticate(ServerWebExchange exchange) {
		TinyEngineCredential credential = credentialExtractor.extract(
				exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION), tenantId(exchange));
		return userSystemService.currentUser(credential);
	}

	private boolean requiresAuthentication(ServerWebExchange exchange) {
		if (exchange.getRequest().getMethod() != null
				&& "OPTIONS".equalsIgnoreCase(exchange.getRequest().getMethod().name())) {
			return false;
		}
		String path = exchange.getRequest().getURI().getPath();
		if (isAnonymousUserApi(path)) {
			return false;
		}
		return containsApiPrefix(path, "/app-center/api/")
				|| containsApiPrefix(path, "/app-center/v1/api/")
				|| containsApiPrefix(path, "/material-center/api/")
				|| containsApiPrefix(path, "/material-center/v1/api/")
				|| containsApiPrefix(path, "/platform-center/api/")
				|| containsApiPrefix(path, "/platform-center/v1/api/");
	}

	private boolean isAnonymousUserApi(String path) {
		return path.endsWith("/platform-center/api/user/login")
				|| path.endsWith("/platform-center/api/user/register")
				|| path.endsWith("/platform-center/api/user/forgot-password");
	}

	private boolean containsApiPrefix(String path, String prefix) {
		return path.startsWith(prefix) || path.contains(prefix);
	}

	private String tenantId(ServerWebExchange exchange) {
		String tenantId = exchange.getRequest().getHeaders().getFirst("X-Tenant-Id");
		if (isBlank(tenantId)) {
			tenantId = exchange.getRequest().getQueryParams().getFirst("tenantId");
		}
		if (isBlank(tenantId)) {
			tenantId = exchange.getRequest().getQueryParams().getFirst("tenant");
		}
		return tenantId;
	}

	private Mono<Void> writeError(ServerWebExchange exchange, Throwable error) {
		HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
		String code = "500";
		if (error instanceof TinyEngineUnauthenticatedException) {
			status = HttpStatus.UNAUTHORIZED;
			code = "401";
		} else if (error instanceof TinyEngineAccessDeniedException) {
			status = HttpStatus.FORBIDDEN;
			code = "403";
		}
		String message = error.getMessage() == null ? "" : escapeJson(error.getMessage());
		byte[] body = ("{\"data\":null,\"code\":\"" + code + "\",\"message\":\"" + message
				+ "\",\"success\":false,\"error\":{\"code\":\"" + code + "\",\"message\":\""
				+ message + "\"},\"errMsg\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
		exchange.getResponse().setStatusCode(status);
		exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
		DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
		return exchange.getResponse().writeWith(Mono.just(buffer));
	}

	private String escapeJson(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"")
				.replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
