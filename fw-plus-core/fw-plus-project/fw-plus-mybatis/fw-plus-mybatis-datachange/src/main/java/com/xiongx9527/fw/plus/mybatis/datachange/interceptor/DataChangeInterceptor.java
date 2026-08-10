package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.StatementChange;
import com.xiongx9527.fw.plus.mybatis.datachange.config.DataChangeConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.BoundSqlContext;
import org.apache.ibatis.cache.CacheKey;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

@Intercepts({
		@Signature(type = Executor.class, method = "update",
				args = {MappedStatement.class, Object.class}),
		@Signature(type = StatementHandler.class, method = "prepare",
				args = {Connection.class, Integer.class}),
		@Signature(type = StatementHandler.class, method = "update",
				args = {Statement.class}),
		@Signature(type = StatementHandler.class, method = "batch",
				args = {Statement.class}),
		@Signature(type = Executor.class, method = "flushStatements", args = {}),
		@Signature(type = Executor.class, method = "commit", args = {boolean.class}),
		@Signature(type = Executor.class, method = "rollback", args = {boolean.class}),
		@Signature(type = Executor.class, method = "close", args = {boolean.class}),
		@Signature(type = Executor.class, method = "query",
				args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
		@Signature(type = Executor.class, method = "query",
				args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
						CacheKey.class, BoundSql.class})
})
public final class DataChangeInterceptor implements Interceptor {
	private final DataChangeConfiguration configuration;
	private final DataChangeEngine engine;
	private final DataChangeBatchCoordinator batchCoordinator;
	private final JdbcUpdateCountCollector updateCountCollector = new JdbcUpdateCountCollector();

	public DataChangeInterceptor(DataChangeConfiguration configuration, DataChangeEngine engine) {
		this.configuration = configuration;
		this.engine = engine;
		this.batchCoordinator = new DataChangeBatchCoordinator(engine);
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		String method = invocation.getMethod().getName();
		if (invocation.getTarget() instanceof Executor && "update".equals(method)) {
			return interceptExecutorUpdate(invocation);
		}
		if (invocation.getTarget() instanceof Executor && "flushStatements".equals(method)) {
			return interceptFlushStatements(invocation);
		}
		if (invocation.getTarget() instanceof Executor && "commit".equals(method)) {
			return interceptFlushBefore(invocation);
		}
		if (invocation.getTarget() instanceof Executor && "rollback".equals(method)) {
			return interceptRollback(invocation);
		}
		if (invocation.getTarget() instanceof Executor && "close".equals(method)) {
			return interceptClose(invocation);
		}
		if (invocation.getTarget() instanceof Executor && "query".equals(method)) {
			return interceptFlushBefore(invocation);
		}
		if (invocation.getTarget() instanceof StatementHandler && "prepare".equals(method)) {
			return interceptPrepare(invocation);
		}
		if (invocation.getTarget() instanceof StatementHandler && "update".equals(method)) {
			return interceptStatementUpdate(invocation);
		}
		if (invocation.getTarget() instanceof StatementHandler && "batch".equals(method)) {
			return interceptStatementBatch(invocation);
		}
		return invocation.proceed();
	}

	private Object interceptExecutorUpdate(Invocation invocation) throws Throwable {
		if (!configuration.isEnabled() || DataChangeScopeHolder.isSuppressed()) {
			return invocation.proceed();
		}
		MappedStatement mappedStatement = (MappedStatement) invocation.getArgs()[0];
		Object parameterObject = invocation.getArgs()[1];
		if (!isDml(mappedStatement.getSqlCommandType())) { return invocation.proceed(); }
		DataChangeFrameworkAdapter adapter = engine.selectAdapter(mappedStatement, parameterObject);
		if (adapter == null) { return invocation.proceed(); }
		DataChangeExecutionContext context = new DataChangeExecutionContext(mappedStatement, parameterObject, adapter,
				invocation.getTarget());
		try (DataChangeExecutionContextHolder.Scope ignored = DataChangeExecutionContextHolder.push(context)) {
			Object result = invocation.proceed();
			if (context.isBatch()) { return result; }
			List<StatementChange> statements = engine.afterStatement(context, context.getPlans(),
					context.getStatementResults());
			engine.dispatch(context, statements);
			return result;
		}
	}

	private Object interceptPrepare(Invocation invocation) throws Throwable {
		Object result = invocation.proceed();
		if (DataChangeScopeHolder.isSuppressed()) { return result; }
		Optional<DataChangeExecutionContext> current = DataChangeExecutionContextHolder.current();
		if (!current.isPresent()) { return result; }
		try {
			DataChangeExecutionContext context = current.get();
			Connection connection = (Connection) invocation.getArgs()[0];
			engine.initializeConnection(context, connection);
			StatementHandler handler = (StatementHandler) invocation.getTarget();
			List<StatementChangePlan> plans = engine.beforeStatement(context,
					new BoundSqlContext(handler.getBoundSql(),
							context.getConsistencyLevel() == com.xiongx9527.fw.plus.mybatis.datachange.api.ConsistencyLevel.TRANSACTIONAL,
							configuration.getMaxSnapshotRows()));
			context.registerPlans(plans);
			return result;
		} catch (Throwable failure) {
			closeSafely(result);
			throw failure;
		}
	}

	private void closeSafely(Object result) {
		if (!(result instanceof Statement)) { return; }
		try {
			((Statement) result).close();
		} catch (Throwable ignored) {
			// Cleanup failure must not replace or expose details from the audit preparation failure.
		}
	}

	private Object interceptStatementUpdate(Invocation invocation) throws Throwable {
		Object result = invocation.proceed();
		if (DataChangeScopeHolder.isSuppressed()) { return result; }
		Optional<DataChangeExecutionContext> current = DataChangeExecutionContextHolder.current();
		if (!current.isPresent() || !(result instanceof Number)) { return result; }
		DataChangeExecutionContext context = current.get();
		int updateCount = ((Number) result).intValue();
		List<JdbcStatementResult> statementResults;
		try {
			List<StatementChangePlan> plans = context.getCurrentPlans();
			List<String> returningColumns = returningColumns(plans);
			statementResults = returningColumns.isEmpty()
					? updateCountCollector.collect((Statement) invocation.getArgs()[0], updateCount)
					: updateCountCollector.collect((Statement) invocation.getArgs()[0], updateCount,
							returningColumns, returningMaxRows(plans));
		} catch (SQLException failure) {
			engine.handlePostProcessingFailure(context, new DataChangeException(DataChangeErrorCode.UNKNOWN,
					"DATA_CHANGE_UPDATE_COUNT_UNAVAILABLE"));
			return result;
		}
		context.addStatementResults(statementResults);
		engine.captureStatementResults(context, context.getCurrentPlans(),
				(Statement) invocation.getArgs()[0], statementResults);
		return result;
	}

	private List<String> returningColumns(List<StatementChangePlan> plans) {
		Set<String> columns = new LinkedHashSet<String>();
		for (StatementChangePlan plan : plans) {
			if (plan.getTarget().isReturnsRows()) { columns.addAll(plan.getTarget().getKeyColumnsHint()); }
		}
		return new ArrayList<String>(columns);
	}

	private int returningMaxRows(List<StatementChangePlan> plans) {
		int maxRows = Integer.MAX_VALUE;
		for (StatementChangePlan plan : plans) {
			if (plan.getTarget().isReturnsRows()) { maxRows = Math.min(maxRows, plan.getTarget().getMaxRows()); }
		}
		return maxRows == Integer.MAX_VALUE ? configuration.getMaxSnapshotRows() : maxRows;
	}

	private Object interceptStatementBatch(Invocation invocation) throws Throwable {
		if (DataChangeScopeHolder.isSuppressed()) { return invocation.proceed(); }
		Optional<DataChangeExecutionContext> current = DataChangeExecutionContextHolder.current();
		if (!current.isPresent()) { return invocation.proceed(); }
		DataChangeExecutionContext context = current.get();
		Statement statement = (Statement) invocation.getArgs()[0];
		StatementHandler handler = (StatementHandler) invocation.getTarget();
		BoundSql boundSql = handler.getBoundSql();
		context.markBatch(statement, boundSql.getSql());
		engine.initializeConnection(context, statement.getConnection());
		List<StatementChangePlan> plans = engine.beforeStatement(context, new BoundSqlContext(boundSql,
				context.getConsistencyLevel() == com.xiongx9527.fw.plus.mybatis.datachange.api.ConsistencyLevel.TRANSACTIONAL,
				configuration.getMaxSnapshotRows()));
		context.registerPlans(plans);
		batchCoordinator.collect(context, plans);
		try {
			return invocation.proceed();
		} catch (Throwable failure) {
			batchCoordinator.discard(context);
			throw failure;
		}
	}

	private Object interceptFlushStatements(Invocation invocation) throws Throwable {
		DataChangeExecutionContext context = lifecycleContext(invocation);
		if (!batchCoordinator.hasPending(context)) { return invocation.proceed(); }
		try {
			Object result = invocation.proceed();
			batchCoordinator.flush(context, batchResults(result));
			return result;
		} catch (Throwable failure) {
			batchCoordinator.discard(context);
			throw failure;
		}
	}

	private Object interceptFlushBefore(Invocation invocation) throws Throwable {
		DataChangeExecutionContext context = lifecycleContext(invocation);
		if (batchCoordinator.hasPending(context)) {
			try {
				List<BatchResult> results = ((Executor) invocation.getTarget()).flushStatements();
				batchCoordinator.flush(context, results);
			} catch (Throwable failure) {
				batchCoordinator.discard(context);
				throw failure;
			}
		}
		return invocation.proceed();
	}

	private Object interceptRollback(Invocation invocation) throws Throwable {
		batchCoordinator.discard(lifecycleContext(invocation));
		return invocation.proceed();
	}

	private Object interceptClose(Invocation invocation) throws Throwable {
		batchCoordinator.discard(lifecycleContext(invocation));
		return invocation.proceed();
	}

	private DataChangeExecutionContext lifecycleContext(Invocation invocation) {
		return new DataChangeExecutionContext(null, null, null, invocation.getTarget());
	}

	@SuppressWarnings("unchecked")
	private List<BatchResult> batchResults(Object result) {
		return result instanceof List ? (List<BatchResult>) result : java.util.Collections.<BatchResult>emptyList();
	}

	private boolean isDml(SqlCommandType commandType) {
		return commandType == SqlCommandType.INSERT || commandType == SqlCommandType.UPDATE
				|| commandType == SqlCommandType.DELETE;
	}

	@Override
	public Object plugin(Object target) { return Plugin.wrap(target, this); }

	@Override
	public void setProperties(Properties properties) {
		// Constructor configuration is authoritative.
	}
}
