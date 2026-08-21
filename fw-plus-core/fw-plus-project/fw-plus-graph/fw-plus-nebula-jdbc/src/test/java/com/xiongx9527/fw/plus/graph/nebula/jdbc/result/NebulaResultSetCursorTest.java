package com.xiongx9527.fw.plus.graph.nebula.jdbc.result;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaNativeValue;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
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
    public void emptyResultSetShouldMoveDirectlyAfterLast() throws Exception {
        NebulaResultSetCursor cursor = new NebulaResultSetCursor(0);

        assertFalse(cursor.next());
        assertEquals(0, cursor.getCurrentRowNumber());
        assertUnreadable(cursor, "after");
    }

    @Test
    public void currentRowNumberShouldExposeReadableRowIndexForNativeLookup() throws Exception {
        NebulaResultSetCursor cursor = new NebulaResultSetCursor(2);

        assertTrue(cursor.next());
        assertEquals(0, cursor.getCurrentRowNumber());
        assertTrue(cursor.next());
        assertEquals(1, cursor.getCurrentRowNumber());
        assertFalse(cursor.next());
        assertEquals(2, cursor.getCurrentRowNumber());
    }

    @Test
    public void wasNullShouldReturnLastColumnNullStateAfterColumnAccess() throws Exception {
        NebulaResultSetCursor cursor = new NebulaResultSetCursor(1);
        NebulaNativeValue nullValue = mock(NebulaNativeValue.class);
        NebulaNativeValue nonNullValue = mock(NebulaNativeValue.class);
        when(nullValue.isNull()).thenReturn(true);
        when(nonNullValue.isNull()).thenReturn(false);

        assertTrue(cursor.next());
        cursor.recordColumnAccess(nullValue);
        assertTrue(cursor.wasNull());
        cursor.recordColumnAccess(nonNullValue);
        assertFalse(cursor.wasNull());
        cursor.recordColumnAccess(null);
        assertTrue(cursor.wasNull());
    }

    @Test
    public void wasNullShouldRejectCallsBeforeColumnAccess() throws Exception {
        NebulaResultSetCursor cursor = new NebulaResultSetCursor(1);

        assertTrue(cursor.next());
        try {
            cursor.wasNull();
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().contains("wasNull"));
            assertTrue(exception.getMessage().contains("column access"));
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
