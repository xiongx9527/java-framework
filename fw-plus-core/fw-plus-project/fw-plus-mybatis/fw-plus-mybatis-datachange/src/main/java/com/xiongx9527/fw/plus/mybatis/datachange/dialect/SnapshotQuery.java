package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SnapshotQuery {
	private final String sql;
	private final List<SnapshotParameter> parameters;
	private final int maxRows;

	public SnapshotQuery(String sql, List<SnapshotParameter> parameters) {
		this(sql, parameters, -1);
	}

	public SnapshotQuery(String sql, List<SnapshotParameter> parameters, int maxRows) {
		this.sql = sql;
		this.parameters = Collections.unmodifiableList(new ArrayList<SnapshotParameter>(
				parameters == null ? Collections.<SnapshotParameter>emptyList() : parameters));
		this.maxRows = maxRows;
	}

	public String getSql() { return sql; }
	public List<SnapshotParameter> getParameters() { return parameters; }
	public int getMaxRows() { return maxRows; }
}
