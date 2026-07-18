package com.vesoft.nebula.jdbc.mysql.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MysqlQueryResult {
	private final List<MysqlColumn> columns;
	private final List<List<Object>> rows;

	private MysqlQueryResult(List<MysqlColumn> columns, List<List<Object>> rows) {
		this.columns = copyColumns(columns);
		this.rows = copyRows(rows);
	}

	public static MysqlQueryResult of(List<MysqlColumn> columns, List<List<Object>> rows) {
		return new MysqlQueryResult(columns, rows);
	}

	public static MysqlQueryResult empty(List<MysqlColumn> columns) {
		return new MysqlQueryResult(columns, Collections.<List<Object>>emptyList());
	}

	public List<MysqlColumn> getColumns() {
		return columns;
	}

	public List<List<Object>> getRows() {
		return rows;
	}

	private static List<MysqlColumn> copyColumns(List<MysqlColumn> columns) {
		if (columns == null) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new ArrayList<MysqlColumn>(columns));
	}

	private static List<List<Object>> copyRows(List<List<Object>> rows) {
		if (rows == null) {
			return Collections.emptyList();
		}
		List<List<Object>> copiedRows = new ArrayList<List<Object>>(rows.size());
		for (List<Object> row : rows) {
			copiedRows.add(row == null
					? Collections.<Object>emptyList()
					: Collections.unmodifiableList(new ArrayList<Object>(row)));
		}
		return Collections.unmodifiableList(copiedRows);
	}
}
