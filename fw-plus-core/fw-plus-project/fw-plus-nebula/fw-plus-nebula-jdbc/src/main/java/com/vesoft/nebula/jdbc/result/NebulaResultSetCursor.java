package com.vesoft.nebula.jdbc.result;

import com.vesoft.nebula.client.graph.data.ValueWrapper;

import java.sql.SQLException;

public class NebulaResultSetCursor {
    private final int rowCount;
    private int currentRowNumber = -1;
    private boolean afterLast = false;
    private ValueWrapper lastColumnAccess;
    private boolean hasColumnAccess = false;

    public NebulaResultSetCursor(int rowCount) {
        this.rowCount = Math.max(rowCount, 0);
    }

    public boolean next() {
        clearColumnAccess();
        if (currentRowNumber + 1 < rowCount) {
            currentRowNumber++;
            afterLast = false;
            return true;
        }
        currentRowNumber = rowCount;
        afterLast = true;
        return false;
    }

    public int getCurrentRowNumber() {
        return currentRowNumber;
    }

    public void checkReadable() throws SQLException {
        if (currentRowNumber < 0) {
            throw new SQLException("ResultSet cursor is before first row.");
        }
        if (afterLast || currentRowNumber >= rowCount) {
            throw new SQLException("ResultSet cursor is after last row.");
        }
    }

    public void recordColumnAccess(ValueWrapper value) {
        lastColumnAccess = value;
        hasColumnAccess = true;
    }

    public boolean wasNull() throws SQLException {
        if (!hasColumnAccess) {
            throw new SQLException("wasNull() requires a previous column access.");
        }
        return lastColumnAccess == null || lastColumnAccess.isNull();
    }

    private void clearColumnAccess() {
        lastColumnAccess = null;
        hasColumnAccess = false;
    }
}
