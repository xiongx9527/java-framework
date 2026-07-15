package com.vesoft.nebula.jdbc.lifecycle;

import com.vesoft.nebula.jdbc.NebulaResultSet;
import com.vesoft.nebula.jdbc.statement.NebulaStatementImpl;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
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
}
