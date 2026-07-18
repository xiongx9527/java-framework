package com.vesoft.nebula.jdbc.sql;

import com.vesoft.nebula.jdbc.sql.trigger.TriggerAwareNebulaSqlExecutor;

public final class NebulaSqlExecutorProvider {
	private static final NebulaSqlExecutor DEFAULT_EXECUTOR = new TriggerAwareNebulaSqlExecutor();
	private static final ThreadLocal<NebulaSqlExecutor> EXECUTOR_OVERRIDE = new ThreadLocal<NebulaSqlExecutor>();

	private NebulaSqlExecutorProvider() {
	}

	public static NebulaSqlExecutor getExecutor() {
		NebulaSqlExecutor executor = EXECUTOR_OVERRIDE.get();
		return executor == null ? DEFAULT_EXECUTOR : executor;
	}

	public static void setExecutor(NebulaSqlExecutor executor) {
		if (executor == null) {
			throw new IllegalArgumentException("executor must not be null");
		}
		EXECUTOR_OVERRIDE.set(executor);
	}

	public static void reset() {
		EXECUTOR_OVERRIDE.remove();
	}
}
