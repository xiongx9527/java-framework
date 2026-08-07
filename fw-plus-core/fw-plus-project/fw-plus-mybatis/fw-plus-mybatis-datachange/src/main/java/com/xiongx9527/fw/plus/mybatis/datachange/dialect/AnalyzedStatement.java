package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AnalyzedStatement {
	private final int statementIndex;
	private final boolean auditable;
	private final List<ChangeTarget> targets;

	public AnalyzedStatement(int statementIndex, boolean auditable, List<ChangeTarget> targets) {
		this.statementIndex = statementIndex;
		this.auditable = auditable;
		this.targets = Collections.unmodifiableList(new ArrayList<ChangeTarget>(
				targets == null ? Collections.<ChangeTarget>emptyList() : targets));
	}

	public static AnalyzedStatement nonAuditable(int statementIndex) {
		return new AnalyzedStatement(statementIndex, false, Collections.<ChangeTarget>emptyList());
	}

	public int getStatementIndex() { return statementIndex; }
	public boolean isAuditable() { return auditable; }
	public List<ChangeTarget> getTargets() { return targets; }

	public AnalyzedStatement withTargets(List<ChangeTarget> filteredTargets) {
		return new AnalyzedStatement(statementIndex, auditable, filteredTargets);
	}
}
