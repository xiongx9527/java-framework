package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query;

public class NebulaQueryBindings {
	private static final NebulaQueryBindings EMPTY = new NebulaQueryBindings();

	private NebulaQueryBindings() {
	}

	public static NebulaQueryBindings empty() {
		return EMPTY;
	}
}
