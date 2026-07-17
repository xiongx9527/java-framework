package com.vesoft.nebula.jdbc.sql.parser.ast;

import com.vesoft.nebula.jdbc.sql.parser.StatementKind;

public class NebulaDeleteStatement implements NebulaSqlStatement {

	private final NebulaTableSource table;
	private final NebulaExpression where;

	public NebulaDeleteStatement(NebulaTableSource table, NebulaExpression where) {
		this.table = table;
		this.where = where;
	}

	public StatementKind getKind() {
		return StatementKind.DELETE;
	}

	public NebulaTableSource getTable() {
		return table;
	}

	public NebulaExpression getWhere() {
		return where;
	}
}
