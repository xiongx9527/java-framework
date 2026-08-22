package com.xiongx9527.fw.plus.mybatis.datachange.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TableChange {
	private final TableIdentifier table;
	private final List<RowChange> rows;

	public TableChange(TableIdentifier table, List<RowChange> rows) {
		this.table = table;
		this.rows = rows == null ? new ArrayList<RowChange>() : new ArrayList<RowChange>(rows);
	}

	public TableIdentifier getTable() { return table; }
	public List<RowChange> getRows() { return Collections.unmodifiableList(new ArrayList<RowChange>(rows)); }
}
