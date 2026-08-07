package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SnapshotQuery {
	private final String sql;
	private final List<SnapshotParameter> parameters;

	public SnapshotQuery(String sql, List<SnapshotParameter> parameters) {
		this.sql = sql;
		this.parameters = Collections.unmodifiableList(new ArrayList<SnapshotParameter>(
				parameters == null ? Collections.<SnapshotParameter>emptyList() : parameters));
	}

	public String getSql() { return sql; }
	public List<SnapshotParameter> getParameters() { return parameters; }
}
