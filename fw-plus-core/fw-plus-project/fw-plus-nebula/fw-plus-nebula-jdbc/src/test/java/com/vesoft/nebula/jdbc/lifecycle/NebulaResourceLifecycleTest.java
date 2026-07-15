package com.vesoft.nebula.jdbc.lifecycle;

import org.junit.Test;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NebulaResourceLifecycleTest {

    @Test
    public void closeShouldExecuteActionOnlyOnce() throws Exception {
        NebulaResourceLifecycle lifecycle = new NebulaResourceLifecycle();
        AtomicInteger closeCount = new AtomicInteger();

        lifecycle.close(closeCount::incrementAndGet);
        lifecycle.close(closeCount::incrementAndGet);

        assertEquals(1, closeCount.get());
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
}
