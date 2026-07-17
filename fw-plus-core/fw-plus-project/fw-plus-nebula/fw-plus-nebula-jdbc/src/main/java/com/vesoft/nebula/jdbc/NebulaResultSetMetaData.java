/* Copyright (c) 2020 vesoft inc. All rights reserved.
 *
 * This source code is licensed under Apache 2.0 License.
 */

package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.client.graph.data.ValueWrapper;
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
        return getColumnTypeDescriptor(column).signed;
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
        return getColumnTypeDescriptor(column).jdbcType;
    }

    @Override
    public String getColumnTypeName(int column) throws SQLException {
        checkColumnIndex(column);
        return getColumnTypeDescriptor(column).typeName;
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
        return getColumnTypeDescriptor(column).className;
    }

    private ColumnTypeDescriptor getColumnTypeDescriptor(int column) throws SQLException {
        ValueWrapper value = firstNonNullColumnValue(column);
        if (value == null) {
            return ColumnTypeDescriptor.JAVA_OBJECT;
        }
        if (value.isString()) {
            return ColumnTypeDescriptor.VARCHAR;
        }
        if (value.isLong()) {
            return ColumnTypeDescriptor.BIGINT;
        }
        if (value.isDouble()) {
            return ColumnTypeDescriptor.DOUBLE;
        }
        if (value.isBoolean()) {
            return ColumnTypeDescriptor.BOOLEAN;
        }
        if (value.isDate()) {
            return ColumnTypeDescriptor.DATE;
        }
        if (value.isTime()) {
            return ColumnTypeDescriptor.TIME;
        }
        if (value.isDateTime()) {
            return ColumnTypeDescriptor.TIMESTAMP;
        }
        return ColumnTypeDescriptor.JAVA_OBJECT;
    }

    private ValueWrapper firstNonNullColumnValue(int column) throws SQLException {
        if (this.nebulaResultSet == null || this.nebulaResultSet.getNativeNebulaResultSet() == null) {
            return null;
        }
        String columnName = getColumnName(column);
        try {
            List<ValueWrapper> values = this.nebulaResultSet.getNativeNebulaResultSet().colValues(columnName);
            if (values == null) {
                return null;
            }
            for (ValueWrapper value : values) {
                if (value != null && !value.isNull()) {
                    return value;
                }
            }
            return null;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static class ColumnTypeDescriptor {
        private static final ColumnTypeDescriptor VARCHAR = new ColumnTypeDescriptor(
                Types.VARCHAR,
                "VARCHAR",
                String.class.getName(),
                false
        );
        private static final ColumnTypeDescriptor BIGINT = new ColumnTypeDescriptor(
                Types.BIGINT,
                "BIGINT",
                Long.class.getName(),
                true
        );
        private static final ColumnTypeDescriptor DOUBLE = new ColumnTypeDescriptor(
                Types.DOUBLE,
                "DOUBLE",
                Double.class.getName(),
                true
        );
        private static final ColumnTypeDescriptor BOOLEAN = new ColumnTypeDescriptor(
                Types.BOOLEAN,
                "BOOLEAN",
                Boolean.class.getName(),
                false
        );
        private static final ColumnTypeDescriptor DATE = new ColumnTypeDescriptor(
                Types.DATE,
                "DATE",
                java.sql.Date.class.getName(),
                false
        );
        private static final ColumnTypeDescriptor TIME = new ColumnTypeDescriptor(
                Types.TIME,
                "TIME",
                java.sql.Time.class.getName(),
                false
        );
        private static final ColumnTypeDescriptor TIMESTAMP = new ColumnTypeDescriptor(
                Types.TIMESTAMP,
                "TIMESTAMP",
                java.sql.Timestamp.class.getName(),
                false
        );
        private static final ColumnTypeDescriptor JAVA_OBJECT = new ColumnTypeDescriptor(
                Types.JAVA_OBJECT,
                "JAVA_OBJECT",
                Object.class.getName(),
                false
        );

        private final int jdbcType;
        private final String typeName;
        private final String className;
        private final boolean signed;

        private ColumnTypeDescriptor(int jdbcType, String typeName, String className, boolean signed) {
            this.jdbcType = jdbcType;
            this.typeName = typeName;
            this.className = className;
            this.signed = signed;
        }
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
