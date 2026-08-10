package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.BoundSql;

public final class SnapshotParameter {
	private final ParameterMapping parameterMapping;
	private final BoundSql boundSql;
	private final Object value;

	private SnapshotParameter(ParameterMapping parameterMapping, BoundSql boundSql, Object value) {
		this.parameterMapping = parameterMapping;
		this.boundSql = boundSql;
		this.value = value;
	}

	public static SnapshotParameter boundSql(ParameterMapping parameterMapping) {
		return new SnapshotParameter(parameterMapping, null, null);
	}

	public static SnapshotParameter boundSql(ParameterMapping parameterMapping, BoundSql boundSql) {
		return new SnapshotParameter(parameterMapping, boundSql, null);
	}

	public static SnapshotParameter value(Object value) {
		return new SnapshotParameter(null, null, value);
	}

	public ParameterMapping getParameterMapping() { return parameterMapping; }
	public BoundSql getBoundSql() { return boundSql; }
	public Object getValue() { return value; }
	public boolean isBoundSqlParameter() { return parameterMapping != null; }
}
