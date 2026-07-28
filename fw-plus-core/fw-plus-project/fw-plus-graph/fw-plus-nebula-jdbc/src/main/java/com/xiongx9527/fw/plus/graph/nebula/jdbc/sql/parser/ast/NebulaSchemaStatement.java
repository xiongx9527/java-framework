package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.ast;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.StatementKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NebulaSchemaStatement implements NebulaSqlStatement {

	private final StatementKind kind;
	private final String tableName;
	private final List<NebulaColumnDefinition> columns;
	private final List<String> unsupportedOptions;

	public NebulaSchemaStatement(StatementKind kind, String tableName, List<NebulaColumnDefinition> columns) {
		this(kind, tableName, columns, null);
	}

	public NebulaSchemaStatement(StatementKind kind, String tableName, List<NebulaColumnDefinition> columns,
			List<String> unsupportedOptions) {
		this.kind = kind;
		this.tableName = tableName;
		this.columns = columns == null ? Collections.<NebulaColumnDefinition>emptyList()
				: Collections.unmodifiableList(new ArrayList<NebulaColumnDefinition>(columns));
		this.unsupportedOptions = unsupportedOptions == null ? Collections.<String>emptyList()
				: Collections.unmodifiableList(new ArrayList<String>(unsupportedOptions));
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

	public List<String> getUnsupportedOptions() {
		return unsupportedOptions;
	}
}
