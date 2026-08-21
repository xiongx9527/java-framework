package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.mapping.MappedStatement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class BatchChangePlan {
	private final Object executionOwner;
	private final MappedStatement mappedStatement;
	private final String sql;
	private final List<Entry> entries = new ArrayList<Entry>();

	BatchChangePlan(DataChangeExecutionContext context, List<StatementChangePlan> plans) {
		this.executionOwner = context.getExecutionOwner();
		this.mappedStatement = context.getMappedStatement();
		this.sql = context.getBatchSql();
		add(context, plans);
	}

	Object getExecutionOwner() { return executionOwner; }
	DataChangeExecutionContext getContext() { return entries.get(0).context; }
	List<Entry> getEntries() { return Collections.unmodifiableList(new ArrayList<Entry>(entries)); }

	boolean matches(DataChangeExecutionContext context) {
		return context != null && mappedStatement == context.getMappedStatement()
				&& equal(sql, context.getBatchSql());
	}

	boolean matches(BatchResult result) {
		return result != null && mappedStatement == result.getMappedStatement() && equal(sql, result.getSql());
	}

	void add(DataChangeExecutionContext context, List<StatementChangePlan> plans) {
		entries.add(new Entry(context, plans));
	}

	private boolean equal(String first, String second) {
		return first == null ? second == null : first.equals(second);
	}

	static final class Entry {
		private final DataChangeExecutionContext context;
		private final List<StatementChangePlan> plans;

		private Entry(DataChangeExecutionContext context, List<StatementChangePlan> plans) {
			this.context = context;
			this.plans = Collections.unmodifiableList(new ArrayList<StatementChangePlan>(plans));
		}

		DataChangeExecutionContext getContext() { return context; }
		List<StatementChangePlan> getPlans() { return plans; }
	}
}
