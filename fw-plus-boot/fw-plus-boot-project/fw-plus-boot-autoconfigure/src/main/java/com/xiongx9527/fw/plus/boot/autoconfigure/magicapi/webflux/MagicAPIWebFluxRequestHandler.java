package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.webflux.WebFluxMagicAPIRequestAccess;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.webflux.WebFluxMagicAPIResponseAccess;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.ssssssss.magicapi.core.web.RequestHandler;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

public class MagicAPIWebFluxRequestHandler {

	private final RequestHandler requestHandler;

	public MagicAPIWebFluxRequestHandler(RequestHandler requestHandler) {
		this.requestHandler = requestHandler;
	}

	@ResponseBody
	public Mono<Object> invoke(ServerWebExchange exchange,
							   @PathVariable(required = false) Map<String, Object> pathVariables,
							   @RequestHeader(required = false) Map<String, Object> headers,
							   @RequestParam(required = false) Map<String, Object> parameters) {
		return body(exchange).flatMap(bytes -> Mono.fromCallable(() -> {
			WebFluxMagicAPIRequestAccess request = new WebFluxMagicAPIRequestAccess(exchange, bytes);
			WebFluxMagicAPIResponseAccess response = new WebFluxMagicAPIResponseAccess(exchange);
			Object pattern = exchange.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
			if (pattern != null) {
				request.setAttribute(org.springframework.web.servlet.HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE,
						pattern.toString());
			}
			Object value;
			try {
				value = requestHandler.invoke(request, response, pathVariables, headers, parameters);
			} catch (Exception exception) {
				throw exception;
			} catch (Throwable throwable) {
				throw new IllegalStateException(throwable);
			}
			if (value == null && response.body().length > 0) {
				return ResponseEntity.ok().body(response.body());
			}
			return value;
		}).subscribeOn(Schedulers.boundedElastic()));
	}

	private Mono<byte[]> body(ServerWebExchange exchange) {
		return DataBufferUtils.join(exchange.getRequest().getBody())
				.map(dataBuffer -> {
					byte[] bytes = new byte[dataBuffer.readableByteCount()];
					dataBuffer.read(bytes);
					DataBufferUtils.release(dataBuffer);
					return bytes;
				})
				.defaultIfEmpty(new byte[0]);
	}
}
