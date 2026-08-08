package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AnalyzedStatement {
	public enum JdbcResultRequirement {
		OPTIONAL,
		RESULT_SET,
		RETURNING,
		UPDATE_COUNT
	}

	private final int statementIndex;
	private final boolean auditable;
	private final JdbcResultRequirement jdbcResultRequirement;
	private final List<ChangeTarget> targets;

	public AnalyzedStatement(int statementIndex, boolean auditable, List<ChangeTarget> targets) {
		this(statementIndex, auditable, JdbcResultRequirement.UPDATE_COUNT, targets);
	}

	private AnalyzedStatement(int statementIndex, boolean auditable,
			JdbcResultRequirement jdbcResultRequirement,
			List<ChangeTarget> targets) {
		this.statementIndex = statementIndex;
		this.auditable = auditable;
		this.jdbcResultRequirement = jdbcResultRequirement;
		this.targets = Collections.unmodifiableList(new ArrayList<ChangeTarget>(
				targets == null ? Collections.<ChangeTarget>emptyList() : targets));
	}

	public static AnalyzedStatement nonAuditable(int statementIndex) {
		return nonAuditable(statementIndex, JdbcResultRequirement.UPDATE_COUNT);
	}

	public static AnalyzedStatement nonAuditable(int statementIndex,
			JdbcResultRequirement jdbcResultRequirement) {
		return new AnalyzedStatement(statementIndex, false, jdbcResultRequirement,
				Collections.<ChangeTarget>emptyList());
	}

	public static AnalyzedStatement auditable(int statementIndex,
			JdbcResultRequirement jdbcResultRequirement, List<ChangeTarget> targets) {
		return new AnalyzedStatement(statementIndex, true, jdbcResultRequirement, targets);
	}

	public int getStatementIndex() { return statementIndex; }
	public boolean isAuditable() { return auditable; }
	public JdbcResultRequirement getJdbcResultRequirement() { return jdbcResultRequirement; }
	public List<ChangeTarget> getTargets() { return targets; }

	public AnalyzedStatement withTargets(List<ChangeTarget> filteredTargets) {
		return new AnalyzedStatement(statementIndex, auditable, jdbcResultRequirement, filteredTargets);
	}
}
