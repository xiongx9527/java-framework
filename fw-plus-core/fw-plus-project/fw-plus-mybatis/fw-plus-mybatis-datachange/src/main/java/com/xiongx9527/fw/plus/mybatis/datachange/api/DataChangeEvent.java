package com.xiongx9527.fw.plus.mybatis.datachange.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class DataChangeEvent {
	private final ConsistencyLevel consistencyLevel;
	private final List<StatementChange> statements;

	public DataChangeEvent(ConsistencyLevel consistencyLevel, List<StatementChange> statements) {
		this.consistencyLevel = consistencyLevel;
		this.statements = copy(statements);
	}

	public ConsistencyLevel getConsistencyLevel() { return consistencyLevel; }
	public List<StatementChange> getStatements() { return Collections.unmodifiableList(new ArrayList<StatementChange>(statements)); }

	private List<StatementChange> copy(List<StatementChange> values) {
		return values == null ? new ArrayList<StatementChange>() : new ArrayList<StatementChange>(values);
	}
}
