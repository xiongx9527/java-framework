package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.web.TinyEngineWebResponses;
import com.xiongx9527.fw.plus.tinyengine.common.model.Result;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

import java.util.concurrent.Callable;

final class TinyEngineWebFluxSupport {

	private TinyEngineWebFluxSupport() {
	}

	static Mono<ResponseEntity<? extends Result<?>>> submit(TinyEngineReactiveExecutor executor, Callable<?> task) {
		Callable<ResponseEntity<? extends Result<?>>> responseTask = () -> TinyEngineWebResponses.ok(task.call());
		return submitResponse(executor, responseTask);
	}

	static Mono<ResponseEntity<? extends Result<?>>> submitResponse(TinyEngineReactiveExecutor executor,
			Callable<ResponseEntity<? extends Result<?>>> task) {
		return executor.submit(task).onErrorResume(Exception.class,
				ex -> Mono.just(TinyEngineWebResponses.error(ex)));
	}
}
