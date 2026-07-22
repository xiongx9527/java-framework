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
import com.vesoft.nebula.jdbc.sql.routine.RoutineCallResult;
import com.vesoft.nebula.jdbc.sql.routine.RoutineService;
import com.vesoft.nebula.jdbc.sql.routine.RoutineStatement;
import com.vesoft.nebula.jdbc.sql.routine.RoutineStatementParser;
import com.vesoft.nebula.jdbc.utils.ExceptionBuilder;
import com.vesoft.nebula.jdbc.utils.JdbcWrapperSupport;
import java.sql.BatchUpdateException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
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
    protected int maxFieldSize;
    protected int maxRows;
    protected boolean escapeProcessing = true;
    protected int queryTimeout;
    protected int fetchDirection = ResultSet.FETCH_FORWARD;
    protected int fetchSize;
    protected String cursorName;
    protected boolean poolable;
    protected boolean closeOnCompletion;
    protected final List<String> batchSql = new ArrayList<String>();

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
        if (RoutineStatementParser.isRoutineStatement(nGql)) {
            return executeRoutine(nGql).hasResultSet();
        }
        if (isQuery(nGql)) {
            this.currentResultSet = applyResultSetLimits(this.sqlExecutor.executeQuery(this.nebulaConnection, nGql, NebulaParameterBindings.empty()));
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
        if (RoutineStatementParser.isRoutineStatement(nGql)) {
            RoutineCallResult result = executeRoutine(nGql);
            if (!result.hasResultSet()) {
                throw new SQLException("Routine call did not return a result set");
            }
            return currentResultSet;
        }
        this.currentResultSet = applyResultSetLimits(this.sqlExecutor.executeQuery(this.nebulaConnection, nGql, NebulaParameterBindings.empty()));
        return currentResultSet;
    }


    /** This method will not return the number of data which influenced by the executed ngql.
     *  If the user inserts multiple points or edges in a single insert statement, some of them may succeed,
     *  but the server will only return to tell the user that it has failed, however, the user may actually be able to find out some of the data have inserted.
    */
    @Override
    public int executeUpdate(String nGql) throws SQLException {
        this.checkClosed();
        if (RoutineStatementParser.isRoutineStatement(nGql)) {
            RoutineCallResult result = executeRoutine(nGql);
            if (result.hasResultSet()) {
                throw new SQLException("Routine call returned a result set");
            }
            return result.getUpdateCount();
        }
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

    private RoutineCallResult executeRoutine(String sql) throws SQLException {
        RoutineStatement statement = RoutineStatementParser.parse(sql);
        RoutineService service = routineService();
        if (statement.getKind() == RoutineStatement.Kind.CREATE_PROCEDURE) {
            service.create(currentSchema(), currentUser(), sql);
            this.currentResultSet = null;
            isExecuteSucceed = true;
            return RoutineCallResult.updateCount(0);
        }
        if (statement.getKind() == RoutineStatement.Kind.DROP_PROCEDURE) {
            service.drop(currentSchema(), currentUser(), sql);
            this.currentResultSet = null;
            isExecuteSucceed = true;
            return RoutineCallResult.updateCount(0);
        }
        RoutineCallResult result = service.call(currentSchema(), currentUser(), sql,
                this.nebulaConnection, this.sqlExecutor);
        this.currentResultSet = applyResultSetLimits(result.getResultSet());
        isExecuteSucceed = true;
        return result;
    }

    protected RoutineService routineService() {
        return RoutineService.getDefault();
    }

    protected ResultSet applyResultSetLimits(ResultSet resultSet) throws SQLException {
        if (resultSet instanceof NebulaResultSet) {
            ((NebulaResultSet) resultSet).limitRows(maxRows);
        }
        return resultSet;
    }

    private String currentSchema() throws SQLException {
        return this.nebulaConnection == null ? "" : this.nebulaConnection.getSchema();
    }

    private String currentUser() {
        if (this.nebulaConnection == null || this.nebulaConnection.getConnectionConfig() == null) {
            return "";
        }
        Properties properties = this.nebulaConnection.getConnectionConfig();
        return properties.getProperty("user", "");
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
        this.checkClosed();
        return maxFieldSize;
    }

    @Override
    public void setMaxFieldSize(int max) throws SQLException {
        this.checkClosed();
        if (max < 0) {
            throw new SQLException("Max field size must be >= 0.");
        }
        this.maxFieldSize = max;
    }

    @Override
    public int getMaxRows() throws SQLException {
        this.checkClosed();
        return maxRows;
    }

    @Override
    public void setMaxRows(int max) throws SQLException {
        this.checkClosed();
        if (max < 0) {
            throw new SQLException("Max rows must be >= 0.");
        }
        this.maxRows = max;
    }

    @Override
    public void setEscapeProcessing(boolean enable) throws SQLException {
        this.checkClosed();
        this.escapeProcessing = enable;
    }

    @Override
    public int getQueryTimeout() throws SQLException {
        this.checkClosed();
        return queryTimeout;
    }

    @Override
    public void setQueryTimeout(int seconds) throws SQLException {
        this.checkClosed();
        if (seconds < 0) {
            throw new SQLException("Query timeout must be >= 0.");
        }
        this.queryTimeout = seconds;
    }

    @Override
    public void cancel() throws SQLException {
        this.checkClosed();
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
    public void setCursorName(String name) throws SQLException {
        this.checkClosed();
        this.cursorName = name;
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
        this.checkClosed();
        if (direction != ResultSet.FETCH_FORWARD
                && direction != ResultSet.FETCH_REVERSE
                && direction != ResultSet.FETCH_UNKNOWN) {
            throw new SQLException("Invalid fetch direction: " + direction);
        }
        this.fetchDirection = direction;
    }

    @Override
    public int getFetchDirection() throws SQLException {
        this.checkClosed();
        return fetchDirection;
    }

    @Override
    public void setFetchSize(int rows) throws SQLException {
        this.checkClosed();
        if (rows < 0) {
            throw new SQLException("Fetch size must be >= 0.");
        }
        this.fetchSize = rows;
    }

    @Override
    public int getFetchSize() throws SQLException {
        this.checkClosed();
        return fetchSize;
    }

    @Override
    public void addBatch(String sql) throws SQLException {
        this.checkClosed();
        batchSql.add(sql);
    }

    @Override
    public void clearBatch() throws SQLException {
        this.checkClosed();
        batchSql.clear();
    }

    @Override
    public int[] executeBatch() throws SQLException {
        this.checkClosed();
        int[] counts = new int[batchSql.size()];
        for (int i = 0; i < batchSql.size(); i++) {
            try {
                counts[i] = executeUpdate(batchSql.get(i));
            } catch (SQLException exception) {
                throw new BatchUpdateException(exception.getMessage(), counts, exception);
            }
        }
        clearBatch();
        return counts;
    }

    @Override
    public boolean getMoreResults(int current) throws SQLException {
        this.checkClosed();
        if (current == Statement.CLOSE_CURRENT_RESULT || current == Statement.CLOSE_ALL_RESULTS) {
            if (currentResultSet != null) {
                currentResultSet.close();
                currentResultSet = null;
            }
        } else if (current != Statement.KEEP_CURRENT_RESULT) {
            throw new SQLException("Invalid getMoreResults flag: " + current);
        }
        return false;
    }

    @Override
    public ResultSet getGeneratedKeys() throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int executeUpdate(String sql, int autoGeneratedKeys) throws SQLException {
        if (autoGeneratedKeys == Statement.NO_GENERATED_KEYS) {
            return executeUpdate(sql);
        }
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int executeUpdate(String sql, int[] columnIndexes) throws SQLException {
        if (columnIndexes == null || columnIndexes.length == 0) {
            return executeUpdate(sql);
        }
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int executeUpdate(String sql, String[] columnNames) throws SQLException {
        if (columnNames == null || columnNames.length == 0) {
            return executeUpdate(sql);
        }
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public boolean execute(String sql, int autoGeneratedKeys) throws SQLException {
        if (autoGeneratedKeys == Statement.NO_GENERATED_KEYS) {
            return execute(sql);
        }
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public boolean execute(String sql, int[] columnIndexes) throws SQLException {
        if (columnIndexes == null || columnIndexes.length == 0) {
            return execute(sql);
        }
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public boolean execute(String sql, String[] columnNames) throws SQLException {
        if (columnNames == null || columnNames.length == 0) {
            return execute(sql);
        }
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public void setPoolable(boolean poolable) throws SQLException {
        this.checkClosed();
        this.poolable = poolable;
    }

    @Override
    public boolean isPoolable() throws SQLException {
        this.checkClosed();
        return poolable;
    }

    @Override
    public void closeOnCompletion() throws SQLException {
        this.checkClosed();
        this.closeOnCompletion = true;
    }

    @Override
    public boolean isCloseOnCompletion() throws SQLException {
        this.checkClosed();
        return closeOnCompletion;
    }


    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return JdbcWrapperSupport.unwrap(this, iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return JdbcWrapperSupport.isWrapperFor(this, iface);
    }
}
