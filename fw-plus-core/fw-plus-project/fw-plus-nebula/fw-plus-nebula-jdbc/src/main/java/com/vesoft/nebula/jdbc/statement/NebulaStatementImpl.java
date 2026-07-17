/* Copyright (c) 2020 vesoft inc. All rights reserved.
 *
 * This source code is licensed under Apache 2.0 License.
 */

package com.vesoft.nebula.jdbc.statement;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.NebulaResultSet;
import com.vesoft.nebula.jdbc.lifecycle.NebulaResourceLifecycle;
import com.vesoft.nebula.jdbc.sql.NebulaParameterBindings;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutor;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutorProvider;
import com.vesoft.nebula.jdbc.utils.ExceptionBuilder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NebulaStatementImpl implements NebulaStatement {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    protected String nGql;
    protected NebulaConnection nebulaConnection;
    protected NebulaSqlExecutor sqlExecutor;
    protected boolean isExecuteSucceed;
    protected ResultSet currentResultSet;
    protected final NebulaResourceLifecycle lifecycle = new NebulaResourceLifecycle();

    public NebulaStatementImpl(NebulaConnection connection) {
        this(connection, NebulaSqlExecutorProvider.getExecutor());
    }

    public NebulaStatementImpl(NebulaConnection connection, NebulaSqlExecutor sqlExecutor) {
        this.nebulaConnection = connection;
        this.sqlExecutor = sqlExecutor == null ? NebulaSqlExecutorProvider.getExecutor() : sqlExecutor;
    }

    /** This method just return a boolean value to indicate whether succeed or not,
     *  if you call it directly you can call getResultSet() to get currentResultSet to retrieve result from serve.
     */
    @Override
    public boolean execute(String nGql) throws SQLException {
        this.checkClosed();
        this.nGql = nGql;
        if (isQuery(nGql)) {
            this.currentResultSet = this.sqlExecutor.executeQuery(this.nebulaConnection, nGql, NebulaParameterBindings.empty());
            isExecuteSucceed = true;
            return true;
        }
        this.sqlExecutor.executeUpdate(this.nebulaConnection, nGql, NebulaParameterBindings.empty());
        this.currentResultSet = null;
        isExecuteSucceed = true;
        return false;
    }

    @Override
    public ResultSet executeQuery(String nGql) throws SQLException {
        this.checkClosed();
        this.currentResultSet = this.sqlExecutor.executeQuery(this.nebulaConnection, nGql, NebulaParameterBindings.empty());
        return currentResultSet;
    }


    /** This method will not return the number of data which influenced by the executed ngql.
     *  If the user inserts multiple points or edges in a single insert statement, some of them may succeed,
     *  but the server will only return to tell the user that it has failed, however, the user may actually be able to find out some of the data have inserted.
    */
    @Override
    public int executeUpdate(String nGql) throws SQLException {
        this.checkClosed();
        return this.sqlExecutor.executeUpdate(this.nebulaConnection, nGql, NebulaParameterBindings.empty());
    }

    @Override
    public NebulaConnection getConnection() throws SQLException {
        this.checkClosed();
        return this.nebulaConnection;
    }



    @Override
    public void checkReadOnly(String nGql) throws SQLException {
        NebulaStatementClassifier.checkQuery(nGql);
    }

    @Override
    public void checkUpdate(String nGql) throws SQLException {
        NebulaStatementClassifier.checkUpdate(nGql);
    }

    protected boolean isQuery(String nGql) throws SQLException {
        try {
            NebulaStatementClassifier.checkQuery(nGql);
            return true;
        } catch (SQLException queryException) {
            try {
                NebulaStatementClassifier.checkUpdate(nGql);
                return false;
            } catch (SQLException updateException) {
                throw queryException;
            }
        }
    }



    @Override
    public void close() throws SQLException {
        lifecycle.close(new NebulaResourceLifecycle.ThrowingCloseAction() {
            @Override
            public void close() throws SQLException {
                currentResultSet = null;
            }
        });
    }

    @Override
    public boolean isClosed() throws SQLException {
        return lifecycle.isClosed();
    }


    @Override
    public void checkClosed() throws SQLException {
        lifecycle.checkOpen("Statement");
    }



    @Override
    public ResultSet getResultSet() throws SQLException {
        this.checkClosed();
        return this.currentResultSet;
    }

    @Override
    public int getResultSetHoldability() throws SQLException {
        this.checkClosed();
        return NebulaResultSet.SUPPORTED_HOLDABILITY;
    }

    @Override
    public int getResultSetConcurrency() throws SQLException {
        this.checkClosed();
        return NebulaResultSet.SUPPORTED_CONCURRENCY;
    }

    @Override
    public int getResultSetType() throws SQLException {
        this.checkClosed();
        return NebulaResultSet.SUPPORTED_TYPE;
    }

    @Override
    public int getMaxFieldSize() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int getMaxRows() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int getQueryTimeout() throws SQLException {
//        return ((NebulaResultSet)currentResultSet).getNativeNebulaResultSet().getLatency();
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void cancel() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public SQLWarning getWarnings() throws SQLException {
        this.checkClosed();
        return null;
    }

    @Override
    public void clearWarnings() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setCursorName(String name) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }


    @Override
    public int getUpdateCount() throws SQLException {
        this.checkClosed();
        return -1;
    }

    @Override
    public boolean getMoreResults() throws SQLException {
        this.checkClosed();
        return false;
    }

    @Override
    public void setFetchDirection(int direction) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int getFetchDirection() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int getFetchSize() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void clearBatch() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public boolean isPoolable() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
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
