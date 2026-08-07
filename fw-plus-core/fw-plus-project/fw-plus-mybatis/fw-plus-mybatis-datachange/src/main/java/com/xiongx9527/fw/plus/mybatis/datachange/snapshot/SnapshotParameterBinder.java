package com.xiongx9527.fw.plus.mybatis.datachange.snapshot;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.SnapshotParameter;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

public final class SnapshotParameterBinder {

	public void bind(PreparedStatement statement, List<SnapshotParameter> parameters, Configuration configuration) {
		if (statement == null || configuration == null) {
			throw new IllegalArgumentException("statement and configuration must not be null");
		}
		List<SnapshotParameter> safeParameters = parameters == null
				? Collections.<SnapshotParameter>emptyList() : parameters;
		try {
			for (int index = 0; index < safeParameters.size(); index++) {
				SnapshotParameter parameter = safeParameters.get(index);
				ParameterMapping mapping = parameter.getParameterMapping();
				Object value = mapping == null ? parameter.getValue()
						: boundValue(parameter.getBoundSql(), mapping, configuration);
				TypeHandler<Object> handler = handler(mapping, value, configuration);
				JdbcType jdbcType = mapping == null ? null : mapping.getJdbcType();
				if (value == null && jdbcType == null) {
					jdbcType = configuration.getJdbcTypeForNull();
				}
				handler.setParameter(statement, index + 1, value, jdbcType);
			}
		} catch (SQLException exception) {
			throw new DataChangeException(DataChangeErrorCode.SNAPSHOT,
					"SNAPSHOT_PARAMETER_BINDING_FAILED");
		} catch (DataChangeException exception) {
			throw exception;
		} catch (RuntimeException exception) {
			throw new DataChangeException(DataChangeErrorCode.SNAPSHOT,
					"SNAPSHOT_PARAMETER_BINDING_FAILED");
		}
	}

	private Object boundValue(BoundSql boundSql, ParameterMapping mapping, Configuration configuration) {
		if (boundSql == null) {
			throw new DataChangeException(DataChangeErrorCode.SNAPSHOT,
					"SNAPSHOT_PARAMETER_CONTEXT_UNAVAILABLE");
		}
		String property = mapping.getProperty();
		if (boundSql.hasAdditionalParameter(property)) {
			return boundSql.getAdditionalParameter(property);
		}
		Object parameterObject = boundSql.getParameterObject();
		if (parameterObject == null) {
			return null;
		}
		TypeHandlerRegistry registry = configuration.getTypeHandlerRegistry();
		if (registry.hasTypeHandler(parameterObject.getClass())) {
			return parameterObject;
		}
		MetaObject metaObject = configuration.newMetaObject(parameterObject);
		return metaObject.getValue(property);
	}

	@SuppressWarnings("unchecked")
	private TypeHandler<Object> handler(ParameterMapping mapping, Object value, Configuration configuration) {
		TypeHandler<?> selected = mapping == null ? null : mapping.getTypeHandler();
		if (selected == null && value != null) {
			selected = configuration.getTypeHandlerRegistry().getTypeHandler(value.getClass());
		}
		if (selected == null) {
			selected = configuration.getTypeHandlerRegistry().getUnknownTypeHandler();
		}
		return (TypeHandler<Object>) selected;
	}
}
