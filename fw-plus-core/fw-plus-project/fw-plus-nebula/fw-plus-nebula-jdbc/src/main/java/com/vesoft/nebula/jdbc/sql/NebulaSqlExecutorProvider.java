package com.vesoft.nebula.jdbc.sql;

public final class NebulaSqlExecutorProvider {
	private static volatile NebulaSqlExecutor executor = new DirectNebulaSqlExecutor();

	private NebulaSqlExecutorProvider() {
	}

	public static NebulaSqlExecutor getExecutor() {
		return executor;
	}

	public static void setExecutor(NebulaSqlExecutor executor) {
		if (executor == null) {
			throw new IllegalArgumentException("executor must not be null");
		}
		NebulaSqlExecutorProvider.executor = executor;
	}

	public static void reset() {
		executor = new DirectNebulaSqlExecutor();
	}
}
