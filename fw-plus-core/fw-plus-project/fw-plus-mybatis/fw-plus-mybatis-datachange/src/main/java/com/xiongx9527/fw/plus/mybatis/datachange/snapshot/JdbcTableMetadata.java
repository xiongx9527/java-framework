package com.xiongx9527.fw.plus.mybatis.datachange.snapshot;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JdbcTableMetadata {
	private final TableIdentifier table;
	private final List<String> keyColumns;
	private final List<String> columns;

	public JdbcTableMetadata(TableIdentifier table, List<String> keyColumns, List<String> columns) {
		this.table = table;
		this.keyColumns = Collections.unmodifiableList(new ArrayList<String>(keyColumns));
		this.columns = Collections.unmodifiableList(new ArrayList<String>(columns));
	}

	public TableIdentifier getTable() { return table; }
	public List<String> getKeyColumns() { return keyColumns; }
	public List<String> getColumns() { return columns; }
}
