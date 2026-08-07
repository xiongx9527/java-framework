package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapterRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeInvocation;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkChangeMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.api.ConsistencyLevel;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeEvent;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeFailure;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import com.xiongx9527.fw.plus.mybatis.datachange.api.StatementChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableChange;
import com.xiongx9527.fw.plus.mybatis.datachange.config.DataChangeConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.config.LogicalDeleteConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitorRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.AnalyzedStatement;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.BoundSqlContext;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.ChangeTarget;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialectRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.StatementOperation;
import com.xiongx9527.fw.plus.mybatis.datachange.diff.ChangeDiffEngine;
import com.xiongx9527.fw.plus.mybatis.datachange.dispatch.DataChangeDispatcher;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.GeneratedKeyResolver;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.JdbcTableMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.JdbcTableMetadataResolver;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.RowIdentityResolver;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.RowSnapshot;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.SnapshotService;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class DataChangeEngine {
	private final DataChangeConfiguration configuration;
	private final TableMonitorRegistry monitors;
	private final DataChangeFrameworkAdapterRegistry adapters;
	private final DataChangeDialectRegistry dialects;
	private final JdbcTableMetadataResolver metadataResolver;
	private final SnapshotService snapshots;
	private final ChangeDiffEngine diffEngine;
	private final DataChangeDispatcher dispatcher;
	private final DataChangeErrorHandler errorHandler;
	private final RowIdentityResolver identityResolver = new RowIdentityResolver();
	private final JdbcTransactionStateResolver transactionStateResolver = new JdbcTransactionStateResolver();

	public DataChangeEngine(DataChangeConfiguration configuration, TableMonitorRegistry monitors,
			DataChangeFrameworkAdapterRegistry adapters, DataChangeDialectRegistry dialects,
			JdbcTableMetadataResolver metadataResolver, SnapshotService snapshots,
			ChangeDiffEngine diffEngine, DataChangeDispatcher dispatcher, DataChangeErrorHandler errorHandler) {
		this.configuration = configuration;
		this.monitors = monitors;
		this.adapters = adapters;
		this.dialects = dialects;
		this.metadataResolver = metadataResolver;
		this.snapshots = snapshots;
		this.diffEngine = diffEngine;
		this.dispatcher = dispatcher;
		this.errorHandler = errorHandler;
	}

	DataChangeFrameworkAdapter selectAdapter(MappedStatement mappedStatement, Object parameterObject) {
		BoundSql boundSql = mappedStatement == null ? null : mappedStatement.getBoundSql(parameterObject);
		return adapters.select(new DataChangeInvocation(mappedStatement, boundSql, parameterObject));
	}

	public List<StatementChangePlan> beforeStatement(DataChangeExecutionContext context,
			BoundSqlContext boundSql) {
		Connection connection = context.getConnection();
		List<AnalyzedStatement> analyzed = dialects.analyze(connection, boundSql, monitors);
		if (!containsTargets(analyzed)) { return Collections.emptyList(); }
		DataChangeDialect dialect = dialects.select(connection);
		List<StatementChangePlan> plans = new ArrayList<StatementChangePlan>();
		for (AnalyzedStatement statement : analyzed) {
			for (ChangeTarget detectedTarget : statement.getTargets()) {
				Optional<TableMonitor> configured = monitors.find(detectedTarget.getTable(), dialect);
				if (!configured.isPresent()) { continue; }
				TableMonitor monitor = configured.get();
				JdbcTableMetadata jdbcMetadata = metadataResolver.resolve(connection, detectedTarget.getTable());
				FrameworkChangeMetadata framework = resolveFramework(context, boundSql, monitor, jdbcMetadata);
				List<String> keys = identityResolver.resolve(monitor, framework, jdbcMetadata);
				ChangeTarget target = detectedTarget.withSnapshotConfiguration(keys, monitoredColumns(monitor),
						maxRows(monitor), context.getConsistencyLevel() == ConsistencyLevel.TRANSACTIONAL);
				List<RowSnapshot> before = target.getOperation() == StatementOperation.INSERT
						? Collections.<RowSnapshot>emptyList()
						: snapshots.readBefore(connection, dialect, target, jdbcMetadata,
								context.getMappedStatement().getConfiguration());
				plans.add(new StatementChangePlan(statement.getStatementIndex(), target, monitor,
						jdbcMetadata, framework, dialect, before));
			}
		}
		return Collections.unmodifiableList(plans);
	}

	public void initializeConnection(DataChangeExecutionContext context, Connection connection) {
		context.initializeConnection(connection, transactionStateResolver.resolve(connection));
	}

	public void captureStatementResult(DataChangeExecutionContext context, List<StatementChangePlan> plans,
			Statement statement, int updateCount) {
		if (plans == null || plans.isEmpty()) { return; }
		try {
			for (StatementChangePlan plan : plans) {
				List<RowIdentity> identities = updateCount <= 0
						? Collections.<RowIdentity>emptyList() : identities(context, plan, statement);
				plan.captureResult(updateCount, identities);
			}
		} catch (RuntimeException exception) {
			handlePostFailure(context, safe(exception));
		}
	}

	public List<StatementChange> afterStatement(DataChangeExecutionContext context,
			List<StatementChangePlan> plans, List<Integer> updateCounts) {
		if (context.isPostProcessingFailed() || plans == null || plans.isEmpty()) {
			return Collections.emptyList();
		}
		try {
			Map<Integer, List<TableChange>> grouped = new LinkedHashMap<Integer, List<TableChange>>();
			for (StatementChangePlan plan : plans) {
				int count = effectiveUpdateCount(plan, updateCounts);
				if (count <= 0) { continue; }
				List<RowSnapshot> after = plan.getTarget().getOperation() == StatementOperation.PHYSICAL_DELETE
						? Collections.<RowSnapshot>emptyList()
						: snapshots.readAfter(context.getConnection(), plan.getDialect(), plan.getTarget(),
								plan.getIdentities(), plan.getTableMetadata(),
								context.getMappedStatement().getConfiguration());
				TableChange change = diffEngine.diff(plan.getTarget().getTable(), plan.getMonitor(),
						Collections.<String, String>emptyMap(), plan.getBeforeRows(), after);
				if (change.getRows().isEmpty()) { continue; }
				List<TableChange> tables = grouped.get(Integer.valueOf(plan.getStatementIndex()));
				if (tables == null) {
					tables = new ArrayList<TableChange>();
					grouped.put(Integer.valueOf(plan.getStatementIndex()), tables);
				}
				tables.add(change);
			}
			List<StatementChange> changes = new ArrayList<StatementChange>();
			for (Map.Entry<Integer, List<TableChange>> entry : grouped.entrySet()) {
				changes.add(new StatementChange(context.getMappedStatement().getId(), entry.getValue()));
			}
			return Collections.unmodifiableList(changes);
		} catch (RuntimeException exception) {
			handlePostFailure(context, safe(exception));
			return Collections.emptyList();
		}
	}

	public void dispatch(DataChangeExecutionContext context, List<StatementChange> statements) {
		if (context.isPostProcessingFailed() || statements == null || statements.isEmpty()) { return; }
		dispatcher.dispatch(new DataChangeEvent(context.getConsistencyLevel(), statements));
	}

	private FrameworkChangeMetadata resolveFramework(DataChangeExecutionContext context, BoundSqlContext boundSql,
			TableMonitor monitor, JdbcTableMetadata jdbcMetadata) {
		DataChangeFrameworkAdapter adapter = context.getFrameworkAdapter();
		if (adapter == null) {
			throw new DataChangeException(DataChangeErrorCode.CONFIGURATION,
					"DATA_CHANGE_FRAMEWORK_ADAPTER_UNAVAILABLE");
		}
		return adapter.resolve(new DataChangeInvocation(context.getMappedStatement(), boundSql.getBoundSql(),
				context.getParameterObject(), monitor, jdbcMetadata));
	}

	private List<RowIdentity> identities(DataChangeExecutionContext context, StatementChangePlan plan,
			Statement statement) {
		if (plan.getTarget().getOperation() == StatementOperation.INSERT) {
			return new GeneratedKeyResolver(plan.getDialect()).resolve(context.getMappedStatement(),
					context.getParameterObject(), statement, plan.getFrameworkMetadata(),
					plan.getTarget().getKeyColumnsHint());
		}
		List<RowIdentity> identities = new ArrayList<RowIdentity>();
		for (RowSnapshot row : plan.getBeforeRows()) { identities.add(row.getIdentity()); }
		return identities;
	}

	private int effectiveUpdateCount(StatementChangePlan plan, List<Integer> updateCounts) {
		if (plan.getUpdateCount() >= 0) { return plan.getUpdateCount(); }
		int index = plan.getStatementIndex();
		return updateCounts != null && index >= 0 && index < updateCounts.size()
				? updateCounts.get(index).intValue() : -1;
	}

	private int maxRows(TableMonitor monitor) {
		return monitor.getMaxSnapshotRows() == null ? configuration.getMaxSnapshotRows()
				: monitor.getMaxSnapshotRows().intValue();
	}

	private List<String> monitoredColumns(TableMonitor monitor) {
		List<String> columns = new ArrayList<String>(monitor.getIncludeColumns());
		columns.removeAll(monitor.getExcludeColumns());
		LogicalDeleteConfiguration logicalDelete = monitor.getLogicalDelete();
		if (logicalDelete != null && !containsIgnoreCase(columns, logicalDelete.getColumn())) {
			columns.add(logicalDelete.getColumn());
		}
		return columns;
	}

	private boolean containsIgnoreCase(List<String> columns, String candidate) {
		if (candidate == null) { return false; }
		for (String column : columns) {
			if (candidate.equalsIgnoreCase(column)) { return true; }
		}
		return false;
	}

	private boolean containsTargets(List<AnalyzedStatement> statements) {
		for (AnalyzedStatement statement : statements) {
			if (!statement.getTargets().isEmpty()) { return true; }
		}
		return false;
	}

	private void handlePostFailure(DataChangeExecutionContext context, DataChangeException exception) {
		if (context.getConsistencyLevel() == ConsistencyLevel.TRANSACTIONAL) { throw exception; }
		context.markPostProcessingFailed();
		if (errorHandler == null) { return; }
		try (DataChangeScope ignored = DataChangeScope.suppress()) {
			errorHandler.handle(new DataChangeFailure(
					new DataChangeEvent(context.getConsistencyLevel(), Collections.<StatementChange>emptyList()),
					exception));
		} catch (Exception ignored) {
			// Best-effort error reporting cannot change the business DML result.
		}
	}

	private DataChangeException safe(RuntimeException exception) {
		if (exception instanceof DataChangeException) {
			DataChangeException dataChange = (DataChangeException) exception;
			return new DataChangeException(dataChange.getCode(), dataChange.getCode().name());
		}
		return new DataChangeException(DataChangeErrorCode.UNKNOWN, "DATA_CHANGE_POST_PROCESSING_FAILED");
	}
}
