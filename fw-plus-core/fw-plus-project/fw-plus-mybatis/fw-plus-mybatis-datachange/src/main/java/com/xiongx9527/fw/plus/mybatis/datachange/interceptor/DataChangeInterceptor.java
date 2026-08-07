package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.api.StatementChange;
import com.xiongx9527.fw.plus.mybatis.datachange.config.DataChangeConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.BoundSqlContext;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.plugin.Signature;
import org.apache.ibatis.executor.statement.StatementHandler;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Intercepts({
		@Signature(type = Executor.class, method = "update",
				args = {MappedStatement.class, Object.class}),
		@Signature(type = StatementHandler.class, method = "prepare",
				args = {Connection.class, Integer.class}),
		@Signature(type = StatementHandler.class, method = "update",
				args = {Statement.class})
})
public final class DataChangeInterceptor implements Interceptor {
	private final DataChangeConfiguration configuration;
	private final DataChangeEngine engine;

	public DataChangeInterceptor(DataChangeConfiguration configuration, DataChangeEngine engine) {
		this.configuration = configuration;
		this.engine = engine;
	}

	@Override
	public Object intercept(Invocation invocation) throws Throwable {
		String method = invocation.getMethod().getName();
		if (invocation.getTarget() instanceof Executor && "update".equals(method)) {
			return interceptExecutorUpdate(invocation);
		}
		if (invocation.getTarget() instanceof StatementHandler && "prepare".equals(method)) {
			return interceptPrepare(invocation);
		}
		if (invocation.getTarget() instanceof StatementHandler && "update".equals(method)) {
			return interceptStatementUpdate(invocation);
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
		DataChangeExecutionContext context = new DataChangeExecutionContext(mappedStatement, parameterObject, adapter);
		try (DataChangeExecutionContextHolder.Scope ignored = DataChangeExecutionContextHolder.push(context)) {
			Object result = invocation.proceed();
			List<StatementChange> statements = engine.afterStatement(context, context.getPlans(),
					context.getUpdateCounts());
			engine.dispatch(context, statements);
			return result;
		}
	}

	private Object interceptPrepare(Invocation invocation) throws Throwable {
		Object result = invocation.proceed();
		if (DataChangeScopeHolder.isSuppressed()) { return result; }
		Optional<DataChangeExecutionContext> current = DataChangeExecutionContextHolder.current();
		if (!current.isPresent()) { return result; }
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
	}

	private Object interceptStatementUpdate(Invocation invocation) throws Throwable {
		Object result = invocation.proceed();
		if (DataChangeScopeHolder.isSuppressed()) { return result; }
		Optional<DataChangeExecutionContext> current = DataChangeExecutionContextHolder.current();
		if (!current.isPresent() || !(result instanceof Number)) { return result; }
		DataChangeExecutionContext context = current.get();
		int updateCount = ((Number) result).intValue();
		context.addUpdateCount(updateCount);
		engine.captureStatementResult(context, context.getCurrentPlans(),
				(Statement) invocation.getArgs()[0], updateCount);
		return result;
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
