package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

import java.time.Duration;
import java.util.concurrent.Callable;

public final class TinyEngineReactiveExecutor {

	private final Scheduler scheduler;
	private final Duration timeout;

	public TinyEngineReactiveExecutor(Scheduler scheduler, Duration timeout) {
		this.scheduler = scheduler;
		this.timeout = timeout;
	}

	public <T> Mono<T> submit(Callable<T> task) {
		return Mono.fromCallable(task).subscribeOn(scheduler).timeout(timeout);
	}
}
