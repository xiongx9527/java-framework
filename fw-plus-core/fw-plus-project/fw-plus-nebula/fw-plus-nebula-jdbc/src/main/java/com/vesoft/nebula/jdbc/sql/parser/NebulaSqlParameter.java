package com.vesoft.nebula.jdbc.sql.parser;

public class NebulaSqlParameter {

	private final int index;

	public NebulaSqlParameter(int index) {
		this.index = index;
	}

	public int getIndex() {
		return index;
	}
}
