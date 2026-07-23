package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Types;
import java.util.Collections;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class Neo4jJdbcProxies {
    private Neo4jJdbcProxies() {
    }

    static Statement statement(Neo4jConnection connection, Neo4jQueryExecutor executor, int resultSetType,
                               int resultSetConcurrency, int resultSetHoldability) {
        return (Statement) Proxy.newProxyInstance(Neo4jJdbcProxies.class.getClassLoader(),
                new Class<?>[] {Statement.class}, new StatementHandler(connection, executor, null, resultSetType,
                        resultSetConcurrency, resultSetHoldability));
    }

    static PreparedStatement preparedStatement(Neo4jConnection connection, Neo4jQueryExecutor executor, String cypher) {
        return preparedStatement(connection, executor, cypher, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY,
                ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    static PreparedStatement preparedStatement(Neo4jConnection connection, Neo4jQueryExecutor executor, String cypher,
                                               int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
        return (PreparedStatement) Proxy.newProxyInstance(Neo4jJdbcProxies.class.getClassLoader(),
                new Class<?>[] {PreparedStatement.class}, new StatementHandler(connection, executor, cypher,
                        resultSetType, resultSetConcurrency, resultSetHoldability));
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
                            return "0.1.1-SNAPSHOT";
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
                        if ("supportsTransactions".equals(name)) {
                            return Boolean.TRUE;
                        }
                        if ("supportsBatchUpdates".equals(name)) {
                            return Boolean.TRUE;
                        }
                        if ("supportsGetGeneratedKeys".equals(name)
                                || "supportsMultipleOpenResults".equals(name)
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
                            return emptyTablesResultSet();
                        }
                        if ("getColumns".equals(name)) {
                            return emptyColumnsResultSet();
                        }
                        if ("getSchemas".equals(name)) {
                            return emptySchemasResultSet();
                        }
                        if ("getCatalogs".equals(name)) {
                            return emptyCatalogsResultSet();
                        }
                        if ("getTableTypes".equals(name)) {
                            return tableTypesResultSet();
                        }
                        if ("getPrimaryKeys".equals(name)) {
                            return emptyPrimaryKeysResultSet();
                        }
                        if ("getIndexInfo".equals(name)) {
                            return emptyIndexInfoResultSet();
                        }
                        if ("unwrap".equals(name)) {
                            throw new SQLException("No wrapped object for " + method.getReturnType().getName());
                        }
                        if ("isWrapperFor".equals(name)) {
                            return Boolean.FALSE;
                        }
                        return defaultValue(method.getReturnType());
                    }
                });
    }

    static ResultSet resultSet(Neo4jQueryResult result) throws SQLException {
        CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
        List<String> columns = result.getColumns();
        List<Map<String, Object>> rows = result.getRows();
        if (columns.isEmpty() && !rows.isEmpty()) {
            columns.addAll(rows.get(0).keySet());
        }
        RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
        metaData.setColumnCount(columns.size());
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            metaData.setColumnName(i + 1, column);
            metaData.setColumnLabel(i + 1, column);
            metaData.setColumnType(i + 1, jdbcType(firstValue(rows, column)));
        }
        rowSet.setMetaData(metaData);
        for (int rowIndex = rows.size() - 1; rowIndex >= 0; rowIndex--) {
            Map<String, Object> row = rows.get(rowIndex);
            rowSet.moveToInsertRow();
            for (int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
                rowSet.updateObject(columnIndex + 1, row.get(columns.get(columnIndex)));
            }
            rowSet.insertRow();
            rowSet.moveToCurrentRow();
        }
        rowSet.beforeFirst();
        return rowSet;
    }

    private static ResultSet emptyResultSet() throws SQLException {
        return resultSet(new Neo4jQueryResult(Collections.<String>emptyList(),
                Collections.<Map<String, Object>>emptyList(), -1));
    }

    private static ResultSet emptyTablesResultSet() throws SQLException {
        return shapedResultSet(new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "TABLE_TYPE", "REMARKS",
                        "TYPE_CAT", "TYPE_SCHEM", "TYPE_NAME", "SELF_REFERENCING_COL_NAME", "REF_GENERATION"},
                new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR},
                Collections.<Map<String, Object>>emptyList());
    }

    private static ResultSet emptyColumnsResultSet() throws SQLException {
        return shapedResultSet(new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "DATA_TYPE",
                        "TYPE_NAME", "COLUMN_SIZE", "BUFFER_LENGTH", "DECIMAL_DIGITS", "NUM_PREC_RADIX",
                        "NULLABLE", "REMARKS", "COLUMN_DEF", "SQL_DATA_TYPE", "SQL_DATETIME_SUB",
                        "CHAR_OCTET_LENGTH", "ORDINAL_POSITION", "IS_NULLABLE", "SCOPE_CATALOG", "SCOPE_SCHEMA",
                        "SCOPE_TABLE", "SOURCE_DATA_TYPE", "IS_AUTOINCREMENT", "IS_GENERATEDCOLUMN"},
                new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.INTEGER,
                        Types.VARCHAR, Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.INTEGER,
                        Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.INTEGER, Types.INTEGER,
                        Types.INTEGER, Types.INTEGER, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR,
                        Types.VARCHAR, Types.SMALLINT, Types.VARCHAR, Types.VARCHAR},
                Collections.<Map<String, Object>>emptyList());
    }

    private static ResultSet emptySchemasResultSet() throws SQLException {
        return shapedResultSet(new String[] {"TABLE_SCHEM", "TABLE_CATALOG"},
                new int[] {Types.VARCHAR, Types.VARCHAR}, Collections.<Map<String, Object>>emptyList());
    }

    private static ResultSet emptyCatalogsResultSet() throws SQLException {
        return shapedResultSet(new String[] {"TABLE_CAT"}, new int[] {Types.VARCHAR},
                Collections.<Map<String, Object>>emptyList());
    }

    private static ResultSet tableTypesResultSet() throws SQLException {
        Map<String, Object> node = new LinkedHashMap<String, Object>();
        node.put("TABLE_TYPE", "NODE");
        Map<String, Object> edge = new LinkedHashMap<String, Object>();
        edge.put("TABLE_TYPE", "RELATIONSHIP");
        List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
        rows.add(node);
        rows.add(edge);
        return shapedResultSet(new String[] {"TABLE_TYPE"}, new int[] {Types.VARCHAR}, rows);
    }

    private static ResultSet emptyPrimaryKeysResultSet() throws SQLException {
        return shapedResultSet(new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "COLUMN_NAME", "KEY_SEQ",
                        "PK_NAME"},
                new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.SMALLINT,
                        Types.VARCHAR}, Collections.<Map<String, Object>>emptyList());
    }

    private static ResultSet emptyIndexInfoResultSet() throws SQLException {
        return shapedResultSet(new String[] {"TABLE_CAT", "TABLE_SCHEM", "TABLE_NAME", "NON_UNIQUE",
                        "INDEX_QUALIFIER", "INDEX_NAME", "TYPE", "ORDINAL_POSITION", "COLUMN_NAME",
                        "ASC_OR_DESC", "CARDINALITY", "PAGES", "FILTER_CONDITION"},
                new int[] {Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.BOOLEAN, Types.VARCHAR,
                        Types.VARCHAR, Types.SMALLINT, Types.SMALLINT, Types.VARCHAR, Types.VARCHAR,
                        Types.BIGINT, Types.BIGINT, Types.VARCHAR}, Collections.<Map<String, Object>>emptyList());
    }

    private static ResultSet shapedResultSet(String[] columns, int[] types, List<Map<String, Object>> rows)
            throws SQLException {
        CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
        RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
        metaData.setColumnCount(columns.length);
        for (int i = 0; i < columns.length; i++) {
            metaData.setColumnName(i + 1, columns[i]);
            metaData.setColumnLabel(i + 1, columns[i]);
            metaData.setColumnType(i + 1, types[i]);
        }
        rowSet.setMetaData(metaData);
        for (int rowIndex = rows.size() - 1; rowIndex >= 0; rowIndex--) {
            Map<String, Object> row = rows.get(rowIndex);
            rowSet.moveToInsertRow();
            for (int columnIndex = 0; columnIndex < columns.length; columnIndex++) {
                rowSet.updateObject(columnIndex + 1, row.get(columns[columnIndex]));
            }
            rowSet.insertRow();
            rowSet.moveToCurrentRow();
        }
        rowSet.beforeFirst();
        return rowSet;
    }

    private static Object firstValue(List<Map<String, Object>> rows, String column) {
        for (Map<String, Object> row : rows) {
            Object value = row.get(column);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static int jdbcType(Object value) {
        if (value instanceof Integer) {
            return Types.INTEGER;
        }
        if (value instanceof Long) {
            return Types.BIGINT;
        }
        if (value instanceof Float || value instanceof Double) {
            return Types.DOUBLE;
        }
        if (value instanceof Boolean) {
            return Types.BOOLEAN;
        }
        return Types.VARCHAR;
    }

    private static Object defaultValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return Boolean.FALSE;
        }
        if (returnType == Integer.TYPE) {
            return Integer.valueOf(0);
        }
        if (returnType == Long.TYPE) {
            return Long.valueOf(0L);
        }
        if (returnType == Float.TYPE) {
            return Float.valueOf(0F);
        }
        if (returnType == Double.TYPE) {
            return Double.valueOf(0D);
        }
        if (returnType == Short.TYPE) {
            return Short.valueOf((short) 0);
        }
        if (returnType == Byte.TYPE) {
            return Byte.valueOf((byte) 0);
        }
        return null;
    }

    private static class StatementHandler implements InvocationHandler {
        private final Neo4jConnection connection;
        private final Neo4jQueryExecutor executor;
        private final String preparedCypher;
        private final Map<Integer, Object> parameters = new LinkedHashMap<Integer, Object>();
        private ResultSet currentResultSet;
        private int updateCount = -1;
        private boolean closed;
        private int maxRows;
        private int maxFieldSize;
        private int fetchSize;
        private int fetchDirection = ResultSet.FETCH_FORWARD;
        private int queryTimeout;
        private boolean closeOnCompletion;
        private boolean poolable;
        private final int resultSetType;
        private final int resultSetConcurrency;
        private final int resultSetHoldability;
        private final List<String> batch = new ArrayList<String>();

        private StatementHandler(Neo4jConnection connection, Neo4jQueryExecutor executor, String preparedCypher,
                                 int resultSetType, int resultSetConcurrency, int resultSetHoldability) {
            this.connection = connection;
            this.executor = executor;
            this.preparedCypher = preparedCypher;
            this.resultSetType = resultSetType;
            this.resultSetConcurrency = resultSetConcurrency;
            this.resultSetHoldability = resultSetHoldability;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (preparedCypher != null && args != null && args.length > 0 && args[0] instanceof String
                    && ("executeQuery".equals(name) || "executeUpdate".equals(name) || "execute".equals(name))) {
                throw new SQLException(name + "(String) cannot be called on PreparedStatement.");
            }
            if ("executeQuery".equals(name)) {
                return executeQuery(args == null || args.length == 0 ? preparedCypher : (String) args[0]);
            }
            if ("executeUpdate".equals(name)) {
                boolean hasResultSet = execute(args == null || args.length == 0 ? preparedCypher : (String) args[0]);
                if (hasResultSet) {
                    throw new SQLException("SQL produced a ResultSet and cannot be used with executeUpdate.");
                }
                return Integer.valueOf(updateCount < 0 ? 0 : updateCount);
            }
            if ("execute".equals(name)) {
                return Boolean.valueOf(execute(args == null || args.length == 0 ? preparedCypher : (String) args[0]));
            }
            if ("close".equals(name)) {
                closed = true;
                if (currentResultSet != null) {
                    currentResultSet.close();
                }
                return null;
            }
            if ("isClosed".equals(name)) {
                return Boolean.valueOf(closed);
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
                parameters.clear();
                return null;
            }
            if ("setNull".equals(name) && args != null && args.length >= 1 && args[0] instanceof Integer) {
                parameters.put((Integer) args[0], null);
                return null;
            }
            if (name.startsWith("set") && args != null && args.length >= 2 && args[0] instanceof Integer) {
                parameters.put((Integer) args[0], normalizeParameter(name, args));
                return null;
            }
            if ("addBatch".equals(name)) {
                batch.add(args == null || args.length == 0 ? preparedCypher : (String) args[0]);
                return null;
            }
            if ("clearBatch".equals(name)) {
                batch.clear();
                return null;
            }
            if ("executeBatch".equals(name)) {
                int[] counts = new int[batch.size()];
                for (int i = 0; i < batch.size(); i++) {
                    execute(batch.get(i));
                    counts[i] = updateCount < 0 ? Statement.SUCCESS_NO_INFO : updateCount;
                }
                batch.clear();
                return counts;
            }
            if ("getGeneratedKeys".equals(name)) {
                return emptyGeneratedKeysResultSet();
            }
            if ("getParameterMetaData".equals(name) && preparedCypher != null) {
                return parameterMetaData(countParameters(preparedCypher));
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
            if ("setObject".equals(name) && args != null && args.length >= 2 && args[0] instanceof Integer) {
                parameters.put((Integer) args[0], normalizeParameter(name, args));
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
                Class<?> iface = (Class<?>) args[0];
                if (iface.isInstance(proxy)) {
                    return iface.cast(proxy);
                }
                throw new SQLException("No wrapped object for " + args[0]);
            }
            if ("isWrapperFor".equals(name)) {
                return Boolean.valueOf(((Class<?>) args[0]).isInstance(proxy));
            }
            return defaultValue(method.getReturnType());
        }

        private ResultSet executeQuery(String cypher) throws SQLException {
            boolean hasResultSet = execute(cypher);
            if (!hasResultSet) {
                throw new SQLException("SQL did not produce a ResultSet and cannot be used with executeQuery.");
            }
            if (currentResultSet == null) {
                currentResultSet = emptyResultSet();
            }
            return currentResultSet;
        }

        private boolean execute(String cypher) throws SQLException {
            checkOpen();
            PreparedCypher prepared = preparedCypher == null
                    ? new PreparedCypher(Neo4jSqlToCypherTranslator.translate(cypher), Collections.<String, Object>emptyMap())
                    : bindPreparedCypher(cypher, parameters);
            Neo4jQueryResult result = executor.execute(prepared.cypher, prepared.parameters);
            boolean hasResultSet = hasResultSet(result);
            if (hasResultSet) {
                currentResultSet = resultSet(result);
                updateCount = -1;
            }
            else {
                currentResultSet = null;
                updateCount = result.getUpdateCount();
            }
            return hasResultSet;
        }

        private void checkOpen() throws SQLException {
            connection.checkOpen();
            if (closed) {
                throw new SQLException("Statement is closed.");
            }
        }
    }

    private static boolean hasResultSet(Neo4jQueryResult result) {
        return !result.getColumns().isEmpty() || !result.getRows().isEmpty() || result.getUpdateCount() < 0;
    }

    private static ResultSet emptyGeneratedKeysResultSet() throws SQLException {
        return shapedResultSet(new String[] {"GENERATED_KEY"}, new int[] {Types.JAVA_OBJECT},
                Collections.<Map<String, Object>>emptyList());
    }

    private static ParameterMetaData parameterMetaData(final int count) {
        return (ParameterMetaData) Proxy.newProxyInstance(Neo4jJdbcProxies.class.getClassLoader(),
                new Class<?>[] {ParameterMetaData.class}, new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        String name = method.getName();
                        if ("getParameterCount".equals(name)) {
                            return Integer.valueOf(count);
                        }
                        if ("isNullable".equals(name)) {
                            checkParameterIndex(args, count);
                            return Integer.valueOf(ParameterMetaData.parameterNullableUnknown);
                        }
                        if ("isSigned".equals(name)) {
                            checkParameterIndex(args, count);
                            return Boolean.TRUE;
                        }
                        if ("getPrecision".equals(name) || "getScale".equals(name)) {
                            checkParameterIndex(args, count);
                            return Integer.valueOf(0);
                        }
                        if ("getParameterType".equals(name)) {
                            checkParameterIndex(args, count);
                            return Integer.valueOf(Types.JAVA_OBJECT);
                        }
                        if ("getParameterTypeName".equals(name)) {
                            checkParameterIndex(args, count);
                            return "JAVA_OBJECT";
                        }
                        if ("getParameterClassName".equals(name)) {
                            checkParameterIndex(args, count);
                            return Object.class.getName();
                        }
                        if ("getParameterMode".equals(name)) {
                            checkParameterIndex(args, count);
                            return Integer.valueOf(ParameterMetaData.parameterModeIn);
                        }
                        if ("unwrap".equals(name)) {
                            Class<?> iface = (Class<?>) args[0];
                            if (iface.isInstance(proxy)) {
                                return iface.cast(proxy);
                            }
                            throw new SQLException("No wrapped object for " + args[0]);
                        }
                        if ("isWrapperFor".equals(name)) {
                            return Boolean.valueOf(((Class<?>) args[0]).isInstance(proxy));
                        }
                        return defaultValue(method.getReturnType());
                    }
                });
    }

    private static void checkParameterIndex(Object[] args, int count) throws SQLException {
        int index = ((Integer) args[0]).intValue();
        if (index < 1 || index > count) {
            throw new SQLException("Parameter index out of range: " + index);
        }
    }

    private static int countParameters(String sql) {
        boolean inString = false;
        int count = 0;
        for (int i = 0; i < sql.length(); i++) {
            char current = sql.charAt(i);
            if (current == '\'') {
                if (inString && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    i++;
                }
                else {
                    inString = !inString;
                }
            }
            else if (!inString && current == '?') {
                count++;
            }
        }
        return count;
    }

    private static Object normalizeParameter(String methodName, Object[] args) throws SQLException {
        Object value = args[1];
        if (value == null) {
            return null;
        }
        if ("setRowId".equals(methodName) && value instanceof RowId) {
            return value.toString();
        }
        if ("setRef".equals(methodName) && value instanceof Ref) {
            return ((Ref) value).getObject();
        }
        if ("setArray".equals(methodName) && value instanceof Array) {
            return ((Array) value).getArray();
        }
        if ("setBlob".equals(methodName)) {
            if (value instanceof Blob) {
                Blob blob = (Blob) value;
                long length = blob.length();
                if (length > Integer.MAX_VALUE) {
                    throw new SQLException("Blob parameter exceeds supported in-memory size.");
                }
                return blob.getBytes(1L, (int) length);
            }
            if (value instanceof InputStream) {
                return readBytes((InputStream) value, lengthArgument(args));
            }
        }
        if ("setClob".equals(methodName) || "setNClob".equals(methodName)) {
            if (value instanceof Clob) {
                Clob clob = (Clob) value;
                long length = clob.length();
                if (length > Integer.MAX_VALUE) {
                    throw new SQLException("Clob parameter exceeds supported in-memory size.");
                }
                return clob.getSubString(1L, (int) length);
            }
            if (value instanceof Reader) {
                return readString((Reader) value, lengthArgument(args));
            }
        }
        if ("setSQLXML".equals(methodName) && value instanceof SQLXML) {
            return ((SQLXML) value).getString();
        }
        if ("setAsciiStream".equals(methodName) && value instanceof InputStream) {
            return new String(readBytes((InputStream) value, lengthArgument(args)), StandardCharsets.US_ASCII);
        }
        if ("setUnicodeStream".equals(methodName) && value instanceof InputStream) {
            return new String(readBytes((InputStream) value, lengthArgument(args)), StandardCharsets.UTF_8);
        }
        if ("setBinaryStream".equals(methodName) && value instanceof InputStream) {
            return readBytes((InputStream) value, lengthArgument(args));
        }
        if (("setCharacterStream".equals(methodName) || "setNCharacterStream".equals(methodName))
                && value instanceof Reader) {
            return readString((Reader) value, lengthArgument(args));
        }
        if ("setObject".equals(methodName)) {
            if (value instanceof RowId) {
                return value.toString();
            }
            if (value instanceof Ref) {
                return ((Ref) value).getObject();
            }
            if (value instanceof Array) {
                return ((Array) value).getArray();
            }
            if (value instanceof SQLXML) {
                return ((SQLXML) value).getString();
            }
            if (value instanceof NClob || value instanceof Clob) {
                Clob clob = (Clob) value;
                long length = clob.length();
                if (length > Integer.MAX_VALUE) {
                    throw new SQLException("Clob parameter exceeds supported in-memory size.");
                }
                return clob.getSubString(1L, (int) length);
            }
            if (value instanceof Blob) {
                Blob blob = (Blob) value;
                long length = blob.length();
                if (length > Integer.MAX_VALUE) {
                    throw new SQLException("Blob parameter exceeds supported in-memory size.");
                }
                return blob.getBytes(1L, (int) length);
            }
        }
        return value;
    }

    private static long lengthArgument(Object[] args) {
        if (args == null || args.length < 3 || !(args[2] instanceof Number)) {
            return -1L;
        }
        return ((Number) args[2]).longValue();
    }

    private static byte[] readBytes(InputStream inputStream, long requestedLength) throws SQLException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            long remaining = requestedLength;
            while (remaining != 0L) {
                int max = buffer.length;
                if (remaining > 0L && remaining < max) {
                    max = (int) remaining;
                }
                int read = inputStream.read(buffer, 0, max);
                if (read < 0) {
                    break;
                }
                output.write(buffer, 0, read);
                if (remaining > 0L) {
                    remaining -= read;
                }
            }
            return output.toByteArray();
        }
        catch (IOException exception) {
            throw new SQLException("Failed to read stream parameter.", exception);
        }
    }

    private static String readString(Reader reader, long requestedLength) throws SQLException {
        try {
            StringWriter writer = new StringWriter();
            char[] buffer = new char[4096];
            long remaining = requestedLength;
            while (remaining != 0L) {
                int max = buffer.length;
                if (remaining > 0L && remaining < max) {
                    max = (int) remaining;
                }
                int read = reader.read(buffer, 0, max);
                if (read < 0) {
                    break;
                }
                writer.write(buffer, 0, read);
                if (remaining > 0L) {
                    remaining -= read;
                }
            }
            return writer.toString();
        }
        catch (IOException exception) {
            throw new SQLException("Failed to read reader parameter.", exception);
        }
    }

    private static PreparedCypher bindPreparedCypher(String cypher, Map<Integer, Object> values)
            throws SQLException {
        StringBuilder builder = new StringBuilder();
        Map<String, Object> named = new LinkedHashMap<String, Object>();
        boolean inString = false;
        int index = 1;
        for (int i = 0; i < cypher.length(); i++) {
            char current = cypher.charAt(i);
            if (current == '\'') {
                builder.append(current);
                if (inString && i + 1 < cypher.length() && cypher.charAt(i + 1) == '\'') {
                    builder.append(cypher.charAt(++i));
                }
                else {
                    inString = !inString;
                }
                continue;
            }
            if (!inString && current == '?') {
                Object value = values.get(Integer.valueOf(index));
                String name = "p" + index++;
                named.put(name, value);
                builder.append('$').append(name);
            }
            else {
                builder.append(current);
            }
        }
        if (index == 1 && cypher.indexOf('?') >= 0) {
            throw new SQLException("Prepared Cypher contains no bindable parameter.");
        }
        return new PreparedCypher(Neo4jSqlToCypherTranslator.translate(builder.toString()), named);
    }

    private static class PreparedCypher {
        private final String cypher;
        private final Map<String, Object> parameters;

        private PreparedCypher(String cypher, Map<String, Object> parameters) {
            this.cypher = cypher;
            this.parameters = parameters;
        }
    }
}
