package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import org.apache.ibatis.mapping.ParameterMapping;

public final class SnapshotParameter {
	private final ParameterMapping parameterMapping;
	private final Object value;

	private SnapshotParameter(ParameterMapping parameterMapping, Object value) {
		this.parameterMapping = parameterMapping;
		this.value = value;
	}

	public static SnapshotParameter boundSql(ParameterMapping parameterMapping) {
		return new SnapshotParameter(parameterMapping, null);
	}

	public static SnapshotParameter value(Object value) {
		return new SnapshotParameter(null, value);
	}

	public ParameterMapping getParameterMapping() { return parameterMapping; }
	public Object getValue() { return value; }
	public boolean isBoundSqlParameter() { return parameterMapping != null; }
}
