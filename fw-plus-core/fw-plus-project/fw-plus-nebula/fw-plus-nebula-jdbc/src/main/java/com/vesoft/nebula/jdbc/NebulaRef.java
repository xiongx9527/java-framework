package com.vesoft.nebula.jdbc;

import java.sql.Ref;
import java.sql.SQLException;
import java.util.Map;

public class NebulaRef implements Ref {
	private final String baseTypeName;
	private Object value;

	public NebulaRef(String baseTypeName, Object value) {
		this.baseTypeName = baseTypeName == null ? "JAVA_OBJECT" : baseTypeName;
		this.value = value;
	}

	@Override
	public String getBaseTypeName() throws SQLException {
		return baseTypeName;
	}

	@Override
	public Object getObject(Map<String, Class<?>> map) throws SQLException {
		return value;
	}

	@Override
	public Object getObject() throws SQLException {
		return value;
	}

	@Override
	public void setObject(Object value) throws SQLException {
		this.value = value;
	}
}
