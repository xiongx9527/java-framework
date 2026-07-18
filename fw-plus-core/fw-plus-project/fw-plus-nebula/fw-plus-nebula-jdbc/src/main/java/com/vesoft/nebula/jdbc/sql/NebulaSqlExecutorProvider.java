package com.vesoft.nebula.jdbc.sql;

import com.vesoft.nebula.jdbc.sql.binlog.BinlogEventStore;
import com.vesoft.nebula.jdbc.sql.binlog.NebulaBinlogEventOptions;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.vesoft.nebula.jdbc.sql.trigger.TriggerAwareNebulaSqlExecutor;

public final class NebulaSqlExecutorProvider {
	private static final NebulaSqlExecutor DEFAULT_EXECUTOR = new TriggerAwareNebulaSqlExecutor();
	private static final ThreadLocal<NebulaSqlExecutor> EXECUTOR_OVERRIDE = new ThreadLocal<NebulaSqlExecutor>();
	private static volatile DefaultExecutorConfiguration defaultExecutorConfiguration;

	private NebulaSqlExecutorProvider() {
	}

	public static NebulaSqlExecutor getExecutor() {
		NebulaSqlExecutor executor = EXECUTOR_OVERRIDE.get();
		if (executor != null) {
			return executor;
		}
		DefaultExecutorConfiguration configuration = defaultExecutorConfiguration;
		if (configuration != null) {
			return new DirectNebulaSqlExecutor(
					configuration.schemaMapping,
					configuration.binlogEventStore,
					configuration.binlogEventOptions
			);
		}
		return DEFAULT_EXECUTOR;
	}

	public static void setExecutor(NebulaSqlExecutor executor) {
		if (executor == null) {
			throw new IllegalArgumentException("executor must not be null");
		}
		EXECUTOR_OVERRIDE.set(executor);
	}

	public static void configureDefaultExecutor(NebulaSchemaMappingRegistry schemaMapping,
			BinlogEventStore binlogEventStore) {
		configureDefaultExecutor(schemaMapping, binlogEventStore, NebulaBinlogEventOptions.defaults());
	}

	public static void configureDefaultExecutor(NebulaSchemaMappingRegistry schemaMapping,
			BinlogEventStore binlogEventStore, NebulaBinlogEventOptions binlogEventOptions) {
		if (binlogEventStore == null) {
			throw new IllegalArgumentException("binlogEventStore must not be null");
		}
		defaultExecutorConfiguration = new DefaultExecutorConfiguration(schemaMapping, binlogEventStore,
				binlogEventOptions);
	}

	public static void reset() {
		EXECUTOR_OVERRIDE.remove();
		defaultExecutorConfiguration = null;
	}

	private static class DefaultExecutorConfiguration {
		private final NebulaSchemaMappingRegistry schemaMapping;
		private final BinlogEventStore binlogEventStore;
		private final NebulaBinlogEventOptions binlogEventOptions;

		private DefaultExecutorConfiguration(NebulaSchemaMappingRegistry schemaMapping, BinlogEventStore binlogEventStore,
				NebulaBinlogEventOptions binlogEventOptions) {
			this.schemaMapping = schemaMapping;
			this.binlogEventStore = binlogEventStore;
			this.binlogEventOptions = binlogEventOptions;
		}
	}
}
