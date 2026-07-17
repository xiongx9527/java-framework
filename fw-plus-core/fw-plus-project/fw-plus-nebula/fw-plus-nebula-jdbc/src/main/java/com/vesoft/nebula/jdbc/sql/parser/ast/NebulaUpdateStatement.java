package com.vesoft.nebula.jdbc.sql.parser.ast;

import com.vesoft.nebula.jdbc.sql.parser.StatementKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NebulaUpdateStatement implements NebulaSqlStatement {

	private final NebulaTableSource table;
	private final List<NebulaUpdateItem> items;
	private final NebulaExpression where;

	public NebulaUpdateStatement(NebulaTableSource table, List<NebulaUpdateItem> items, NebulaExpression where) {
		this.table = table;
		this.items = Collections.unmodifiableList(new ArrayList<NebulaUpdateItem>(items));
		this.where = where;
	}

	public StatementKind getKind() {
		return StatementKind.UPDATE;
	}

	public NebulaTableSource getTable() {
		return table;
	}

	public List<NebulaUpdateItem> getItems() {
		return items;
	}

	public NebulaExpression getWhere() {
		return where;
	}
}
