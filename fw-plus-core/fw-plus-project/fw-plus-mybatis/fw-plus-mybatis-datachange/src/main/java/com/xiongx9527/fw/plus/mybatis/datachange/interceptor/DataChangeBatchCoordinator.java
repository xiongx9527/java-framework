package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import com.xiongx9527.fw.plus.mybatis.datachange.api.StatementChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.RowSnapshot;
import org.apache.ibatis.executor.BatchResult;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DataChangeBatchCoordinator {
	private final DataChangeEngine engine;
	private final Map<Object, List<BatchChangePlan>> pending =
			Collections.synchronizedMap(new IdentityHashMap<Object, List<BatchChangePlan>>());

	public DataChangeBatchCoordinator(DataChangeEngine engine) {
		this.engine = engine;
	}

	public void collect(DataChangeExecutionContext context, List<StatementChangePlan> plans) {
		if (context == null || plans == null || plans.isEmpty()) { return; }
		Object owner = context.getExecutionOwner();
		if (owner == null) { throw invalidState(); }
		synchronized (pending) {
			List<BatchChangePlan> groups = pending.get(owner);
			if (groups == null) {
				groups = new ArrayList<BatchChangePlan>();
				pending.put(owner, groups);
			}
			BatchChangePlan current = groups.isEmpty() ? null : groups.get(groups.size() - 1);
			if (current != null && current.matches(context)) {
				current.add(context, plans);
			} else {
				groups.add(new BatchChangePlan(context, plans));
			}
		}
	}

	public void flush(DataChangeExecutionContext context, List<BatchResult> results) {
		List<BatchChangePlan> groups = remove(context);
		if (groups.isEmpty()) { return; }
		List<StatementChange> changes = new ArrayList<StatementChange>();
		try {
			int groupIndex = 0;
			for (BatchResult result : results == null ? Collections.<BatchResult>emptyList() : results) {
				if (groupIndex >= groups.size()) { break; }
				BatchChangePlan group = groups.get(groupIndex);
				if (!group.matches(result)) { continue; }
				List<StatementChange> groupChanges = changes(group, result);
				if (hasPostProcessingFailure(group)) { return; }
				changes.addAll(groupChanges);
				groupIndex++;
			}
			if (groupIndex != groups.size()) { throw invalidState(); }
		} catch (RuntimeException failure) {
			engine.handlePostProcessingFailure(groups.get(0).getContext(), failure);
			return;
		}
		if (!changes.isEmpty()) {
			engine.dispatch(groups.get(0).getContext(), changes);
		}
	}

	public void discard(DataChangeExecutionContext context) {
		if (context == null || context.getExecutionOwner() == null) { return; }
		pending.remove(context.getExecutionOwner());
	}

	boolean hasPending(DataChangeExecutionContext context) {
		return context != null && context.getExecutionOwner() != null
				&& pending.containsKey(context.getExecutionOwner());
	}

	private List<BatchChangePlan> remove(DataChangeExecutionContext context) {
		if (context == null || context.getExecutionOwner() == null) { return Collections.emptyList(); }
		List<BatchChangePlan> groups = pending.remove(context.getExecutionOwner());
		return groups == null ? Collections.<BatchChangePlan>emptyList() : groups;
	}

	private List<StatementChange> changes(BatchChangePlan group, BatchResult result) {
		int[] counts = result.getUpdateCounts();
		List<BatchChangePlan.Entry> entries = group.getEntries();
		if (counts == null || counts.length != entries.size()) { throw invalidState(); }
		for (int count : counts) {
			if (count == Statement.EXECUTE_FAILED) { return Collections.emptyList(); }
		}
		for (int index = 0; index < entries.size(); index++) {
			BatchChangePlan.Entry entry = entries.get(index);
			engine.captureStatementResults(entry.getContext(), entry.getPlans(),
					entry.getContext().getBatchStatement(),
					Collections.singletonList(JdbcStatementResult.updateCount(counts[index])));
		}
		if (hasPostProcessingFailure(group)) { return Collections.emptyList(); }
		List<StatementChangePlan> collapsed = collapse(entries, affectedRows(counts));
		return engine.afterStatement(group.getContext(), collapsed, Collections.<JdbcStatementResult>emptyList());
	}

	private boolean hasPostProcessingFailure(BatchChangePlan group) {
		for (BatchChangePlan.Entry entry : group.getEntries()) {
			if (entry.getContext().isPostProcessingFailed()) { return true; }
		}
		return false;
	}

	private List<StatementChangePlan> collapse(List<BatchChangePlan.Entry> entries, int affectedRows) {
		Map<PlanKey, MergedPlan> merged = new LinkedHashMap<PlanKey, MergedPlan>();
		for (BatchChangePlan.Entry entry : entries) {
			for (StatementChangePlan plan : entry.getPlans()) {
				if (!successful(plan.getUpdateCount())) { continue; }
				PlanKey key = new PlanKey(plan.getStatementIndex(), plan.getTarget().getTable());
				MergedPlan accumulator = merged.get(key);
				if (accumulator == null) {
					accumulator = new MergedPlan(plan);
					merged.put(key, accumulator);
				}
				accumulator.add(plan);
			}
		}
		List<StatementChangePlan> collapsed = new ArrayList<StatementChangePlan>();
		for (MergedPlan value : merged.values()) {
			collapsed.add(value.build(affectedRows));
		}
		return collapsed;
	}

	private int affectedRows(int[] counts) {
		long total = 0L;
		for (int count : counts) {
			if (count == Statement.SUCCESS_NO_INFO) { return Statement.SUCCESS_NO_INFO; }
			if (count > 0) { total += count; }
		}
		return total > Integer.MAX_VALUE ? Statement.SUCCESS_NO_INFO : (int) total;
	}

	private boolean successful(int count) {
		return count > 0 || count == Statement.SUCCESS_NO_INFO;
	}

	private DataChangeException invalidState() {
		return new DataChangeException(DataChangeErrorCode.UNKNOWN, "DATA_CHANGE_BATCH_STATE_INVALID");
	}

	private static final class PlanKey {
		private final int statementIndex;
		private final TableIdentifier table;

		private PlanKey(int statementIndex, TableIdentifier table) {
			this.statementIndex = statementIndex;
			this.table = table;
		}

		@Override
		public boolean equals(Object value) {
			if (this == value) { return true; }
			if (!(value instanceof PlanKey)) { return false; }
			PlanKey other = (PlanKey) value;
			return statementIndex == other.statementIndex && (table == null ? other.table == null : table.equals(other.table));
		}

		@Override
		public int hashCode() { return 31 * statementIndex + (table == null ? 0 : table.hashCode()); }
	}

	private static final class MergedPlan {
		private StatementChangePlan latest;
		private final Map<RowIdentity, RowSnapshot> before = new LinkedHashMap<RowIdentity, RowSnapshot>();
		private final Set<RowIdentity> identities = new LinkedHashSet<RowIdentity>();

		private MergedPlan(StatementChangePlan first) { this.latest = first; }

		private void add(StatementChangePlan plan) {
			latest = plan;
			for (RowSnapshot row : plan.getBeforeRows()) {
				if (!before.containsKey(row.getIdentity())) { before.put(row.getIdentity(), row); }
			}
			identities.addAll(plan.getIdentities());
		}

		private StatementChangePlan build(int affectedRows) {
			StatementChangePlan plan = new StatementChangePlan(latest.getStatementIndex(),
					latest.getJdbcResultRequirements(),
					latest.getTarget(),
					latest.getMonitor(), latest.getTableMetadata(), latest.getFrameworkMetadata(), latest.getDialect(),
					new ArrayList<RowSnapshot>(before.values()));
			plan.captureResult(affectedRows, new ArrayList<RowIdentity>(identities));
			return plan;
		}
	}
}
