/* Copyright (c) 2020 vesoft inc. All rights reserved.
 *
 * This source code is licensed under Apache 2.0 License.
 */

package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.jdbc.statement.NebulaPreparedStatementImpl;
import com.vesoft.nebula.jdbc.sql.NebulaParameterBinding;
import com.vesoft.nebula.jdbc.utils.ExceptionBuilder;
import com.vesoft.nebula.jdbc.utils.JdbcWrapperSupport;
import java.math.BigDecimal;
import java.sql.ParameterMetaData;
import java.sql.SQLException;
import java.sql.Types;

public class NebulaParameterMetaData implements ParameterMetaData{

    private NebulaPreparedStatementImpl preparedStatement;

    private NebulaParameterMetaData(NebulaPreparedStatementImpl preparedStatement){
        this.preparedStatement = preparedStatement;
    }

    public static ParameterMetaData getInstance(NebulaPreparedStatementImpl preparedStatement){
        return new NebulaParameterMetaData(preparedStatement);
    }

    /**  If you want to get MetaData after PreparedStatement changes, the method below should be called. */
    public static void release(){
    }

    @Override
    public int getParameterCount() throws SQLException {
        return preparedStatement.getParametersNumber();
    }

    @Override
    public String getParameterClassName(int param) throws SQLException {
        NebulaParameterBinding binding = preparedStatement.getParameterBindings().get(param);
        if(binding != null && binding.getValue() != null){
            return binding.getValue().getClass().getName();
        }
        return Object.class.getName();
    }

    @Override
    public String getParameterTypeName(int param) throws SQLException {
        return typeName(getParameterType(param));
    }

    @Override
    public int isNullable(int param) throws SQLException {
        preparedStatement.getParameterBindings().get(param);
        return ParameterMetaData.parameterNullableUnknown;
    }

    @Override
    public boolean isSigned(int param) throws SQLException {
        switch (getParameterType(param)) {
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
            case Types.FLOAT:
            case Types.REAL:
            case Types.DOUBLE:
            case Types.NUMERIC:
            case Types.DECIMAL:
                return true;
            default:
                return false;
        }
    }

    @Override
    public int getPrecision(int param) throws SQLException {
        NebulaParameterBinding binding = preparedStatement.getParameterBindings().get(param);
        if (binding == null || binding.getValue() == null) {
            return 0;
        }
        if (binding.getScaleOrLength() != null) {
            return binding.getScaleOrLength();
        }
        Object value = binding.getValue();
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).precision();
        }
        if (value instanceof byte[]) {
            return ((byte[]) value).length;
        }
        if (value instanceof CharSequence) {
            return ((CharSequence) value).length();
        }
        return 0;
    }

    @Override
    public int getScale(int param) throws SQLException {
        NebulaParameterBinding binding = preparedStatement.getParameterBindings().get(param);
        return binding == null || binding.getScaleOrLength() == null ? 0 : binding.getScaleOrLength();
    }

    @Override
    public int getParameterType(int param) throws SQLException {
        NebulaParameterBinding binding = preparedStatement.getParameterBindings().get(param);
        return binding == null ? Types.JAVA_OBJECT : binding.getJdbcType();
    }

    @Override
    public int getParameterMode(int param) throws SQLException {
        preparedStatement.getParameterBindings().get(param);
        return ParameterMetaData.parameterModeIn;
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return JdbcWrapperSupport.unwrap(this, iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return JdbcWrapperSupport.isWrapperFor(this, iface);
    }

    private String typeName(int jdbcType) {
        switch (jdbcType) {
            case Types.BOOLEAN:
                return "BOOLEAN";
            case Types.TINYINT:
                return "TINYINT";
            case Types.SMALLINT:
                return "SMALLINT";
            case Types.INTEGER:
                return "INTEGER";
            case Types.BIGINT:
                return "BIGINT";
            case Types.FLOAT:
                return "FLOAT";
            case Types.REAL:
                return "REAL";
            case Types.DOUBLE:
                return "DOUBLE";
            case Types.NUMERIC:
                return "NUMERIC";
            case Types.DECIMAL:
                return "DECIMAL";
            case Types.CHAR:
                return "CHAR";
            case Types.VARCHAR:
                return "VARCHAR";
            case Types.LONGVARCHAR:
                return "LONGVARCHAR";
            case Types.NCHAR:
                return "NCHAR";
            case Types.NVARCHAR:
                return "NVARCHAR";
            case Types.LONGNVARCHAR:
                return "LONGNVARCHAR";
            case Types.DATE:
                return "DATE";
            case Types.TIME:
                return "TIME";
            case Types.TIMESTAMP:
                return "TIMESTAMP";
            case Types.BINARY:
                return "BINARY";
            case Types.VARBINARY:
                return "VARBINARY";
            case Types.LONGVARBINARY:
                return "LONGVARBINARY";
            case Types.NULL:
                return "NULL";
            case Types.DATALINK:
                return "DATALINK";
            case Types.JAVA_OBJECT:
                return "JAVA_OBJECT";
            case Types.OTHER:
            default:
                return "OTHER";
        }
    }
}
