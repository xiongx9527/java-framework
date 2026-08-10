package com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.jdbc.dynamic.datasource.processor.DsRequestContext;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;

public final class DsWebFluxContextHook implements InitializingBean, DisposableBean {

	public static final String HOOK_KEY = DsWebFluxContextHook.class.getName();
	public static final Class<DsRequestContext> CONTEXT_KEY = DsRequestContext.class;

	private static final Object LIFECYCLE_MONITOR = new Object();
	private static int referenceCount;

	private boolean initialized;

	@Override
	public void afterPropertiesSet() {
		synchronized (LIFECYCLE_MONITOR) {
			if (initialized) {
				return;
			}
			if (referenceCount == 0) {
				Hooks.onEachOperator(HOOK_KEY,
						Operators.lift((scannable, subscriber) ->
								new DsWebFluxContextSubscriber<Object>(subscriber)));
			}
			referenceCount++;
			initialized = true;
		}
	}

	@Override
	public void destroy() {
		synchronized (LIFECYCLE_MONITOR) {
			if (!initialized) {
				return;
			}
			initialized = false;
			referenceCount--;
			if (referenceCount == 0) {
				Hooks.resetOnEachOperator(HOOK_KEY);
			}
		}
	}

}
