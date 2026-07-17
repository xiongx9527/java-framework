/* Copyright (c) 2020 vesoft inc. All rights reserved.
 *
 * This source code is licensed under Apache 2.0 License.
 */

package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.exception.IOErrorException;
import com.vesoft.nebula.jdbc.client.NebulaClientFacade;
import com.vesoft.nebula.jdbc.config.NebulaConnectionConfig;
import com.vesoft.nebula.jdbc.config.NebulaConnectionConfigParser;
import com.vesoft.nebula.jdbc.lifecycle.NebulaResourceLifecycle;
import com.vesoft.nebula.jdbc.statement.NebulaPreparedStatementImpl;
import com.vesoft.nebula.jdbc.statement.NebulaStatementImpl;
import com.vesoft.nebula.jdbc.utils.ExceptionBuilder;
import com.vesoft.nebula.jdbc.utils.NebulaPropertyKey;
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
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Savepoint;
import java.sql.Statement;
import java.sql.Struct;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class NebulaConnection implements Connection {

    private Properties properties;
    private String graphSpace = null;
    private final NebulaResourceLifecycle lifecycle = new NebulaResourceLifecycle();
    private NebulaClientFacade clientFacade;

    protected NebulaConnection(String url, Properties properties) throws SQLException {
        NebulaConnectionConfig config = NebulaConnectionConfigParser.parse(url, properties);
        this.properties = config.getMergedProperties();
        this.graphSpace = config.getGraphspace();
        this.clientFacade = new NebulaClientFacade();
        this.clientFacade.open(config);
        log.debug(String.format("Access graph space [%s] succeeded", graphSpace));
    }

    public ResultSet execute(String nGql) throws SQLException {
        this.checkClosed();
        return this.clientFacade.executeNgql(nGql);
    }

    private void checkClosed() throws SQLException {
        lifecycle.checkOpen("Connection");
    }

    @Override
    public void close() throws SQLException {
        lifecycle.close(new NebulaResourceLifecycle.ThrowingCloseAction() {
            @Override
            public void close() throws SQLException {
                clientFacade.close();
                log.debug("JDBCConnection closed");
            }
        });
    }

    @Override
    public boolean isClosed() throws SQLException {
        return lifecycle.isClosed();
    }


    @Override
    public Statement createStatement() throws SQLException {
        this.checkClosed();
        return new NebulaStatementImpl(this);
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency) throws SQLException {
        log.debug("You are calling method \"Statement createStatement(int resultSetType, int " +
                "resultSetConcurrency)\", " +
                "the supported type is [TYPE_SCROLL_INSENSITIVE] and the supported concurrency is" +
                " [CONCUR_READ_ONLY]. " +
                "That is, the method you call is the same as \"Statement createStatement()\". ");
        return this.createStatement();
    }

    @Override
    public Statement createStatement(int resultSetType, int resultSetConcurrency,int resultSetHoldability) throws SQLException {
        log.debug("You are calling method \"Statement createStatement(int resultSetType, int " +
                "resultSetConcurrency, int resultSetHoldability)\", " +
                "the supported type is [TYPE_SCROLL_INSENSITIVE], " +
                "the supported concurrency is [CONCUR_READ_ONLY]" +
                "and the supported holdability is [CLOSE_CURSORS_AT_COMMIT]." +
                "That is, the method you call is the same as \"Statement createStatement()\". ");
        return this.createStatement();
    }

    @Override
    public PreparedStatement prepareStatement(String rawNGQL) throws SQLException {
        this.checkClosed();
        return new NebulaPreparedStatementImpl(this, rawNGQL);
    }

    @Override
    public PreparedStatement prepareStatement(String nGql, int resultSetType,int resultSetConcurrency) throws SQLException {
        log.debug("You are calling method \"PreparedStatement prepareStatement(String nGql, int " +
                "resultSetType, int resultSetConcurrency)\", " +
                "the supported type is [TYPE_SCROLL_INSENSITIVE] and the supported concurrency is" +
                " [CONCUR_READ_ONLY]. " +
                "That is, the method you call is the same as \"PreparedStatement prepareStatement" +
                "(String nGql)\". ");
        return this.prepareStatement(nGql);
    }

    @Override
    public PreparedStatement prepareStatement(String nGql, int resultSetType,int resultSetConcurrency, int resultSetHoldability) throws SQLException {
        log.debug("You are calling method \"PreparedStatement prepareStatement(String nGql, int " +
                "resultSetType, int resultSetConcurrency, int resultSetHoldability)\", " +
                "the supported type is [TYPE_SCROLL_INSENSITIVE] and the supported concurrency is" +
                " [CONCUR_READ_ONLY] " +
                "and the supported holdability is [CLOSE_CURSORS_AT_COMMIT]. " +
                "That is, the method you call is the same as \"PreparedStatement prepareStatement" +
                "(String nGql)\". ");
        return this.prepareStatement(nGql);
    }

    @Override
    public String getSchema() throws SQLException {
        this.checkClosed();
        return this.properties.getProperty(NebulaPropertyKey.DBNAME.getKeyName());
    }

    @Override
    public Properties getClientInfo() throws SQLException {
        this.checkClosed();
        return null;
    }

    @Override
    public String getClientInfo(String name) throws SQLException {
        this.checkClosed();
        return (String) (getClientInfo().getOrDefault(name, "null"));
    }

    @Override
    public void setSchema(String schema) throws SQLException {
        this.checkClosed();
        properties.setProperty(NebulaPropertyKey.DBNAME.getKeyName(), schema);
    }

    public Properties getConnectionConfig() {
        return this.properties;
    }


    @Override
    public PreparedStatement prepareStatement(String sql, int autoGeneratedKeys) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public PreparedStatement prepareStatement(String sql, int[] columnIndexes) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public PreparedStatement prepareStatement(String sql, String[] columnNames) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public CallableStatement prepareCall(String sql) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public CallableStatement prepareCall(String sql, int resultSetType, int resultSetConcurrency,
                                         int resultSetHoldability) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public String nativeSQL(String sql) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if (!autoCommit) {
            throw ExceptionBuilder.buildUnsupportedOperationException();
        }
        this.checkClosed();
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        this.checkClosed();
        return true;
    }

    @Override
    public void commit() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void rollback() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public DatabaseMetaData getMetaData() throws SQLException {
        this.checkClosed();
        return new NebulaDatabaseMetaData(this);
    }

    @Override
    public void setReadOnly(boolean readOnly) throws SQLException {
        this.checkClosed();
    }

    @Override
    public boolean isReadOnly() throws SQLException {
        this.checkClosed();
        return false;
    }

    @Override
    public void setCatalog(String catalog) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public String getCatalog() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setTransactionIsolation(int level) throws SQLException {
        this.checkClosed();
        if (level != Connection.TRANSACTION_NONE) {
            throw ExceptionBuilder.buildUnsupportedOperationException();
        }
    }

    @Override
    public int getTransactionIsolation() throws SQLException {
        this.checkClosed();
        return Connection.TRANSACTION_NONE;
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        this.checkClosed();
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        this.checkClosed();
    }

    @Override
    public Map<String, Class<?>> getTypeMap() throws SQLException {
        this.checkClosed();
        return null;
    }

    @Override
    public void setTypeMap(Map<String, Class<?>> map) throws SQLException {
        this.checkClosed();
    }

    @Override
    public void setHoldability(int holdability) throws SQLException {
        this.checkClosed();
        if (holdability != java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT) {
            throw ExceptionBuilder.buildUnsupportedOperationException();
        }
    }

    @Override
    public int getHoldability() throws SQLException {
        this.checkClosed();
        return java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT;
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public Savepoint setSavepoint(String name) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void rollback(Savepoint savepoint) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void releaseSavepoint(Savepoint savepoint) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public Clob createClob() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public Blob createBlob() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public NClob createNClob() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public SQLXML createSQLXML() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public boolean isValid(int timeout) throws SQLException {
        if (timeout < 0) {
            throw new SQLException("Timeout value mustn't be less 0");
        }

        if (isClosed()) {
            return false;
        }

        Statement statement = null;
        try {
            statement = createStatement();
            if (statement.execute("YIELD 1")) {
                return true;
            } else {
                return false;
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }

    @Override
    public void setClientInfo(String name, String value) throws SQLClientInfoException {

    }

    @Override
    public void setClientInfo(Properties properties) throws SQLClientInfoException {

    }

    @Override
    public Array createArrayOf(String typeName, Object[] elements) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public Struct createStruct(String typeName, Object[] attributes) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void abort(Executor executor) throws SQLException {
        this.close();
    }

    @Override
    public void setNetworkTimeout(Executor executor, int milliseconds) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int getNetworkTimeout() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }
}
