package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.jdbc.client.NebulaNativeResultSet;
import com.vesoft.nebula.jdbc.client.NebulaNativeRecord;
import com.vesoft.nebula.jdbc.client.NebulaNativeValue;
import org.junit.Test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.vesoft.nebula.jdbc.test.NativeResultSets.record;
import static com.vesoft.nebula.jdbc.test.NativeResultSets.rows;

public class NebulaResultSetCursorMovementTest {

    @Test
    public void firstShouldSynchronizeCursorForReads() throws Exception {
        NebulaNativeValue firstValue = nonNullValue("first");
        NebulaResultSet resultSet = resultSetWithRows(firstValue, nonNullValue());

        assertTrue(resultSet.first());

        assertEquals(1, resultSet.getRow());
        assertEquals("first", resultSet.getObject(1));
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
        NebulaNativeValue firstValue = nonNullValue("first");
        NebulaNativeValue secondValue = nonNullValue("second");
        NebulaResultSet resultSet = resultSetWithRows(firstValue, secondValue);

        assertTrue(resultSet.absolute(1));
        assertEquals(1, resultSet.getRow());
        assertEquals("first", resultSet.getObject(1));

        assertTrue(resultSet.relative(1));
        assertEquals(2, resultSet.getRow());
        assertEquals("second", resultSet.getObject(1));
    }

    @Test
    public void previousShouldExposeTheSameCursorStateToRowsAndReads() throws Exception {
        NebulaNativeValue firstValue = nonNullValue("first");
        NebulaNativeValue secondValue = nonNullValue("second");
        NebulaResultSet resultSet = resultSetWithRows(firstValue, secondValue);
        assertTrue(resultSet.last());

        assertTrue(resultSet.previous());

        assertEquals(1, resultSet.getRow());
        assertEquals("first", resultSet.getObject(1));
    }

    @Test
    public void typedGettersShouldReturnJdbcDefaultsAndExposeWasNullForNullValues() throws Exception {
        NebulaResultSet resultSet = resultSetWithRows(nullValue());
        assertTrue(resultSet.next());

        assertNull(resultSet.getString(1));
        assertTrue(resultSet.wasNull());
        assertEquals(0, resultSet.getInt(1));
        assertTrue(resultSet.wasNull());
        assertEquals(0L, resultSet.getLong(1));
        assertTrue(resultSet.wasNull());
        assertFalse(resultSet.getBoolean(1));
        assertTrue(resultSet.wasNull());
        assertEquals(0.0d, resultSet.getDouble(1), 0.0d);
        assertTrue(resultSet.wasNull());
        assertNull(resultSet.getDate(1));
        assertTrue(resultSet.wasNull());
        assertNull(resultSet.getTime(1));
        assertTrue(resultSet.wasNull());
        assertNull(resultSet.getObject(1));
        assertTrue(resultSet.wasNull());
    }

    private static NebulaResultSet resultSetWithRows(NebulaNativeValue... values) throws Exception {
        List<NebulaNativeRecord> records = new ArrayList<NebulaNativeRecord>();
        for (NebulaNativeValue value : values) {
            records.add(record(Collections.singletonList("value"), Collections.singletonList(value)));
        }
        return new NebulaResultSet(rows(Collections.singletonList("value"), records), null);
    }

    private static NebulaNativeValue nonNullValue() {
        return nonNullValue("value");
    }

    private static NebulaNativeValue nonNullValue(String value) {
        NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
        when(valueWrapper.isNull()).thenReturn(false);
        try {
            when(valueWrapper.asString()).thenReturn(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return valueWrapper;
    }

    private static NebulaNativeValue nullValue() {
        NebulaNativeValue valueWrapper = mock(NebulaNativeValue.class);
        when(valueWrapper.isNull()).thenReturn(true);
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
