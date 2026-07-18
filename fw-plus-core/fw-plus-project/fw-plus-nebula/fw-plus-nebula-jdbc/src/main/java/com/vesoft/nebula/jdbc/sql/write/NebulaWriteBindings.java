package com.vesoft.nebula.jdbc.sql.write;

import com.vesoft.nebula.jdbc.sql.NebulaParameterBindings;

public class NebulaWriteBindings {
	private static final NebulaWriteBindings EMPTY = new NebulaWriteBindings(null, NebulaParameterBindings.empty());

	private final String sourceSql;
	private final NebulaParameterBindings parameterBindings;

	public NebulaWriteBindings(String sourceSql, NebulaParameterBindings parameterBindings) {
		this.sourceSql = sourceSql;
		this.parameterBindings = parameterBindings == null ? NebulaParameterBindings.empty() : parameterBindings;
	}

	public static NebulaWriteBindings empty() {
		return EMPTY;
	}

	public String getSourceSql() {
		return sourceSql;
	}

	public NebulaParameterBindings getParameterBindings() {
		return parameterBindings;
	}
}
