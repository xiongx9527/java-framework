package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.BoundSql;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChangeTarget {
	private final StatementOperation operation;
	private final TableIdentifier table;
	private final Table sourceTable;
	private final Table snapshotSourceTable;
	private final Expression where;
	private final List<Join> joins;
	private final List<AssignedColumn> assignedColumns;
	private final List<ParameterMapping> parameterMappings;
	private final List<ParameterMapping> predicateParameterMappings;
	private final List<ParameterMapping> matchParameterMappings;
	private final List<String> keyColumnsHint;
	private final List<String> monitoredColumns;
	private final int maxRows;
	private final boolean lockRows;
	private final BoundSql boundSql;
	private final boolean upsert;
	private final boolean returnsRows;
	private final ConflictTarget conflictTarget;

	public ChangeTarget(StatementOperation operation, TableIdentifier table, Table sourceTable, Expression where,
						List<Join> joins, List<AssignedColumn> assignedColumns,
						List<ParameterMapping> parameterMappings, List<ParameterMapping> predicateParameterMappings,
						List<String> keyColumnsHint) {
		this(operation, table, sourceTable, where, joins, assignedColumns, parameterMappings,
					predicateParameterMappings, keyColumnsHint, Collections.<String>emptyList(), 1000, false, null,
					sourceTable, false, ConflictTarget.none());
	}

	ChangeTarget(StatementOperation operation, TableIdentifier table, Table sourceTable, Expression where,
			List<Join> joins, List<AssignedColumn> assignedColumns,
			List<ParameterMapping> parameterMappings, List<ParameterMapping> predicateParameterMappings,
			List<String> keyColumnsHint, BoundSql boundSql) {
		this(operation, table, sourceTable, where, joins, assignedColumns, parameterMappings,
					predicateParameterMappings, keyColumnsHint, Collections.<String>emptyList(), 1000, false, boundSql,
					sourceTable, false, ConflictTarget.none());
	}

	ChangeTarget(StatementOperation operation, TableIdentifier table, Table sourceTable, Expression where,
			List<Join> joins, List<AssignedColumn> assignedColumns,
			List<ParameterMapping> parameterMappings, List<ParameterMapping> predicateParameterMappings,
			List<String> keyColumnsHint, BoundSql boundSql, Table snapshotSourceTable) {
		this(operation, table, sourceTable, where, joins, assignedColumns, parameterMappings,
				predicateParameterMappings, keyColumnsHint, Collections.<String>emptyList(), 1000, false, boundSql,
						snapshotSourceTable, false, ConflictTarget.none());
	}

	ChangeTarget(StatementOperation operation, TableIdentifier table, Table sourceTable, Expression where,
			List<Join> joins, List<AssignedColumn> assignedColumns,
			List<ParameterMapping> parameterMappings, List<ParameterMapping> predicateParameterMappings,
			List<String> keyColumnsHint, BoundSql boundSql, Table snapshotSourceTable,
			boolean upsert, List<String> conflictColumnsHint) {
		this(operation, table, sourceTable, where, joins, assignedColumns, parameterMappings,
				predicateParameterMappings, keyColumnsHint, boundSql, snapshotSourceTable, upsert,
				conflictColumnsHint == null || conflictColumnsHint.isEmpty() ? ConflictTarget.none()
						: ConflictTarget.columns(conflictColumnsHint));
	}

	ChangeTarget(StatementOperation operation, TableIdentifier table, Table sourceTable, Expression where,
			List<Join> joins, List<AssignedColumn> assignedColumns,
			List<ParameterMapping> parameterMappings, List<ParameterMapping> predicateParameterMappings,
			List<String> keyColumnsHint, BoundSql boundSql, Table snapshotSourceTable,
			boolean upsert, ConflictTarget conflictTarget) {
		this(operation, table, sourceTable, where, joins, assignedColumns, parameterMappings,
				predicateParameterMappings, keyColumnsHint, Collections.<String>emptyList(), 1000, false, boundSql,
				snapshotSourceTable, upsert, conflictTarget);
	}

	private ChangeTarget(StatementOperation operation, TableIdentifier table, Table sourceTable, Expression where,
						 List<Join> joins, List<AssignedColumn> assignedColumns,
						 List<ParameterMapping> parameterMappings, List<ParameterMapping> predicateParameterMappings,
						 List<String> keyColumnsHint, List<String> monitoredColumns, int maxRows, boolean lockRows,
								 BoundSql boundSql, Table snapshotSourceTable, boolean upsert,
								 ConflictTarget conflictTarget) {
		this(operation, table, sourceTable, where, joins, assignedColumns, parameterMappings,
				predicateParameterMappings, Collections.<ParameterMapping>emptyList(), keyColumnsHint, monitoredColumns,
				maxRows, lockRows, boundSql, snapshotSourceTable, upsert, false, conflictTarget);
	}

	private ChangeTarget(StatementOperation operation, TableIdentifier table, Table sourceTable, Expression where,
			List<Join> joins, List<AssignedColumn> assignedColumns,
			List<ParameterMapping> parameterMappings, List<ParameterMapping> predicateParameterMappings,
			List<ParameterMapping> matchParameterMappings, List<String> keyColumnsHint,
			List<String> monitoredColumns, int maxRows, boolean lockRows, BoundSql boundSql,
			Table snapshotSourceTable, boolean upsert, boolean returnsRows, ConflictTarget conflictTarget) {
		this.operation = operation;
		this.table = table;
		this.sourceTable = sourceTable;
		this.snapshotSourceTable = snapshotSourceTable;
		this.where = where;
		this.joins = immutable(joins);
		this.assignedColumns = immutable(assignedColumns);
		this.parameterMappings = immutable(parameterMappings);
		this.predicateParameterMappings = immutable(predicateParameterMappings);
		this.matchParameterMappings = immutable(matchParameterMappings);
		this.keyColumnsHint = immutable(keyColumnsHint);
		this.monitoredColumns = immutable(monitoredColumns);
		this.maxRows = maxRows;
		this.lockRows = lockRows;
		this.boundSql = boundSql;
		this.upsert = upsert;
		this.returnsRows = returnsRows;
		this.conflictTarget = conflictTarget == null ? ConflictTarget.none() : conflictTarget;
	}

	public StatementOperation getOperation() { return operation; }
	public TableIdentifier getTable() { return table; }
	public Table getSourceTable() { return sourceTable; }
	public Table getSnapshotSourceTable() { return snapshotSourceTable; }
	public Expression getWhere() { return where; }
	public List<Join> getJoins() { return joins; }
	public List<AssignedColumn> getAssignedColumns() { return assignedColumns; }
	public List<ParameterMapping> getParameterMappings() { return parameterMappings; }
	public List<ParameterMapping> getPredicateParameterMappings() { return predicateParameterMappings; }
	public List<ParameterMapping> getMatchParameterMappings() { return matchParameterMappings; }
	public List<String> getKeyColumnsHint() { return keyColumnsHint; }
	public List<String> getMonitoredColumns() { return monitoredColumns; }
	public int getMaxRows() { return maxRows; }
	public boolean isLockRows() { return lockRows; }
	public BoundSql getBoundSql() { return boundSql; }
	public boolean isUpsert() { return upsert; }
	public boolean isReturnsRows() { return returnsRows; }
	public ConflictTarget getConflictTarget() { return conflictTarget; }
	public List<String> getConflictColumnsHint() { return conflictTarget.getColumns(); }

	ChangeTarget withMatchParameterMappings(List<ParameterMapping> mappings) {
		return new ChangeTarget(operation, table, sourceTable, where, joins, assignedColumns, parameterMappings,
				predicateParameterMappings, mappings, keyColumnsHint, monitoredColumns, maxRows, lockRows, boundSql,
				snapshotSourceTable, upsert, returnsRows, conflictTarget);
	}

	ChangeTarget withReturnsRows(boolean value) {
		return new ChangeTarget(operation, table, sourceTable, where, joins, assignedColumns, parameterMappings,
				predicateParameterMappings, matchParameterMappings, keyColumnsHint, monitoredColumns, maxRows, lockRows,
				boundSql, snapshotSourceTable, upsert, value, conflictTarget);
	}

	public ChangeTarget withSnapshotConfiguration(List<String> keyColumns, List<String> columns,
												  int configuredMaxRows, boolean configuredLockRows) {
		if (configuredMaxRows <= 0) {
			throw new IllegalArgumentException("maxRows must be greater than zero");
		}
		return new ChangeTarget(operation, table, sourceTable, where, joins, assignedColumns, parameterMappings,
				predicateParameterMappings, matchParameterMappings, keyColumns, columns, configuredMaxRows,
				configuredLockRows, boundSql, snapshotSourceTable, upsert, returnsRows, conflictTarget);
	}

	private <T> List<T> immutable(List<T> values) {
		return Collections.unmodifiableList(new ArrayList<T>(values == null ? Collections.<T>emptyList() : values));
	}
}
