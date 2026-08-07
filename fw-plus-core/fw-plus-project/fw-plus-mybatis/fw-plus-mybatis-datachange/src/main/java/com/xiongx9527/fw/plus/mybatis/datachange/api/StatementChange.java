package com.xiongx9527.fw.plus.mybatis.datachange.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class StatementChange {
	private final String statementId;
	private final int statementIndex;
	private final int affectedRows;
	private final List<TableChange> tables;

	public StatementChange(String statementId, List<TableChange> tables) {
		this(statementId, 0, -1, tables);
	}

	public StatementChange(String statementId, int statementIndex, int affectedRows, List<TableChange> tables) {
		this.statementId = statementId;
		this.statementIndex = statementIndex;
		this.affectedRows = affectedRows;
		this.tables = tables == null ? new ArrayList<TableChange>() : new ArrayList<TableChange>(tables);
	}

	public String getStatementId() { return statementId; }
	public int getStatementIndex() { return statementIndex; }
	public int getAffectedRows() { return affectedRows; }
	public List<TableChange> getTables() { return Collections.unmodifiableList(new ArrayList<TableChange>(tables)); }
}
