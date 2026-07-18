package com.vesoft.nebula.jdbc.mysql.query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MysqlQueryResult {
	private final List<MysqlColumn> columns;
	private final List<List<Object>> rows;
	private final boolean okResult;
	private final int affectedRows;
	private final int warnings;

	private MysqlQueryResult(List<MysqlColumn> columns, List<List<Object>> rows,
			boolean okResult, int affectedRows, int warnings) {
		this.columns = copyColumns(columns);
		this.rows = copyRows(rows);
		this.okResult = okResult;
		this.affectedRows = affectedRows;
		this.warnings = warnings;
	}

	public static MysqlQueryResult of(List<MysqlColumn> columns, List<List<Object>> rows) {
		return new MysqlQueryResult(columns, rows, false, 0, 0);
	}

	public static MysqlQueryResult empty(List<MysqlColumn> columns) {
		return new MysqlQueryResult(columns, Collections.<List<Object>>emptyList(), false, 0, 0);
	}

	public static MysqlQueryResult ok(int affectedRows, int warnings) {
		return new MysqlQueryResult(Collections.<MysqlColumn>emptyList(),
				Collections.<List<Object>>emptyList(), true, affectedRows, warnings);
	}

	public List<MysqlColumn> getColumns() {
		return columns;
	}

	public List<List<Object>> getRows() {
		return rows;
	}

	public boolean isOkResult() {
		return okResult;
	}

	public int getAffectedRows() {
		return affectedRows;
	}

	public int getWarnings() {
		return warnings;
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
