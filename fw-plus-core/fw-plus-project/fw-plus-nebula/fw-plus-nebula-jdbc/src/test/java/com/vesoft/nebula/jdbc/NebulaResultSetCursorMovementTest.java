package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.client.graph.data.ResultSet;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NebulaResultSetCursorMovementTest {

    @Test
    public void firstShouldSynchronizeCursorForReads() throws Exception {
        ValueWrapper firstValue = nonNullValue();
        NebulaResultSet resultSet = resultSetWithRows(firstValue, nonNullValue());

        assertTrue(resultSet.first());

        assertEquals(1, resultSet.getRow());
        assertSame(firstValue, resultSet.getObject(1));
        assertFalse(resultSet.wasNull());
    }

    @Test
    public void beforeFirstShouldMakeCurrentRowUnreadable() throws Exception {
        NebulaResultSet resultSet = resultSetWithRows(nonNullValue());
        assertTrue(resultSet.next());

        resultSet.beforeFirst();

        assertTrue(resultSet.isBeforeFirst());
        assertEquals(0, resultSet.getRow());
        assertUnreadable(resultSet, "before");
    }

    @Test
    public void afterLastShouldMakeCurrentRowUnreadable() throws Exception {
        NebulaResultSet resultSet = resultSetWithRows(nonNullValue());
        assertTrue(resultSet.next());

        resultSet.afterLast();

        assertTrue(resultSet.isAfterLast());
        assertEquals(0, resultSet.getRow());
        assertUnreadable(resultSet, "after");
    }

    @Test
    public void absoluteAndRelativeShouldExposeTheSameCursorStateToRowsAndReads() throws Exception {
        ValueWrapper firstValue = nonNullValue();
        ValueWrapper secondValue = nonNullValue();
        NebulaResultSet resultSet = resultSetWithRows(firstValue, secondValue);

        assertTrue(resultSet.absolute(1));
        assertEquals(1, resultSet.getRow());
        assertSame(firstValue, resultSet.getObject(1));

        assertTrue(resultSet.relative(1));
        assertEquals(2, resultSet.getRow());
        assertSame(secondValue, resultSet.getObject(1));
    }

    @Test
    public void previousShouldExposeTheSameCursorStateToRowsAndReads() throws Exception {
        ValueWrapper firstValue = nonNullValue();
        ValueWrapper secondValue = nonNullValue();
        NebulaResultSet resultSet = resultSetWithRows(firstValue, secondValue);
        assertTrue(resultSet.last());

        assertTrue(resultSet.previous());

        assertEquals(1, resultSet.getRow());
        assertSame(firstValue, resultSet.getObject(1));
    }

    private static NebulaResultSet resultSetWithRows(ValueWrapper... values) {
        ResultSet nativeResultSet = mock(ResultSet.class);
        when(nativeResultSet.isEmpty()).thenReturn(values.length == 0);
        when(nativeResultSet.rowsSize()).thenReturn(values.length);
        when(nativeResultSet.getColumnNames()).thenReturn(Collections.singletonList("value"));
        for (int i = 0; i < values.length; i++) {
            ResultSet.Record record = mock(ResultSet.Record.class);
            when(record.get(0)).thenReturn(values[i]);
            when(nativeResultSet.rowValues(i)).thenReturn(record);
        }
        return new NebulaResultSet(nativeResultSet, null);
    }

    private static ValueWrapper nonNullValue() {
        ValueWrapper valueWrapper = mock(ValueWrapper.class);
        when(valueWrapper.isNull()).thenReturn(false);
        return valueWrapper;
    }

    private static void assertUnreadable(NebulaResultSet resultSet, String expectedMessagePart) throws Exception {
        try {
            resultSet.getObject(1);
            fail("Expected SQLException");
        } catch (SQLException exception) {
            assertTrue(exception.getMessage().toLowerCase().contains(expectedMessagePart));
        }
    }
}
