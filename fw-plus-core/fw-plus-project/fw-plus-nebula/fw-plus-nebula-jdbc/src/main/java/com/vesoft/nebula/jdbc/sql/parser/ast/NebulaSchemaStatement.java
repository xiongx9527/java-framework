package com.vesoft.nebula.jdbc.sql.parser.ast;

import com.vesoft.nebula.jdbc.sql.parser.StatementKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NebulaSchemaStatement implements NebulaSqlStatement {

	private final StatementKind kind;
	private final String tableName;
	private final List<NebulaColumnDefinition> columns;

	public NebulaSchemaStatement(StatementKind kind, String tableName, List<NebulaColumnDefinition> columns) {
		this.kind = kind;
		this.tableName = tableName;
		this.columns = columns == null ? Collections.<NebulaColumnDefinition>emptyList()
				: Collections.unmodifiableList(new ArrayList<NebulaColumnDefinition>(columns));
	}

	public StatementKind getKind() {
		return kind;
	}

	public String getTableName() {
		return tableName;
	}

	public List<NebulaColumnDefinition> getColumns() {
		return columns;
	}
}
