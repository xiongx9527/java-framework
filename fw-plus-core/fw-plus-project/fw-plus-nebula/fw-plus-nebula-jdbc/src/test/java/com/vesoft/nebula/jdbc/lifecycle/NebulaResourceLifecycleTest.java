package com.vesoft.nebula.jdbc.lifecycle;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.NebulaResultSet;
import com.vesoft.nebula.jdbc.statement.NebulaStatementImpl;
import com.vesoft.nebula.jdbc.utils.NebulaPropertyKey;
import org.junit.Test;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;

public class NebulaResourceLifecycleTest {

    @Test
    public void closeShouldExecuteActionOnlyOnce() throws Exception {
        NebulaResourceLifecycle lifecycle = new NebulaResourceLifecycle();
        final int[] closeCount = {0};

        lifecycle.close(() -> closeCount[0]++);
        lifecycle.close(() -> closeCount[0]++);

        assertEquals(1, closeCount[0]);
        assertTrue(lifecycle.isClosed());
    }

    @Test
    public void closeShouldKeepLifecycleOpenUntilActionReturns() throws Exception {
        NebulaResourceLifecycle lifecycle = new NebulaResourceLifecycle();
        final boolean[] closedDuringAction = {true};

        lifecycle.close(() -> closedDuringAction[0] = lifecycle.isClosed());

        assertEquals(false, closedDuringAction[0]);
        assertTrue(lifecycle.isClosed());
    }

    @Test
    public void checkOpenShouldThrowSQLExceptionAfterClose() throws Exception {
        NebulaResourceLifecycle lifecycle = new NebulaResourceLifecycle();
        lifecycle.close(() -> { });

        try {
            lifecycle.checkOpen("Connection");
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("Connection"));
        }
    }

    @Test
    public void closeShouldExposeFirstCloseFailureAsSQLException() throws Exception {
        NebulaResourceLifecycle lifecycle = new NebulaResourceLifecycle();
        SQLException failure = new SQLException("close failed");

        try {
            lifecycle.close(() -> {
                throw failure;
            });
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertSame(failure, exception);
        }

        lifecycle.close(() -> {
            throw new AssertionError("close action must not be called twice");
        });
        assertTrue(lifecycle.isClosed());
    }

    @Test
    public void statementShouldRejectAccessAfterClose() throws Exception {
        NebulaStatementImpl statement = new NebulaStatementImpl(null);

        statement.close();

        try {
            statement.getResultSet();
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("Statement"));
        }
    }

    @Test
    public void resultSetWasNullShouldRejectAccessAfterClose() throws Exception {
        NebulaResultSet resultSet = new NebulaResultSet(mock(com.vesoft.nebula.client.graph.data.ResultSet.class), null);

        resultSet.close();

        try {
            resultSet.wasNull();
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("ResultSet"));
        }
    }

    @Test
    public void statementExecuteQueryShouldRejectClosedBeforeClassifyingSql() throws Exception {
        NebulaStatementImpl statement = new NebulaStatementImpl(null);

        statement.close();

        try {
            statement.executeQuery("insert vertex person(name) values 1:(\"Ann\")");
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("Statement"));
        }
    }

    @Test
    public void statementExecuteUpdateShouldRejectClosedBeforeClassifyingSql() throws Exception {
        NebulaStatementImpl statement = new NebulaStatementImpl(null);

        statement.close();

        try {
            statement.executeUpdate("match (v) return v");
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("Statement"));
        }
    }

    @Test
    public void resultSetShouldRejectStatusAccessAfterClose() throws Exception {
        NebulaResultSet resultSet = new NebulaResultSet(mock(com.vesoft.nebula.client.graph.data.ResultSet.class), null);

        resultSet.close();

        try {
            resultSet.isBeforeFirst();
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("ResultSet"));
        }
    }

    @Test
    public void connectionImplementedAccessorsShouldRejectAccessAfterClose() throws Exception {
        NebulaConnection connection = closedConnection();

        assertClosedConnection(() -> connection.getSchema());
        assertClosedConnection(() -> connection.getMetaData());
        assertClosedConnection(() -> connection.getAutoCommit());
        assertClosedConnection(() -> connection.getHoldability());
        assertClosedConnection(() -> connection.setSchema("another_space"));
        assertClosedConnection(() -> connection.setAutoCommit(true));
        assertClosedConnection(() -> connection.isReadOnly());
        assertClosedConnection(() -> connection.setReadOnly(true));
        assertClosedConnection(() -> connection.getTransactionIsolation());
        assertClosedConnection(() -> connection.setTransactionIsolation(java.sql.Connection.TRANSACTION_NONE));
        assertClosedConnection(() -> connection.getWarnings());
        assertClosedConnection(() -> connection.clearWarnings());
        assertClosedConnection(() -> connection.getTypeMap());
        assertClosedConnection(() -> connection.setTypeMap(null));
        assertClosedConnection(() -> connection.setHoldability(java.sql.ResultSet.CLOSE_CURSORS_AT_COMMIT));
        assertClosedConnection(() -> connection.rollback((Savepoint) null));
        assertClosedConnection(() -> connection.getClientInfo());
        assertClosedConnection(() -> connection.getClientInfo("client"));
        assertClosedConnection(() -> connection.getConnectionConfig());
    }

    @Test
    public void statementWarningsShouldRejectAccessAfterClose() throws Exception {
        Statement statement = new NebulaStatementImpl(null);

        statement.close();

        try {
            SQLWarning ignored = statement.getWarnings();
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("Statement"));
        }
    }

    private NebulaConnection closedConnection() throws Exception {
        NebulaConnection connection = mock(NebulaConnection.class, CALLS_REAL_METHODS);
        Properties properties = new Properties();
        properties.setProperty(NebulaPropertyKey.DBNAME.getKeyName(), "test_space");
        setField(connection, "properties", properties);
        setField(connection, "lifecycle", closedLifecycle());
        return connection;
    }

    private NebulaResourceLifecycle closedLifecycle() throws SQLException {
        NebulaResourceLifecycle lifecycle = new NebulaResourceLifecycle();
        lifecycle.close(() -> { });
        return lifecycle;
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = NebulaConnection.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    private void assertClosedConnection(ThrowingJdbcAccess access) {
        try {
            access.call();
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("Connection"));
        }
    }

    @FunctionalInterface
    private interface ThrowingJdbcAccess {
        void call() throws SQLException;
    }
}
