package com.vesoft.nebula.jdbc.sql.query;

public class RelationPattern {
	private final String leftTable;
	private final String leftAlias;
	private final String edgeTable;
	private final String edgeAlias;
	private final String rightTable;
	private final String rightAlias;

	public RelationPattern(String leftTable, String leftAlias, String edgeTable, String edgeAlias,
						   String rightTable, String rightAlias) {
		this.leftTable = leftTable;
		this.leftAlias = leftAlias;
		this.edgeTable = edgeTable;
		this.edgeAlias = edgeAlias;
		this.rightTable = rightTable;
		this.rightAlias = rightAlias;
	}

	public String getLeftTable() {
		return leftTable;
	}

	public String getLeftAlias() {
		return leftAlias;
	}

	public String getEdgeTable() {
		return edgeTable;
	}

	public String getEdgeAlias() {
		return edgeAlias;
	}

	public String getRightTable() {
		return rightTable;
	}

	public String getRightAlias() {
		return rightAlias;
	}
}
