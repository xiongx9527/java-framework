package com.vesoft.nebula.jdbc.sql.parser.ast;

import com.vesoft.nebula.jdbc.sql.parser.StatementKind;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class NebulaInsertStatement implements NebulaSqlStatement {

	private final String tableName;
	private final List<String> columns;
	private final List<List<NebulaExpression>> values;
	private final NebulaSelectStatement selectSource;

	public NebulaInsertStatement(String tableName, List<String> columns, List<List<NebulaExpression>> values,
								 NebulaSelectStatement selectSource) {
		this.tableName = tableName;
		this.columns = Collections.unmodifiableList(new ArrayList<String>(columns));
		List<List<NebulaExpression>> rows = new ArrayList<List<NebulaExpression>>();
		for (List<NebulaExpression> row : values) {
			rows.add(Collections.unmodifiableList(new ArrayList<NebulaExpression>(row)));
		}
		this.values = Collections.unmodifiableList(rows);
		this.selectSource = selectSource;
	}

	public StatementKind getKind() {
		return StatementKind.INSERT;
	}

	public String getTableName() {
		return tableName;
	}

	public List<String> getColumns() {
		return columns;
	}

	public List<List<NebulaExpression>> getValues() {
		return values;
	}

	public NebulaSelectStatement getSelectSource() {
		return selectSource;
	}
}
