package com.xiongx9527.fw.plus.graph.jdbc;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GraphJdbcStatementSupport {

	private GraphJdbcStatementSupport() {
	}

	public static Statement statement(Connection connection, GraphJdbcExecutor executor,
			GraphJdbcSqlPreprocessor preprocessor, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) {
		return (Statement) Proxy.newProxyInstance(GraphJdbcStatementSupport.class.getClassLoader(),
				new Class<?>[] {Statement.class}, new StatementHandler(connection, executor, preprocessor, null,
						resultSetType, resultSetConcurrency, resultSetHoldability));
	}

	public static PreparedStatement preparedStatement(Connection connection, GraphJdbcExecutor executor,
			GraphJdbcSqlPreprocessor preprocessor, String sql, int resultSetType, int resultSetConcurrency,
			int resultSetHoldability) {
		return (PreparedStatement) Proxy.newProxyInstance(GraphJdbcStatementSupport.class.getClassLoader(),
				new Class<?>[] {PreparedStatement.class}, new StatementHandler(connection, executor, preprocessor, sql,
						resultSetType, resultSetConcurrency, resultSetHoldability));
	}

	private static final class StatementHandler implements InvocationHandler {

		private final Connection connection;
		private final GraphJdbcExecutor executor;
		private final GraphJdbcSqlPreprocessor preprocessor;
		private final String preparedSql;
		private final GraphJdbcParameterBindings bindings = new GraphJdbcParameterBindings();
		private final GraphJdbcLifecycle lifecycle = new GraphJdbcLifecycle();
		private final int resultSetType;
		private final int resultSetConcurrency;
		private final int resultSetHoldability;
		private final List<BatchEntry> batch = new ArrayList<BatchEntry>();
		private ResultSet currentResultSet;
		private int updateCount = -1;
		private int maxRows;
		private int maxFieldSize;
		private int fetchSize;
		private int fetchDirection = ResultSet.FETCH_FORWARD;
		private int queryTimeout;
		private boolean closeOnCompletion;
		private boolean poolable;

		private StatementHandler(Connection connection, GraphJdbcExecutor executor, GraphJdbcSqlPreprocessor preprocessor,
				String preparedSql, int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
			if (executor == null) {
				throw new IllegalArgumentException("executor must not be null");
			}
			if (preprocessor == null) {
				throw new IllegalArgumentException("preprocessor must not be null");
			}
			this.connection = connection;
			this.executor = executor;
			this.preprocessor = preprocessor;
			this.preparedSql = preparedSql;
			this.resultSetType = resultSetType;
			this.resultSetConcurrency = resultSetConcurrency;
			this.resultSetHoldability = resultSetHoldability;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			String name = method.getName();
			if ("equals".equals(name)) {
				return Boolean.valueOf(proxy == args[0]);
			}
			if ("hashCode".equals(name)) {
				return Integer.valueOf(System.identityHashCode(proxy));
			}
			if ("toString".equals(name)) {
				return preparedSql == null ? "GraphJdbcStatement" : "GraphJdbcPreparedStatement";
			}
			if (isPreparedStringExecution(name, args)) {
				throw new SQLException(name + "(String) cannot be called on PreparedStatement.");
			}
			if ("close".equals(name)) {
				lifecycle.close(new GraphJdbcLifecycle.ThrowingCloseAction() {
					@Override
					public void close() throws SQLException {
						if (currentResultSet != null) {
							currentResultSet.close();
						}
					}
				});
				return null;
			}
			if ("isClosed".equals(name)) {
				return Boolean.valueOf(lifecycle.isClosed());
			}
			checkOpen();
			if ("executeQuery".equals(name)) {
				return executeQuery(sqlArgument(args));
			}
			if ("executeUpdate".equals(name)) {
				return Integer.valueOf(executeUpdate(sqlArgument(args)));
			}
			if ("execute".equals(name)) {
				return Boolean.valueOf(execute(sqlArgument(args), bindings));
			}
			if ("getResultSet".equals(name)) {
				return currentResultSet;
			}
			if ("getUpdateCount".equals(name)) {
				return Integer.valueOf(updateCount);
			}
			if ("getConnection".equals(name)) {
				return connection;
			}
			if ("clearParameters".equals(name)) {
				bindings.clear();
				return null;
			}
			if ("setNull".equals(name) && isParameterSetter(args)) {
				bindings.bind(((Integer) args[0]).intValue(), null, name, null);
				return null;
			}
			if (isParameterSetter(args) && name.startsWith("set")) {
				bindings.bind(((Integer) args[0]).intValue(), args[1], name, streamLength(name, args));
				return null;
			}
			if ("addBatch".equals(name)) {
				batch.add(new BatchEntry(sqlArgument(args), copyOf(bindings)));
				return null;
			}
			if ("clearBatch".equals(name)) {
				batch.clear();
				return null;
			}
			if ("executeBatch".equals(name)) {
				return executeBatch();
			}
			if ("getGeneratedKeys".equals(name)) {
				return GraphJdbcRowSets.emptyGeneratedKeys();
			}
			if ("getParameterMetaData".equals(name)) {
				return parameterMetaData();
			}
			if ("getResultSetType".equals(name)) {
				return Integer.valueOf(resultSetType);
			}
			if ("getResultSetConcurrency".equals(name)) {
				return Integer.valueOf(resultSetConcurrency);
			}
			if ("getResultSetHoldability".equals(name)) {
				return Integer.valueOf(resultSetHoldability);
			}
			if ("cancel".equals(name)) {
				return null;
			}
			if ("getMaxRows".equals(name)) {
				return Integer.valueOf(maxRows);
			}
			if ("setMaxRows".equals(name)) {
				maxRows = ((Integer) args[0]).intValue();
				return null;
			}
			if ("getMaxFieldSize".equals(name)) {
				return Integer.valueOf(maxFieldSize);
			}
			if ("setMaxFieldSize".equals(name)) {
				maxFieldSize = ((Integer) args[0]).intValue();
				return null;
			}
			if ("getFetchSize".equals(name)) {
				return Integer.valueOf(fetchSize);
			}
			if ("setFetchSize".equals(name)) {
				fetchSize = ((Integer) args[0]).intValue();
				return null;
			}
			if ("getFetchDirection".equals(name)) {
				return Integer.valueOf(fetchDirection);
			}
			if ("setFetchDirection".equals(name)) {
				fetchDirection = ((Integer) args[0]).intValue();
				return null;
			}
			if ("getQueryTimeout".equals(name)) {
				return Integer.valueOf(queryTimeout);
			}
			if ("setQueryTimeout".equals(name)) {
				queryTimeout = ((Integer) args[0]).intValue();
				return null;
			}
			if ("getMoreResults".equals(name)) {
				currentResultSet = null;
				updateCount = -1;
				return Boolean.FALSE;
			}
			if ("closeOnCompletion".equals(name)) {
				closeOnCompletion = true;
				return null;
			}
			if ("isCloseOnCompletion".equals(name)) {
				return Boolean.valueOf(closeOnCompletion);
			}
			if ("setPoolable".equals(name)) {
				poolable = ((Boolean) args[0]).booleanValue();
				return null;
			}
			if ("isPoolable".equals(name)) {
				return Boolean.valueOf(poolable);
			}
			if ("unwrap".equals(name)) {
				return GraphJdbcWrapperSupport.unwrap(proxy, (Class<?>) args[0]);
			}
			if ("isWrapperFor".equals(name)) {
				return Boolean.valueOf(GraphJdbcWrapperSupport.isWrapperFor(proxy, (Class<?>) args[0]));
			}
			return GraphJdbcDefaults.defaultValue(method.getReturnType());
		}

		private boolean isPreparedStringExecution(String name, Object[] args) {
			return preparedSql != null && args != null && args.length > 0 && args[0] instanceof String
					&& ("executeQuery".equals(name) || "executeUpdate".equals(name) || "execute".equals(name));
		}

		private void checkOpen() throws SQLException {
			lifecycle.checkOpen("Statement");
			if (connection != null && connection.isClosed()) {
				throw new SQLException("Connection is closed");
			}
		}

		private String sqlArgument(Object[] args) throws SQLException {
			if (args == null || args.length == 0) {
				if (preparedSql == null) {
					throw new SQLException("SQL must be provided for Statement execution.");
				}
				return preparedSql;
			}
			return (String) args[0];
		}

		private ResultSet executeQuery(String sql) throws SQLException {
			if (!execute(sql, bindings)) {
				throw new SQLException("SQL did not produce a ResultSet and cannot be used with executeQuery.");
			}
			return currentResultSet;
		}

		private int executeUpdate(String sql) throws SQLException {
			if (execute(sql, bindings)) {
				throw new SQLException("SQL produced a ResultSet and cannot be used with executeUpdate.");
			}
			return updateCount < 0 ? 0 : updateCount;
		}

		private boolean execute(String sql, GraphJdbcParameterBindings executionBindings) throws SQLException {
			GraphJdbcResult result = executor.execute(preprocessor.preprocess(sql), executionBindings);
			if (result == null) {
				throw new SQLException("Graph JDBC executor returned no result.");
			}
			if (result.hasResultSet()) {
				currentResultSet = GraphJdbcRowSets.resultSet(result);
				updateCount = -1;
				return true;
			}
			currentResultSet = null;
			updateCount = result.getUpdateCount();
			return false;
		}

		private int[] executeBatch() throws SQLException {
			int[] counts = new int[batch.size()];
			try {
				for (int index = 0; index < batch.size(); index++) {
					BatchEntry entry = batch.get(index);
					execute(entry.sql, entry.bindings);
					counts[index] = updateCount < 0 ? Statement.SUCCESS_NO_INFO : updateCount;
				}
				return counts;
			}
			finally {
				batch.clear();
			}
		}

		private ParameterMetaData parameterMetaData() throws SQLException {
			if (preparedSql == null) {
				throw GraphJdbcDefaults.unsupportedOperation();
			}
			return new GraphJdbcParameterMetaData(GraphJdbcParameterBindings.countQuestionMarks(preparedSql));
		}

		private static boolean isParameterSetter(Object[] args) {
			return args != null && args.length >= 2 && args[0] instanceof Integer;
		}

		private static Long streamLength(String setterName, Object[] args) {
			if (args == null || args.length < 3 || !(args[2] instanceof Number)) {
				return null;
			}
			if ("setAsciiStream".equals(setterName) || "setUnicodeStream".equals(setterName)
					|| "setBinaryStream".equals(setterName) || "setCharacterStream".equals(setterName)
					|| "setNCharacterStream".equals(setterName) || "setBlob".equals(setterName)
					|| "setClob".equals(setterName) || "setNClob".equals(setterName)) {
				return Long.valueOf(((Number) args[2]).longValue());
			}
			return null;
		}

		private static GraphJdbcParameterBindings copyOf(GraphJdbcParameterBindings source) {
			GraphJdbcParameterBindings copy = new GraphJdbcParameterBindings();
			for (Map.Entry<Integer, Object> entry : source.asMap().entrySet()) {
				GraphJdbcParameterBindings.Binding binding = source.getBinding(entry.getKey().intValue());
				copy.bind(entry.getKey().intValue(), entry.getValue(), binding == null ? null : binding.getSetterName(),
						binding == null ? null : binding.getLength());
			}
			return copy;
		}
	}

	private static final class BatchEntry {

		private final String sql;
		private final GraphJdbcParameterBindings bindings;

		private BatchEntry(String sql, GraphJdbcParameterBindings bindings) {
			this.sql = sql;
			this.bindings = bindings;
		}
	}
}
