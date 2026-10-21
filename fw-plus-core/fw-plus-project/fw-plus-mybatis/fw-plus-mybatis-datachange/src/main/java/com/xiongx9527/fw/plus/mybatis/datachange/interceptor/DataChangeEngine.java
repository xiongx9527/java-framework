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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

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
		List<AnalyzedStatement.JdbcResultRequirement> jdbcResultRequirements = jdbcResultRequirements(analyzed);
		List<StatementChangePlan> plans = new ArrayList<StatementChangePlan>();
		for (AnalyzedStatement statement : analyzed) {
			for (ChangeTarget detectedTarget : statement.getTargets()) {
				Optional<TableMonitor> configured = monitors.find(detectedTarget.getTable(), dialect);
				if (!configured.isPresent()) { continue; }
				TableMonitor monitor = configured.get();
				JdbcTableMetadata jdbcMetadata = metadataResolver.resolve(connection, detectedTarget.getTable());
				FrameworkChangeMetadata framework = resolveFramework(context, boundSql, monitor, jdbcMetadata);
				List<String> keys = identityResolver.resolve(monitor, framework, jdbcMetadata);
				keys = dialect.resolveIdentityColumns(connection, detectedTarget, keys);
				ChangeTarget target = detectedTarget.withSnapshotConfiguration(keys, monitoredColumns(monitor),
						maxRows(monitor), context.getConsistencyLevel() == ConsistencyLevel.TRANSACTIONAL);
				List<RowSnapshot> before = target.getOperation() == StatementOperation.INSERT && !target.isUpsert()
						? Collections.<RowSnapshot>emptyList()
						: snapshots.readBefore(connection, dialect, target, jdbcMetadata,
								context.getMappedStatement().getConfiguration());
				plans.add(new StatementChangePlan(statement.getStatementIndex(), jdbcResultRequirements, target, monitor,
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
		captureStatementResults(context, plans, statement,
				Collections.singletonList(JdbcStatementResult.updateCount(updateCount)));
	}

	public void captureStatementResults(DataChangeExecutionContext context, List<StatementChangePlan> plans,
			Statement statement, List<JdbcStatementResult> results) {
		if (plans == null || plans.isEmpty()) { return; }
		try {
			for (StatementChangePlan plan : plans) {
				int updateCount = countAt(results, plans, plan);
				List<RowIdentity> identities = !isSuccessful(updateCount)
						? Collections.<RowIdentity>emptyList() : identities(context, plan, statement, plans, results);
				plan.captureResult(updateCount, identities);
			}
		} catch (RuntimeException exception) {
			handlePostFailure(context, safe(exception));
		}
	}

	public List<StatementChange> afterStatement(DataChangeExecutionContext context,
			List<StatementChangePlan> plans, List<JdbcStatementResult> results) {
		if (context.isPostProcessingFailed() || plans == null || plans.isEmpty()) {
			return Collections.emptyList();
		}
		try {
			Map<Integer, List<TableChange>> grouped = new LinkedHashMap<Integer, List<TableChange>>();
			Map<Integer, Integer> affectedRows = new LinkedHashMap<Integer, Integer>();
			for (StatementChangePlan plan : plans) {
				int count = effectiveUpdateCount(plan, plans, results);
				if (!isSuccessful(count)) { continue; }
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
				affectedRows.put(Integer.valueOf(plan.getStatementIndex()), Integer.valueOf(count));
			}
			List<StatementChange> changes = new ArrayList<StatementChange>();
			for (Map.Entry<Integer, List<TableChange>> entry : grouped.entrySet()) {
				Integer count = affectedRows.get(entry.getKey());
				changes.add(new StatementChange(context.getMappedStatement().getId(), entry.getKey().intValue(),
						count == null ? -1 : count.intValue(), entry.getValue()));
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

	void handlePostProcessingFailure(DataChangeExecutionContext context, RuntimeException exception) {
		handlePostFailure(context, safe(exception));
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
			Statement statement, List<StatementChangePlan> plans, List<JdbcStatementResult> results) {
		if (plan.getTarget().getOperation() == StatementOperation.INSERT && !plan.getTarget().isUpsert()) {
			if (plan.getTarget().isReturnsRows()) { return returnedIdentities(plan, plans, results); }
			return new GeneratedKeyResolver(plan.getDialect()).resolve(context.getMappedStatement(),
					context.getParameterObject(), statement, plan.getFrameworkMetadata(),
					plan.getTarget().getKeyColumnsHint());
		}
		List<RowIdentity> identities = new ArrayList<RowIdentity>();
		for (RowSnapshot row : plan.getBeforeRows()) { identities.add(row.getIdentity()); }
		return identities;
	}

	private List<RowIdentity> returnedIdentities(StatementChangePlan plan, List<StatementChangePlan> plans,
			List<JdbcStatementResult> results) {
		JdbcStatementResult result = resultAt(results, plans, plan);
		if (result == null || !result.hasCapturedRows()) { throw identityUnavailable(); }
		List<RowIdentity> identities = new ArrayList<RowIdentity>();
		for (Map<String, Object> row : result.getRows()) {
			Map<String, Object> keys = new LinkedHashMap<String, Object>();
			for (String key : plan.getTarget().getKeyColumnsHint()) {
				Object value = valueIgnoreCase(row, key);
				if (value == null) { throw identityUnavailable(); }
				keys.put(key, value);
			}
			identities.add(new RowIdentity(keys));
		}
		return Collections.unmodifiableList(identities);
	}

	private Object valueIgnoreCase(Map<String, Object> values, String key) {
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			if (key.equalsIgnoreCase(entry.getKey())) { return entry.getValue(); }
		}
		return null;
	}

	private DataChangeException identityUnavailable() {
		return new DataChangeException(DataChangeErrorCode.ROW_IDENTITY_UNAVAILABLE,
				"ROW_IDENTITY_UNAVAILABLE");
	}

	private int effectiveUpdateCount(StatementChangePlan plan, List<StatementChangePlan> plans,
			List<JdbcStatementResult> results) {
		if (plan.getUpdateCount() != -1) { return plan.getUpdateCount(); }
		return countAt(results, plans, plan);
	}

	private int countAt(List<JdbcStatementResult> results, List<StatementChangePlan> plans,
			StatementChangePlan current) {
		StatementResultBoundary boundary = boundaryAt(results, plans, current);
		if (boundary == null) { return -1; }
		JdbcStatementResult result = results.get(boundary.resultIndex);
		if (current.getTarget().isReturnsRows() && result.getType() == JdbcStatementResult.Type.RESULT_SET) {
			if (boundary.updateCountIndex >= 0) { return countAt(results, boundary.updateCountIndex); }
			return result.hasCapturedRows() ? result.getRows().size() : -1;
		}
		return countAt(results, boundary.resultIndex);
	}

	private JdbcStatementResult resultAt(List<JdbcStatementResult> results, List<StatementChangePlan> plans,
			StatementChangePlan current) {
		StatementResultBoundary boundary = boundaryAt(results, plans, current);
		return boundary == null ? null : results.get(boundary.resultIndex);
	}

	private StatementResultBoundary boundaryAt(List<JdbcStatementResult> results, List<StatementChangePlan> plans,
			StatementChangePlan current) {
		if (results == null) { return null; }
		Set<StatementResultBoundary> candidates = new LinkedHashSet<StatementResultBoundary>();
		mapResults(results, plans, current, 0, 0, -1, -1, candidates,
				new HashSet<ResultMappingState>());
		return candidates.size() == 1 ? candidates.iterator().next() : null;
	}

	private void mapResults(List<JdbcStatementResult> results, List<StatementChangePlan> plans,
			StatementChangePlan current, int statementIndex, int resultIndex,
			int currentResultIndex, int currentUpdateCountIndex,
			Set<StatementResultBoundary> candidates, Set<ResultMappingState> visited) {
		if (candidates.size() > 1 || !visited.add(new ResultMappingState(statementIndex, resultIndex,
				currentResultIndex, currentUpdateCountIndex))) { return; }
		if (statementIndex == current.getStatementCount()) {
			if (resultIndex == results.size() && currentResultIndex >= 0) {
				candidates.add(new StatementResultBoundary(currentResultIndex, currentUpdateCountIndex));
			}
			return;
		}
		AnalyzedStatement.JdbcResultRequirement requirement =
				current.getJdbcResultRequirements().get(statementIndex);
		if (requirement == AnalyzedStatement.JdbcResultRequirement.OPTIONAL) {
			mapResults(results, plans, current, statementIndex + 1, resultIndex,
					currentResultIndex, currentUpdateCountIndex, candidates, visited);
			if (resultIndex < results.size()) {
				mapResults(results, plans, current, statementIndex + 1, resultIndex + 1,
						currentResultIndex, currentUpdateCountIndex, candidates, visited);
			}
			return;
		}
		if (resultIndex >= results.size()) { return; }
		boolean returning = requirement == AnalyzedStatement.JdbcResultRequirement.RETURNING;
		if (returning && !isResultSet(results, resultIndex)
				|| requirement == AnalyzedStatement.JdbcResultRequirement.RESULT_SET
						&& !isResultSet(results, resultIndex)
				|| requirement == AnalyzedStatement.JdbcResultRequirement.UPDATE_COUNT
						&& !isUpdateCount(results, resultIndex)) { return; }
		int capturedResult = statementIndex == current.getStatementIndex()
				? resultIndex : currentResultIndex;
		mapResults(results, plans, current, statementIndex + 1, resultIndex + 1,
				capturedResult, currentUpdateCountIndex, candidates, visited);
		if (returning && isUpdateCount(results, resultIndex + 1)) {
			int capturedCount = statementIndex == current.getStatementIndex()
					? resultIndex + 1 : currentUpdateCountIndex;
			mapResults(results, plans, current, statementIndex + 1, resultIndex + 2,
					capturedResult, capturedCount, candidates, visited);
		}
	}

	private List<AnalyzedStatement.JdbcResultRequirement> jdbcResultRequirements(
			List<AnalyzedStatement> statements) {
		List<AnalyzedStatement.JdbcResultRequirement> requirements =
				new ArrayList<AnalyzedStatement.JdbcResultRequirement>();
		for (AnalyzedStatement statement : statements) {
			requirements.add(statement.getJdbcResultRequirement());
		}
		return Collections.unmodifiableList(requirements);
	}

	private boolean isResultSet(List<JdbcStatementResult> results, int index) {
		return results != null && index >= 0 && index < results.size()
				&& results.get(index) != null
				&& results.get(index).getType() == JdbcStatementResult.Type.RESULT_SET;
	}

	private boolean isUpdateCount(List<JdbcStatementResult> results, int index) {
		return results != null && index >= 0 && index < results.size()
				&& results.get(index) != null && results.get(index).isUpdateCount();
	}

	private int countAt(List<JdbcStatementResult> results, int index) {
		if (results == null || index < 0 || index >= results.size()) { return -1; }
		JdbcStatementResult result = results.get(index);
		return result != null && result.isUpdateCount() ? result.getUpdateCount() : -1;
	}

	private static final class StatementResultBoundary {
		private final int resultIndex;
		private final int updateCountIndex;

		private StatementResultBoundary(int resultIndex, int updateCountIndex) {
			this.resultIndex = resultIndex;
			this.updateCountIndex = updateCountIndex;
		}

		@Override
		public boolean equals(Object value) {
			if (!(value instanceof StatementResultBoundary)) { return false; }
			StatementResultBoundary other = (StatementResultBoundary) value;
			return resultIndex == other.resultIndex && updateCountIndex == other.updateCountIndex;
		}

		@Override
		public int hashCode() { return 31 * resultIndex + updateCountIndex; }
	}

	private static final class ResultMappingState {
		private final int statementIndex;
		private final int resultIndex;
		private final int currentResultIndex;
		private final int currentUpdateCountIndex;

		private ResultMappingState(int statementIndex, int resultIndex,
				int currentResultIndex, int currentUpdateCountIndex) {
			this.statementIndex = statementIndex;
			this.resultIndex = resultIndex;
			this.currentResultIndex = currentResultIndex;
			this.currentUpdateCountIndex = currentUpdateCountIndex;
		}

		@Override
		public boolean equals(Object value) {
			if (!(value instanceof ResultMappingState)) { return false; }
			ResultMappingState other = (ResultMappingState) value;
			return statementIndex == other.statementIndex && resultIndex == other.resultIndex
					&& currentResultIndex == other.currentResultIndex
					&& currentUpdateCountIndex == other.currentUpdateCountIndex;
		}

		@Override
		public int hashCode() {
			int hash = 31 * statementIndex + resultIndex;
			hash = 31 * hash + currentResultIndex;
			return 31 * hash + currentUpdateCountIndex;
		}
	}

	private boolean isSuccessful(int count) {
		return count > 0 || count == Statement.SUCCESS_NO_INFO;
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
			return new DataChangeException(dataChange.getErrorCode(), dataChange.getErrorCode().name());
		}
		return new DataChangeException(DataChangeErrorCode.UNKNOWN, "DATA_CHANGE_POST_PROCESSING_FAILED");
	}
}
