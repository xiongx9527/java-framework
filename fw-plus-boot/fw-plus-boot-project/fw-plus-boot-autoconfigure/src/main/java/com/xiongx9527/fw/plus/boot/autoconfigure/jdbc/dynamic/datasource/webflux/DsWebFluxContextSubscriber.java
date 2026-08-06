package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsRequestContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsRequestContextHolder;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.util.context.Context;

final class DsWebFluxContextSubscriber<T> implements CoreSubscriber<T> {

	private final CoreSubscriber<? super T> actual;

	DsWebFluxContextSubscriber(CoreSubscriber<? super T> actual) {
		this.actual = actual;
	}

	@Override
	public Context currentContext() {
		return actual.currentContext();
	}

	@Override
	public void onSubscribe(Subscription subscription) {
		withRequestContext(() -> actual.onSubscribe(subscription));
	}

	@Override
	public void onNext(T value) {
		withRequestContext(() -> actual.onNext(value));
	}

	@Override
	public void onError(Throwable throwable) {
		withRequestContext(() -> actual.onError(throwable));
	}

	@Override
	public void onComplete() {
		withRequestContext(actual::onComplete);
	}

	private void withRequestContext(Runnable callback) {
		DsRequestContext context = actual.currentContext()
				.getOrDefault(DsWebFluxContextHook.CONTEXT_KEY, null);
		if (context == null) {
			callback.run();
			return;
		}
		try (DsRequestContextHolder.Scope ignored = DsRequestContextHolder.open(context)) {
			callback.run();
		}
	}

}
