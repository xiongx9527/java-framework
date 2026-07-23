package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import java.sql.Array;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.NClob;
import java.sql.PreparedStatement;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

public class Neo4jConnection implements Connection {
    private final Neo4jConnectionConfig config;
    private final Neo4jQueryExecutor executor;
    private boolean closed;
    private boolean readOnly;
    private String catalog;
    private int networkTimeout;
    private int transactionIsolation = Connection.TRANSACTION_READ_COMMITTED;
    private final Properties clientInfo = new Properties();
    private Map<String, Class<?>> typeMap = new LinkedHashMap<String, Class<?>>();

    Neo4jConnection(Neo4jConnectionConfig config, Neo4jQueryExecutor executor) {
        this.config = config;
        this.executor = executor;
        this.catalog = config.getDatabase();
    }

    void checkOpen() throws SQLException {
        if (closed) {
            throw new SQLException("Connection is closed.");
        }
    }

    @Override
    public Statement createStatement() throws SQLException {
        checkOpen();
        return Neo4jJdbcProxies.statement(this, executor, java.sql.ResultSet.TYPE_FORWARD_ONLY,
                java.sql.ResultSet.CONCUR_READ_ONLY, java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Override
    public PreparedStatement prepareStatement(String sql) throws SQLException {
        checkOpen();
        return Neo4jJdbcProxies.preparedStatement(this, executor, sql);
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        throw unsupported("CallableStatement is not supported by Neo4j.");
    }

    @Override
    public String nativeSQL(String sql) {
        return sql;
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        checkOpen();
        executor.setAutoCommit(autoCommit);
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        checkOpen();
        return executor.getAutoCommit();
    }

    @Override
    public void commit() throws SQLException {
        checkOpen();
        executor.commit();
    }

    @Override
    public void rollback() throws SQLException {
        checkOpen();
        executor.rollback();
    }

    @Override
    public void close() throws SQLException {
        if (!closed) {
            closed = true;
            executor.close();
        }
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        checkOpen();
        return Neo4jJdbcProxies.databaseMetaData(this, config);
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Override
    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    @Override
    public String getCatalog() {
        return catalog;
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        if (level != Connection.TRANSACTION_NONE && level != Connection.TRANSACTION_READ_COMMITTED) {
            throw unsupported("Neo4j JDBC supports TRANSACTION_READ_COMMITTED semantics only.");
        }
        this.transactionIsolation = level;
    }

    @Override
    public int getTransactionIsolation() {
        return transactionIsolation;
    }

    @Override
    public SQLWarning getWarnings() {
        return null;
    }

    @Override
    public void clearWarnings() {
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        return createStatement(resultSetType, resultSetConcurrency, java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency)
            throws SQLException {
        return prepareStatement(sql, resultSetType, resultSetConcurrency, java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        throw unsupported("CallableStatement is not supported by Neo4j.");
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        checkOpen();
        return new LinkedHashMap<String, Class<?>>(typeMap);
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        checkOpen();
        typeMap = map == null ? new LinkedHashMap<String, Class<?>>()
                : new LinkedHashMap<String, Class<?>>(map);
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        if (holdability != java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT) {
            throw unsupported("Only CLOSE_CURSORS_AT_COMMIT ResultSet holdability is supported.");
        }
    }

    @Override
    public int getHoldability() {
        return java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw unsupported("Savepoint is not supported by Neo4j JDBC.");
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        throw unsupported("Savepoint is not supported by Neo4j JDBC.");
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        throw unsupported("Savepoint is not supported by Neo4j JDBC.");
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw unsupported("Savepoint is not supported by Neo4j JDBC.");
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        checkResultSetOptions(resultSetType, resultSetConcurrency, resultSetHoldability);
        return Neo4jJdbcProxies.statement(this, executor, resultSetType, resultSetConcurrency, resultSetHoldability);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int resultSetType, int resultSetConcurrency,
                                             int resultSetHoldability) throws SQLException {
        checkResultSetOptions(resultSetType, resultSetConcurrency, resultSetHoldability);
        checkOpen();
        return Neo4jJdbcProxies.preparedStatement(this, executor, sql, resultSetType, resultSetConcurrency,
                resultSetHoldability);
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException {
        throw unsupported("CallableStatement is not supported by Neo4j.");
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        return prepareStatement(sql);
    }

    @Override
    public Clob createClob() throws SQLException {
        checkOpen();
        return new Neo4jClob();
    }

    @Override
    public Blob createBlob() throws SQLException {
        checkOpen();
        return new Neo4jBlob();
    }

    @Override
    public NClob createNClob() throws SQLException {
        checkOpen();
        return new Neo4jNClob();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        checkOpen();
        return new Neo4jSQLXML();
    }

    @Override
    public boolean isValid(int timeout) {
        return !closed;
    }

    @Override
    public void setClientInfo(String name, String value) {
        clientInfo.setProperty(name, value);
    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {
        clientInfo.clear();
        if (properties != null) {
            clientInfo.putAll(properties);
        }
    }

    @Override
    public String getClientInfo(String name) {
        return clientInfo.getProperty(name);
    }

    @Override
    public Properties getClientInfo() {
        Properties copy = new Properties();
        copy.putAll(clientInfo);
        return copy;
    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        checkOpen();
        return new Neo4jArray(typeName, elements);
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        checkOpen();
        return new Neo4jStruct(typeName, attributes);
    }

    @Override
    public void setSchema(String schema) {
        this.catalog = schema;
    }

    @Override
    public String getSchema() {
        return catalog;
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        close();
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) {
        this.networkTimeout = milliseconds;
    }

    @Override
    public int getNetworkTimeout() {
        return networkTimeout;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }
        throw new SQLException("No wrapped object for " + iface.getName());
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) {
        return iface.isInstance(this);
    }

    private SQLFeatureNotSupportedException unsupported(String message) {
        return new SQLFeatureNotSupportedException(message);
    }

    private void checkResultSetOptions(int resultSetType, int resultSetConcurrency, int resultSetHoldability)
            throws SQLException {
        if (resultSetType != java.sql.ResultSet.TYPE_FORWARD_ONLY
                && resultSetType != java.sql.ResultSet.TYPE_SCROLL_INSENSITIVE) {
            throw unsupported("Only TYPE_FORWARD_ONLY and TYPE_SCROLL_INSENSITIVE ResultSet are supported.");
        }
        if (resultSetConcurrency != java.sql.ResultSet.CONCUR_READ_ONLY) {
            throw unsupported("Only CONCUR_READ_ONLY ResultSet is supported.");
        }
        if (resultSetHoldability != java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT) {
            throw unsupported("Only CLOSE_CURSORS_AT_COMMIT ResultSet holdability is supported.");
        }
    }
}
