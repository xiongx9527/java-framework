package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import javax.sql.rowset.CachedRowSet;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.RowSetProvider;

public class Neo4jArray implements Array {
    private final String typeName;
    private final int baseType;
    private Object[] elements;

    public Neo4jArray(String typeName, Object[] elements) {
        this.typeName = typeName == null ? "JAVA_OBJECT" : typeName.toUpperCase(Locale.ROOT);
        this.baseType = jdbcType(this.typeName);
        this.elements = elements == null ? new Object[0] : Arrays.copyOf(elements, elements.length);
    }

    @Override
    public String getBaseTypeName() throws SQLException {
        checkOpen();
        return typeName;
    }

    @Override
    public int getBaseType() throws SQLException {
        checkOpen();
        return baseType;
    }

    @Override
    public Object getArray() throws SQLException {
        checkOpen();
        return Arrays.copyOf(elements, elements.length);
    }

    @Override
    public Object getArray(Map<String, Class<?>> map) throws SQLException {
        return getArray();
    }

    @Override
    public Object getArray(long index, int count) throws SQLException {
        checkOpen();
        checkRange(index, count);
        return Arrays.copyOfRange(elements, (int) index - 1, (int) index - 1 + count);
    }

    @Override
    public Object getArray(long index, int count, Map<String, Class<?>> map) throws SQLException {
        return getArray(index, count);
    }

    @Override
    public ResultSet getResultSet() throws SQLException {
        checkOpen();
        return resultSet(1L, elements.length);
    }

    @Override
    public ResultSet getResultSet(Map<String, Class<?>> map) throws SQLException {
        return getResultSet();
    }

    @Override
    public ResultSet getResultSet(long index, int count) throws SQLException {
        checkOpen();
        checkRange(index, count);
        return resultSet(index, count);
    }

    @Override
    public ResultSet getResultSet(long index, int count, Map<String, Class<?>> map) throws SQLException {
        return getResultSet(index, count);
    }

    @Override
    public void free() throws SQLException {
        elements = null;
    }

    private void checkOpen() throws SQLException {
        if (elements == null) {
            throw new SQLException("Array has been freed.");
        }
    }

    private void checkRange(long index, int count) throws SQLException {
        if (index < 1 || count < 0 || index - 1 + count > elements.length) {
            throw new SQLException("Array slice is out of range.");
        }
        if (index > Integer.MAX_VALUE) {
            throw new SQLException("Array index exceeds supported in-memory size.");
        }
    }

    private ResultSet resultSet(long index, int count) throws SQLException {
        CachedRowSet rowSet = RowSetProvider.newFactory().createCachedRowSet();
        RowSetMetaDataImpl metaData = new RowSetMetaDataImpl();
        metaData.setColumnCount(2);
        metaData.setColumnName(1, "INDEX");
        metaData.setColumnLabel(1, "INDEX");
        metaData.setColumnType(1, Types.BIGINT);
        metaData.setColumnName(2, "VALUE");
        metaData.setColumnLabel(2, "VALUE");
        metaData.setColumnType(2, baseType);
        rowSet.setMetaData(metaData);
        for (int i = count - 1; i >= 0; i--) {
            long arrayIndex = index + i;
            rowSet.moveToInsertRow();
            rowSet.updateLong(1, arrayIndex);
            rowSet.updateObject(2, elements[(int) arrayIndex - 1]);
            rowSet.insertRow();
            rowSet.moveToCurrentRow();
        }
        rowSet.beforeFirst();
        return rowSet;
    }

    private static int jdbcType(String typeName) {
        if ("CHAR".equals(typeName)) {
            return Types.CHAR;
        }
        if ("VARCHAR".equals(typeName) || "STRING".equals(typeName)) {
            return Types.VARCHAR;
        }
        if ("NCHAR".equals(typeName)) {
            return Types.NCHAR;
        }
        if ("NVARCHAR".equals(typeName)) {
            return Types.NVARCHAR;
        }
        if ("INTEGER".equals(typeName) || "INT".equals(typeName) || "INT64".equals(typeName)) {
            return Types.INTEGER;
        }
        if ("BIGINT".equals(typeName)) {
            return Types.BIGINT;
        }
        if ("SMALLINT".equals(typeName)) {
            return Types.SMALLINT;
        }
        if ("TINYINT".equals(typeName)) {
            return Types.TINYINT;
        }
        if ("FLOAT".equals(typeName)) {
            return Types.FLOAT;
        }
        if ("DOUBLE".equals(typeName)) {
            return Types.DOUBLE;
        }
        if ("BOOLEAN".equals(typeName) || "BOOL".equals(typeName)) {
            return Types.BOOLEAN;
        }
        return Types.JAVA_OBJECT;
    }
}
