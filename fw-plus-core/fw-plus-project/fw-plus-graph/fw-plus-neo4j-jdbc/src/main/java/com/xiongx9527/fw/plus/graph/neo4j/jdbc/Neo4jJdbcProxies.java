package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcDefaults;
import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcColumn;
import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcMetaDataSupport;
import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcParameterMetaData;
import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcRowSets;
import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcSqlPreprocessor;
import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcStatementSupport;
import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcWrapperSupport;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.Map;

final class Neo4jJdbcProxies {

    private static final GraphJdbcSqlPreprocessor SQL_PREPROCESSOR = new GraphJdbcSqlPreprocessor() {
        @Override
        public String preprocess(String sql) throws SQLException {
            return Neo4jSqlToCypherTranslator.translate(sql);
        }
    };

    private static final GraphJdbcSqlPreprocessor PREPARED_SQL_PREPROCESSOR = new GraphJdbcSqlPreprocessor() {
        @Override
        public String preprocess(String sql) throws SQLException {
            return Neo4jSqlToCypherTranslator.translate(Neo4jGraphJdbcExecutor.namedParameters(sql));
        }
    };

    private Neo4jJdbcProxies() {
    }

    static Statement statement(Neo4jConnection connection, Neo4jQueryExecutor executor, int resultSetType,
                               int resultSetConcurrency, int resultSetHoldability) {
        return withGeneratedKeys(GraphJdbcStatementSupport.statement(connection, new Neo4jGraphJdbcExecutor(executor),
                SQL_PREPROCESSOR, resultSetType, resultSetConcurrency, resultSetHoldability), Statement.class);
    }

    static PreparedStatement preparedStatement(Neo4jConnection connection, Neo4jQueryExecutor executor, String cypher) {
        return preparedStatement(connection, executor, cypher, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    static PreparedStatement preparedStatement(Neo4jConnection connection, Neo4jQueryExecutor executor, String cypher,
                                               int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        return withGeneratedKeys(GraphJdbcStatementSupport.preparedStatement(connection, new Neo4jGraphJdbcExecutor(executor),
                PREPARED_SQL_PREPROCESSOR, cypher, resultSetType, resultSetConcurrency, resultSetHoldability),
                PreparedStatement.class);
    }

    static DatabaseMetaData databaseMetaData(final Neo4jConnection connection, final Neo4jConnectionConfig config) {
        return (DatabaseMetaData) Proxy.newProxyInstance(Neo4jJdbcProxies.class.getClassLoader(),
                new Class<?>[] {DatabaseMetaData.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String name = method.getName();
                        if ("getDatabaseProductName".equals(name)) {
                            return "Neo4j";
                        }
                        if ("getDatabaseProductVersion".equals(name)) {
                            return "";
                        }
                        if ("getDriverName".equals(name)) {
                            return "fw-plus-neo4j-jdbc";
                        }
                        if ("getDriverVersion".equals(name)) {
                            return "0.4.0-SNAPSHOT";
                        }
                        if ("getURL".equals(name)) {
                            return config.getJdbcUrl();
                        }
                        if ("getUserName".equals(name)) {
                            return config.getUsername();
                        }
                        if ("getConnection".equals(name)) {
                            return connection;
                        }
                        if ("supportsTransactions".equals(name) || "supportsBatchUpdates".equals(name)) {
                            return Boolean.TRUE;
                        }
                        if ("supportsGetGeneratedKeys".equals(name) || "supportsMultipleOpenResults".equals(name)
                                || "supportsMultipleResultSets".equals(name)) {
                            return Boolean.FALSE;
                        }
                        if ("getDefaultTransactionIsolation".equals(name)) {
                            return Integer.valueOf(java.sql.Connection.TRANSACTION_READ_COMMITTED);
                        }
                        if ("supportsTransactionIsolationLevel".equals(name)) {
                            return Boolean.valueOf(args != null && args.length > 0
                                    && ((Integer) args[0]).intValue() == java.sql.Connection.TRANSACTION_READ_COMMITTED);
                        }
                        if ("supportsResultSetType".equals(name)) {
                            return Boolean.valueOf(args != null && args.length > 0
                                    && (((Integer) args[0]).intValue() == ResultSet.TYPE_FORWARD_ONLY
                                    || ((Integer) args[0]).intValue() == ResultSet.TYPE_SCROLL_INSENSITIVE));
                        }
                        if ("supportsResultSetConcurrency".equals(name)) {
                            return Boolean.valueOf(args != null && args.length > 1
                                    && (((Integer) args[0]).intValue() == ResultSet.TYPE_FORWARD_ONLY
                                    || ((Integer) args[0]).intValue() == ResultSet.TYPE_SCROLL_INSENSITIVE)
                                    && ((Integer) args[1]).intValue() == ResultSet.CONCUR_READ_ONLY);
                        }
                        if ("getTables".equals(name)) {
                            return GraphJdbcMetaDataSupport.emptyTables();
                        }
                        if ("getColumns".equals(name)) {
                            return GraphJdbcMetaDataSupport.emptyColumns();
                        }
                        if ("getSchemas".equals(name)) {
                            return GraphJdbcMetaDataSupport.emptySchemas();
                        }
                        if ("getCatalogs".equals(name)) {
                            return GraphJdbcMetaDataSupport.emptyCatalogs();
                        }
                        if ("getTableTypes".equals(name)) {
                            return GraphJdbcMetaDataSupport.tableTypes("NODE", "RELATIONSHIP");
                        }
                        if ("getPrimaryKeys".equals(name)) {
                            return GraphJdbcMetaDataSupport.emptyPrimaryKeys();
                        }
                        if ("getIndexInfo".equals(name)) {
                            return GraphJdbcMetaDataSupport.emptyIndexInfo();
                        }
                        if ("unwrap".equals(name)) {
                            throw new SQLException("No wrapped object for " + method.getReturnType().getName());
                        }
                        if ("isWrapperFor".equals(name)) {
                            return Boolean.FALSE;
                        }
                        return GraphJdbcDefaults.defaultValue(method.getReturnType());
                    }
                });
    }

    @SuppressWarnings("unchecked")
    private static <T> T withGeneratedKeys(final T statement, Class<T> statementType) {
        return (T) Proxy.newProxyInstance(Neo4jJdbcProxies.class.getClassLoader(), new Class<?>[] {statementType},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("equals".equals(method.getName())) {
                            return Boolean.valueOf(proxy == args[0]);
                        }
                        if ("hashCode".equals(method.getName())) {
                            return Integer.valueOf(System.identityHashCode(proxy));
                        }
                        if ("toString".equals(method.getName())) {
                            return statement.toString();
                        }
						if ("unwrap".equals(method.getName())) {
							return GraphJdbcWrapperSupport.unwrap(proxy, (Class<?>) args[0]);
						}
						if ("isWrapperFor".equals(method.getName())) {
							return Boolean.valueOf(GraphJdbcWrapperSupport.isWrapperFor(proxy, (Class<?>) args[0]));
						}
						if ("getParameterMetaData".equals(method.getName())) {
							try {
								java.sql.ParameterMetaData parameterMetaData = (java.sql.ParameterMetaData) method.invoke(statement, args);
								return new GraphJdbcParameterMetaData(parameterMetaData.getParameterCount(), true);
							}
							catch (InvocationTargetException exception) {
								throw exception.getCause();
							}
						}
                        if ("getGeneratedKeys".equals(method.getName())) {
                            try {
                                method.invoke(statement, args);
                            }
                            catch (InvocationTargetException exception) {
                                throw exception.getCause();
                            }
                            return GraphJdbcRowSets.shapedResultSet(
                                    Collections.singletonList(GraphJdbcColumn.of("GENERATED_KEY", Types.JAVA_OBJECT)),
                                    Collections.<Map<String, Object>>emptyList());
                        }
                        try {
                            return method.invoke(statement, args);
                        }
                        catch (InvocationTargetException exception) {
                            throw exception.getCause();
                        }
                    }
                });
    }
}
