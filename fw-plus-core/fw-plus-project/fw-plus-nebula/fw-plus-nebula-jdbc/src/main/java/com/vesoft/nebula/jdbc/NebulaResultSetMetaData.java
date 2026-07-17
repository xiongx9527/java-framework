/* Copyright (c) 2020 vesoft inc. All rights reserved.
 *
 * This source code is licensed under Apache 2.0 License.
 */

package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.jdbc.utils.ExceptionBuilder;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

public class NebulaResultSetMetaData implements ResultSetMetaData {

    private final NebulaResultSet nebulaResultSet;

    private NebulaResultSetMetaData(NebulaResultSet nebulaResultSet) {
        this.nebulaResultSet = nebulaResultSet;
    }

    public static NebulaResultSetMetaData getInstance(NebulaResultSet nebulaResultSet) {
        return new NebulaResultSetMetaData(nebulaResultSet);
    }

    @Override
    public int getColumnCount() throws SQLException {
        List<String> columnNames = getColumnNames();
        return columnNames == null ? 0 : columnNames.size();
    }

    @Override
    public String getColumnName(int column) throws SQLException {
        checkColumnIndex(column);
        return this.nebulaResultSet.getColumnNames().get(column - 1);
    }

    @Override
    public String getSchemaName(int column) throws SQLException {
        checkColumnIndex(column);
        return this.nebulaResultSet.getNativeNebulaResultSet().getSpaceName();
    }

    private List<String> getColumnNames() {
        return nebulaResultSet == null ? null : nebulaResultSet.getColumnNames();
    }

    private void checkColumnIndex(int column) throws SQLException {
        int columnCount = getColumnCount();
        if (column < 1 || column > columnCount) {
            throw new SQLException(String.format("The numbers of column is [%d], your column index [%d] is invalid.", columnCount, column));
        }
    }

    @Override
    public boolean isAutoIncrement(int column) throws SQLException {
        checkColumnIndex(column);
        return false;
    }

    @Override
    public boolean isCaseSensitive(int column) throws SQLException {
        checkColumnIndex(column);
        return true;
    }

    @Override
    public boolean isSearchable(int column) throws SQLException {
        checkColumnIndex(column);
        return false;
    }

    @Override
    public boolean isCurrency(int column) throws SQLException {
        checkColumnIndex(column);
        return false;
    }

    @Override
    public int isNullable(int column) throws SQLException {
        checkColumnIndex(column);
        return ResultSetMetaData.columnNullableUnknown;
    }

    @Override
    public boolean isSigned(int column) throws SQLException {
        checkColumnIndex(column);
        return false;
    }

    @Override
    public int getColumnDisplaySize(int column) throws SQLException {
        checkColumnIndex(column);
        return 0;
    }

    @Override
    public String getColumnLabel(int column) throws SQLException {
        return getColumnName(column);
    }

    @Override
    public int getPrecision(int column) throws SQLException {
        checkColumnIndex(column);
        return 0;
    }

    @Override
    public int getScale(int column) throws SQLException {
        checkColumnIndex(column);
        return 0;
    }

    @Override
    public String getTableName(int column) throws SQLException {
        checkColumnIndex(column);
        return "";
    }

    @Override
    public String getCatalogName(int column) throws SQLException {
        checkColumnIndex(column);
        return "";
    }

    @Override
    public int getColumnType(int column) throws SQLException {
        checkColumnIndex(column);
        return Types.JAVA_OBJECT;
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        checkColumnIndex(column);
        return "JAVA_OBJECT";
    }

    @Override
    public boolean isReadOnly(int column) throws SQLException {
        checkColumnIndex(column);
        return true;
    }

    @Override
    public boolean isWritable(int column) throws SQLException {
        checkColumnIndex(column);
        return false;
    }

    @Override
    public boolean isDefinitelyWritable(int column) throws SQLException {
        checkColumnIndex(column);
        return false;
    }

    @Override
    public String getColumnClassName(int column) throws SQLException {
        checkColumnIndex(column);
        return Object.class.getName();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }
}
