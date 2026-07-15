package com.vesoft.nebula.jdbc.result;

import com.vesoft.nebula.client.graph.data.ValueWrapper;

import java.sql.SQLException;

public class NebulaResultSetCursor {
    private final int rowCount;
    private int rowIndex = -1;
    private Boolean lastColumnWasNull;

    public NebulaResultSetCursor(int rowCount) {
        this.rowCount = Math.max(rowCount, 0);
    }

    public boolean next() {
        lastColumnWasNull = null;
        if (rowIndex < rowCount) {
            rowIndex++;
        }
        return rowIndex >= 0 && rowIndex < rowCount;
    }

    public void checkReadable() throws SQLException {
        if (rowIndex < 0) {
            throw new SQLException("ResultSet cursor is before first row.");
        }
        if (rowIndex >= rowCount) {
            throw new SQLException("ResultSet cursor is after last row.");
        }
    }

    public void recordColumnAccess(ValueWrapper value) {
        lastColumnWasNull = value == null || value.isNull();
    }

    public boolean wasNull() throws SQLException {
        if (lastColumnWasNull == null) {
            throw new SQLException("No column has been read before wasNull().");
        }
        return lastColumnWasNull;
    }
}
