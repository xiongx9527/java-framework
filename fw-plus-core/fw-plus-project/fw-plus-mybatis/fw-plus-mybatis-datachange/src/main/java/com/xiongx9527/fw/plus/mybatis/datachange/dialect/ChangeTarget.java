package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import org.apache.ibatis.mapping.ParameterMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChangeTarget {
	private final StatementOperation operation;
	private final TableIdentifier table;
	private final Table sourceTable;
	private final Expression where;
	private final List<Join> joins;
	private final List<AssignedColumn> assignedColumns;
	private final List<ParameterMapping> parameterMappings;
	private final List<ParameterMapping> predicateParameterMappings;
	private final List<String> keyColumnsHint;
	private final List<String> monitoredColumns;
	private final int maxRows;
	private final boolean lockRows;

	public ChangeTarget(StatementOperation operation, TableIdentifier table, Table sourceTable, Expression where,
						List<Join> joins, List<AssignedColumn> assignedColumns,
						List<ParameterMapping> parameterMappings, List<ParameterMapping> predicateParameterMappings,
						List<String> keyColumnsHint) {
		this(operation, table, sourceTable, where, joins, assignedColumns, parameterMappings,
				predicateParameterMappings, keyColumnsHint, Collections.<String>emptyList(), 1000, false);
	}

	private ChangeTarget(StatementOperation operation, TableIdentifier table, Table sourceTable, Expression where,
						 List<Join> joins, List<AssignedColumn> assignedColumns,
						 List<ParameterMapping> parameterMappings, List<ParameterMapping> predicateParameterMappings,
						 List<String> keyColumnsHint, List<String> monitoredColumns, int maxRows, boolean lockRows) {
		this.operation = operation;
		this.table = table;
		this.sourceTable = sourceTable;
		this.where = where;
		this.joins = immutable(joins);
		this.assignedColumns = immutable(assignedColumns);
		this.parameterMappings = immutable(parameterMappings);
		this.predicateParameterMappings = immutable(predicateParameterMappings);
		this.keyColumnsHint = immutable(keyColumnsHint);
		this.monitoredColumns = immutable(monitoredColumns);
		this.maxRows = maxRows;
		this.lockRows = lockRows;
	}

	public StatementOperation getOperation() { return operation; }
	public TableIdentifier getTable() { return table; }
	public Table getSourceTable() { return sourceTable; }
	public Expression getWhere() { return where; }
	public List<Join> getJoins() { return joins; }
	public List<AssignedColumn> getAssignedColumns() { return assignedColumns; }
	public List<ParameterMapping> getParameterMappings() { return parameterMappings; }
	public List<ParameterMapping> getPredicateParameterMappings() { return predicateParameterMappings; }
	public List<String> getKeyColumnsHint() { return keyColumnsHint; }
	public List<String> getMonitoredColumns() { return monitoredColumns; }
	public int getMaxRows() { return maxRows; }
	public boolean isLockRows() { return lockRows; }

	public ChangeTarget withSnapshotConfiguration(List<String> keyColumns, List<String> columns,
												  int configuredMaxRows, boolean configuredLockRows) {
		if (configuredMaxRows <= 0) {
			throw new IllegalArgumentException("maxRows must be greater than zero");
		}
		return new ChangeTarget(operation, table, sourceTable, where, joins, assignedColumns, parameterMappings,
				predicateParameterMappings, keyColumns, columns, configuredMaxRows, configuredLockRows);
	}

	private <T> List<T> immutable(List<T> values) {
		return Collections.unmodifiableList(new ArrayList<T>(values == null ? Collections.<T>emptyList() : values));
	}
}
