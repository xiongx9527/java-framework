package com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.webflux;

import reactor.core.publisher.Mono;
import reactor.util.context.ContextView;
import reactor.core.scheduler.Scheduler;

import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineRequestUserScope;

import java.time.Duration;
import java.util.concurrent.Callable;

public final class TinyEngineReactiveExecutor {

	private final Scheduler scheduler;
	private final Duration timeout;
	private final TinyEngineRequestUserScope userScope;

	static final String PRINCIPAL_CONTEXT_KEY = TinyEngineReactiveExecutor.class.getName() + ".principal";

	public TinyEngineReactiveExecutor(Scheduler scheduler, Duration timeout) {
		this(scheduler, timeout, null);
	}

	public TinyEngineReactiveExecutor(Scheduler scheduler, Duration timeout, TinyEngineRequestUserScope userScope) {
		this.scheduler = scheduler;
		this.timeout = timeout;
		this.userScope = userScope;
	}

	public <T> Mono<T> submit(Callable<T> task) {
		return Mono.deferContextual(context -> Mono.fromCallable(() -> callWithPrincipal(context, task)))
				.subscribeOn(scheduler).timeout(timeout);
	}

	<T> Mono<T> submitUnscoped(Callable<T> task) {
		return Mono.fromCallable(task).subscribeOn(scheduler).timeout(timeout);
	}

	private <T> T callWithPrincipal(ContextView context, Callable<T> task) throws Exception {
		if (userScope == null || !context.hasKey(PRINCIPAL_CONTEXT_KEY)) {
			return task.call();
		}
		return userScope.callAs(context.<TinyEnginePrincipal>get(PRINCIPAL_CONTEXT_KEY), task);
	}
}
