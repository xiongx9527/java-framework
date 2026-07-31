package com.xiongx9527.fw.plus.tinyengine.support.context;

import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEnginePrincipal;
import com.xiongx9527.fw.plus.tinyengine.support.auth.TinyEngineUnauthenticatedException;

import java.util.concurrent.Callable;

public class TinyEngineRequestUserScope implements TinyEngineUserContext {

	private final ThreadLocal<TinyEnginePrincipal> current = new ThreadLocal<>();

	@Override
	public TinyEngineUser getCurrentUser() {
		return current.get();
	}

	public <T> T callAs(TinyEnginePrincipal principal, Callable<T> task) throws Exception {
		if (principal == null) {
			throw new TinyEngineUnauthenticatedException("TinyEngine principal is required");
		}
		TinyEnginePrincipal previous = current.get();
		try {
			current.set(principal);
			return task.call();
		} finally {
			if (previous == null) {
				current.remove();
			} else {
				current.set(previous);
			}
		}
	}
}
