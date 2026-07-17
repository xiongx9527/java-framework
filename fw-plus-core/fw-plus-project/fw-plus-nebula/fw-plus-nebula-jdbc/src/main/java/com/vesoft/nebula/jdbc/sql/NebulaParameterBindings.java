package com.vesoft.nebula.jdbc.sql;

import java.sql.SQLException;

public class NebulaParameterBindings {
	private static final NebulaParameterBindings EMPTY = new NebulaParameterBindings(0);

	private final int size;

	public NebulaParameterBindings(int size) {
		if (size < 0) {
			throw new IllegalArgumentException("size must be >= 0");
		}
		this.size = size;
	}

	public static NebulaParameterBindings empty() {
		return EMPTY;
	}

	public int size() {
		return size;
	}

	public void validateComplete() throws SQLException {
	}
}
