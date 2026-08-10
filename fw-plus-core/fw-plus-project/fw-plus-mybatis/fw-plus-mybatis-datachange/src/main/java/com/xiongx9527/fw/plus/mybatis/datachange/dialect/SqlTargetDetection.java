package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitorRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SqlTargetDetection {
	private final List<StatementDetection> statements;

	public SqlTargetDetection(List<StatementDetection> statements) {
		this.statements = Collections.unmodifiableList(new ArrayList<StatementDetection>(statements));
	}

	public List<StatementDetection> getStatements() { return statements; }

	public boolean mightMatch(TableMonitorRegistry registry) {
		if (registry == null) {
			return false;
		}
		for (StatementDetection statement : statements) {
			if (!statement.isAuditable()) {
				continue;
			}
			for (TableIdentifier table : statement.getTargetTables()) {
				if (registry.mightMatch(table)) {
					return true;
				}
			}
		}
		return false;
	}

	public static final class StatementDetection {
		private final int statementIndex;
		private final boolean auditable;
		private final boolean targetCertain;
		private final StatementOperation operation;
		private final List<TableIdentifier> targetTables;

		public StatementDetection(int statementIndex, boolean auditable, boolean targetCertain,
								  StatementOperation operation, List<TableIdentifier> targetTables) {
			this.statementIndex = statementIndex;
			this.auditable = auditable;
			this.targetCertain = targetCertain;
			this.operation = operation;
			this.targetTables = Collections.unmodifiableList(new ArrayList<TableIdentifier>(
					targetTables == null ? Collections.<TableIdentifier>emptyList() : targetTables));
		}

		public int getStatementIndex() { return statementIndex; }
		public boolean isAuditable() { return auditable; }
		public boolean isTargetCertain() { return targetCertain; }
		public StatementOperation getOperation() { return operation; }
		public List<TableIdentifier> getTargetTables() { return targetTables; }
	}
}
