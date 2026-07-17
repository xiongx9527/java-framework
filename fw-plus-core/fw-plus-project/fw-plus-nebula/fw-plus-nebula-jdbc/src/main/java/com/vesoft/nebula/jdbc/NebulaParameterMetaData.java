/* Copyright (c) 2020 vesoft inc. All rights reserved.
 *
 * This source code is licensed under Apache 2.0 License.
 */

package com.vesoft.nebula.jdbc;

import com.vesoft.nebula.jdbc.statement.NebulaPreparedStatementImpl;
import com.vesoft.nebula.jdbc.sql.NebulaParameterBinding;
import com.vesoft.nebula.jdbc.utils.ExceptionBuilder;
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
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int isNullable(int param) throws SQLException {
        preparedStatement.getParameterBindings().get(param);
        return ParameterMetaData.parameterNullableUnknown;
    }

    @Override
    public boolean isSigned(int param) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public int getPrecision(int param) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
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
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        throw ExceptionBuilder.buildUnsupportedOperationException();
    }
}
