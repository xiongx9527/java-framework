package com.xiongx9527.fw.plus.mybatis.datachange.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StatementChange {
	private final String statementId;
	private final List<TableChange> tables;

	public StatementChange(String statementId, List<TableChange> tables) {
		this.statementId = statementId;
		this.tables = tables == null ? new ArrayList<TableChange>() : new ArrayList<TableChange>(tables);
	}

	public String getStatementId() { return statementId; }
	public List<TableChange> getTables() { return Collections.unmodifiableList(new ArrayList<TableChange>(tables)); }
}
