package com.vesoft.nebula.jdbc.sql.parser.ast;

public class NebulaLimit {

	private final Integer offset;
	private final Integer rowCount;

	public NebulaLimit(Integer offset, Integer rowCount) {
		this.offset = offset;
		this.rowCount = rowCount;
	}

	public Integer getOffset() {
		return offset;
	}

	public Integer getRowCount() {
		return rowCount;
	}
}
