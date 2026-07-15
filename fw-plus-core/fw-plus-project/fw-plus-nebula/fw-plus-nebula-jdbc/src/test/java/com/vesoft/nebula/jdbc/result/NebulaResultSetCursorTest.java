package com.vesoft.nebula.jdbc.result;

import com.vesoft.nebula.client.graph.data.ValueWrapper;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NebulaResultSetCursorTest {

    @Test
    public void checkReadableShouldRejectBeforeFirstAndAfterLast() throws Exception {
        NebulaResultSetCursor cursor = new NebulaResultSetCursor(1);

        assertUnreadable(cursor, "before");
        assertTrue(cursor.next());
        cursor.checkReadable();
        assertFalse(cursor.next());
        assertUnreadable(cursor, "after");
    }

    @Test
    public void nextShouldMoveCursorWithinRows() throws Exception {
        NebulaResultSetCursor cursor = new NebulaResultSetCursor(2);

        assertTrue(cursor.next());
        cursor.checkReadable();
        assertTrue(cursor.next());
        cursor.checkReadable();
        assertFalse(cursor.next());
        assertFalse(cursor.next());
    }

    @Test
    public void wasNullShouldReturnLastColumnNullStateAfterColumnAccess() throws Exception {
        NebulaResultSetCursor cursor = new NebulaResultSetCursor(1);
        ValueWrapper nullValue = mock(ValueWrapper.class);
        ValueWrapper nonNullValue = mock(ValueWrapper.class);
        when(nullValue.isNull()).thenReturn(true);
        when(nonNullValue.isNull()).thenReturn(false);

        assertTrue(cursor.next());
        cursor.recordColumnAccess(nullValue);
        assertTrue(cursor.wasNull());
        cursor.recordColumnAccess(nonNullValue);
        assertFalse(cursor.wasNull());
    }

    @Test
    public void wasNullShouldRejectCallsBeforeColumnAccess() throws Exception {
        NebulaResultSetCursor cursor = new NebulaResultSetCursor(1);

        assertTrue(cursor.next());
        try {
            cursor.wasNull();
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("column"));
        }
    }

    private static void assertUnreadable(NebulaResultSetCursor cursor, String expectedMessagePart) throws Exception {
        try {
            cursor.checkReadable();
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().toLowerCase().contains(expectedMessagePart));
        }
    }
}
