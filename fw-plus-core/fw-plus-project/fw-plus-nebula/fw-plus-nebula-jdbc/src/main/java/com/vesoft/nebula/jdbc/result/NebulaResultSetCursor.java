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

    public boolean previous() {
        clearColumnAccess();
        if (currentRowNumber - 1 >= 0) {
            currentRowNumber--;
            afterLast = false;
            return true;
        }
        currentRowNumber = -1;
        afterLast = false;
        return false;
    }

    public void beforeFirst() {
        clearColumnAccess();
        currentRowNumber = -1;
        afterLast = false;
    }

    public void afterLast() {
        clearColumnAccess();
        currentRowNumber = rowCount;
        afterLast = true;
    }

    public boolean first() {
        clearColumnAccess();
        if (rowCount == 0) {
            currentRowNumber = -1;
            afterLast = false;
            return false;
        }
        currentRowNumber = 0;
        afterLast = false;
        return true;
    }

    public boolean last() {
        clearColumnAccess();
        if (rowCount == 0) {
            currentRowNumber = -1;
            afterLast = false;
            return false;
        }
        currentRowNumber = rowCount - 1;
        afterLast = false;
        return true;
    }

    public boolean absolute(int row) {
        clearColumnAccess();
        if (row > 0) {
            return moveToRow(row - 1);
        }
        if (row < 0) {
            return moveToRow(rowCount + row);
        }
        beforeFirstWithoutClearingColumnAccess();
        return false;
    }

    public boolean relative(int rows) {
        clearColumnAccess();
        return moveToRow(currentRowNumber + rows);
    }

    public boolean isBeforeFirst() {
        return currentRowNumber < 0;
    }

    public boolean isAfterLast() {
        return afterLast || currentRowNumber >= rowCount;
    }

    public boolean isFirst() {
        return rowCount > 0 && currentRowNumber == 0 && !afterLast;
    }

    public boolean isLast() {
        return rowCount > 0 && currentRowNumber == rowCount - 1 && !afterLast;
    }

    public int getRow() {
        if (isBeforeFirst() || isAfterLast()) {
            return 0;
        }
        return currentRowNumber + 1;
    }

    public int getCurrentRowNumber() {
        return currentRowNumber;
    }

    public void checkReadable() throws SQLException {
        if (isBeforeFirst()) {
            throw new SQLException("ResultSet cursor is before first row.");
        }
        if (isAfterLast()) {
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

    private boolean moveToRow(int rowNumber) {
        if (rowNumber < 0) {
            beforeFirstWithoutClearingColumnAccess();
            return false;
        }
        if (rowNumber >= rowCount) {
            afterLastWithoutClearingColumnAccess();
            return false;
        }
        currentRowNumber = rowNumber;
        afterLast = false;
        return true;
    }

    private void beforeFirstWithoutClearingColumnAccess() {
        currentRowNumber = -1;
        afterLast = false;
    }

    private void afterLastWithoutClearingColumnAccess() {
        currentRowNumber = rowCount;
        afterLast = true;
    }

    private void clearColumnAccess() {
        lastColumnAccess = null;
        hasColumnAccess = false;
    }
}
