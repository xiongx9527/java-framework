package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.Join;
import org.apache.ibatis.mapping.ParameterMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

abstract class AbstractStandardDataChangeDialect extends AbstractJSqlParserDataChangeDialect {
	@Override
	public TableIdentifier normalize(TableIdentifier table) {
		return table == null ? null : new TableIdentifier(table.getCatalog(), table.getSchema(), table.getTable());
	}

	@Override
	public SnapshotQuery buildBeforeQuery(ChangeTarget target) {
		if (target.getOperation() == StatementOperation.INSERT) {
			if (target.isUpsert()) { return buildAssignedIdentityQuery(target, true); }
			throw snapshotFailure("An INSERT statement has no before snapshot");
		}
		StringBuilder sql = selectPrefix(target, snapshotColumns(target), !target.getJoins().isEmpty(),
				target.isLockRows());
		for (Join join : target.getJoins()) { sql.append(' ').append(join.toString().trim()); }
		boolean hasWhere = target.getWhere() != null;
		if (hasWhere) { sql.append(" WHERE (").append(target.getWhere()).append(')'); }
		appendLimit(sql, target, hasWhere);
		if (target.isLockRows()) { appendRowLock(sql); }
		List<SnapshotParameter> parameters = new ArrayList<SnapshotParameter>();
		for (ParameterMapping mapping : target.getPredicateParameterMappings()) {
			parameters.add(SnapshotParameter.boundSql(mapping, target.getBoundSql()));
		}
		return new SnapshotQuery(sql.toString(), parameters, queryLimit(target));
	}

	@Override
	public SnapshotQuery buildAfterQuery(ChangeTarget target, List<RowIdentity> identities) {
		if (target.isUpsert()) { return buildAssignedIdentityQuery(target, false); }
		if (target.getOperation() == StatementOperation.MERGE) { return buildMergeMatchQuery(target); }
		List<String> keys = target.getKeyColumnsHint();
		if (keys.isEmpty()) { throw snapshotFailure("A unique key is required for an after snapshot"); }
		StringBuilder sql = selectPrefix(target, snapshotColumns(target), false, false);
		List<SnapshotParameter> parameters = new ArrayList<SnapshotParameter>();
		sql.append(" WHERE ");
		if (identities == null || identities.isEmpty()) {
			sql.append("1 = 0");
		} else {
			sql.append('(');
			for (int row = 0; row < identities.size(); row++) {
				if (row > 0) { sql.append(" OR "); }
				sql.append('(');
				Map<String, Object> values = identities.get(row).getKeyValues();
				for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
					if (keyIndex > 0) { sql.append(" AND "); }
					String key = keys.get(keyIndex);
					sql.append(quote(key)).append(" = ?");
					parameters.add(SnapshotParameter.value(identityValue(values, key)));
				}
				sql.append(')');
			}
			sql.append(')');
		}
		appendLimit(sql, target, true);
		return new SnapshotQuery(sql.toString(), parameters, queryLimit(target));
	}

	@Override
	public int getOrder() { return 100; }

	protected int queryLimit(ChangeTarget target) {
		long limit = (long) target.getMaxRows() + 1L;
		return limit > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) limit;
	}

	protected String selectKeyword(ChangeTarget target) { return "SELECT "; }

	protected void appendTableLock(StringBuilder sql, boolean lockRows) { }

	protected void appendLimit(StringBuilder sql, ChangeTarget target, boolean hasWhere) {
		sql.append(" LIMIT ").append(queryLimit(target));
	}

	protected void appendRowLock(StringBuilder sql) { sql.append(" FOR UPDATE"); }

	protected abstract String quote(String identifier);

	private SnapshotQuery buildAssignedIdentityQuery(ChangeTarget target, boolean lockRows) {
		List<String> keys = target.getKeyColumnsHint();
		if (keys.isEmpty()) { throw identityFailure(); }
		StringBuilder sql = selectPrefix(target, snapshotColumns(target), false, lockRows);
		List<SnapshotParameter> parameters = new ArrayList<SnapshotParameter>();
		sql.append(" WHERE ");
		for (int index = 0; index < keys.size(); index++) {
			if (index > 0) { sql.append(" AND "); }
			String key = keys.get(index);
			AssignedColumn assigned = assigned(target, key);
			if (assigned == null || assigned.getExpression() == null
					|| parameterCount(assigned.getExpression()) == 0
					|| parameterCount(assigned.getExpression()) != assigned.getParameterMappings().size()) {
				throw identityFailure();
			}
			sql.append(quote(key)).append(" = (").append(assigned.getExpression()).append(')');
			for (ParameterMapping mapping : assigned.getParameterMappings()) {
				parameters.add(SnapshotParameter.boundSql(mapping, target.getBoundSql()));
			}
		}
		appendLimit(sql, target, true);
		if (lockRows) { appendRowLock(sql); }
		return new SnapshotQuery(sql.toString(), parameters, queryLimit(target));
	}

	private SnapshotQuery buildMergeMatchQuery(ChangeTarget target) {
		StringBuilder sql = selectPrefix(target, snapshotColumns(target), true, false);
		for (Join join : target.getJoins()) { sql.append(' ').append(join.toString().trim()); }
		appendLimit(sql, target, false);
		List<SnapshotParameter> parameters = new ArrayList<SnapshotParameter>();
		for (ParameterMapping mapping : target.getMatchParameterMappings()) {
			parameters.add(SnapshotParameter.boundSql(mapping, target.getBoundSql()));
		}
		return new SnapshotQuery(sql.toString(), parameters, queryLimit(target));
	}

	private AssignedColumn assigned(ChangeTarget target, String key) {
		for (AssignedColumn assigned : target.getAssignedColumns()) {
			if (assigned.getColumn() != null && assigned.getColumn().equalsIgnoreCase(key)) { return assigned; }
		}
		return null;
	}

	private int parameterCount(Expression expression) {
		final int[] count = new int[]{0};
		expression.accept(new ExpressionVisitorAdapter() {
			@Override
			public void visit(JdbcParameter parameter) { count[0]++; }
		});
		return count[0];
	}

	private StringBuilder selectPrefix(ChangeTarget target, List<String> columns, boolean qualifyColumns,
			boolean lockRows) {
		StringBuilder sql = new StringBuilder(selectKeyword(target));
		String qualifier = qualifyColumns ? projectionQualifier(target) : null;
		for (int index = 0; index < columns.size(); index++) {
			if (index > 0) { sql.append(", "); }
			if (qualifier != null) { sql.append(qualifier).append('.'); }
			sql.append(quote(columns.get(index)));
		}
		sql.append(" FROM ").append(qualifiedTable(target.getSnapshotSourceTable(), target.getTable()));
		Alias alias = target.getSnapshotSourceTable() == null ? null : target.getSnapshotSourceTable().getAlias();
		if (alias != null) { sql.append(alias.isUseAs() ? " AS " : " ").append(alias.getName()); }
		appendTableLock(sql, lockRows);
		return sql;
	}

	private String projectionQualifier(ChangeTarget target) {
		Alias alias = target.getSourceTable() == null ? null : target.getSourceTable().getAlias();
		return alias == null ? qualifiedTable(target.getSourceTable(), target.getTable()) : alias.getName();
	}

	private List<String> snapshotColumns(ChangeTarget target) {
		Map<String, String> columns = new LinkedHashMap<String, String>();
		addColumns(columns, target.getKeyColumnsHint());
		addColumns(columns, target.getMonitoredColumns());
		if (columns.isEmpty()) { throw snapshotFailure("Snapshot columns must not be empty"); }
		return new ArrayList<String>(columns.values());
	}

	private void addColumns(Map<String, String> columns, List<String> additions) {
		for (String column : additions) {
			String key = column.toLowerCase(Locale.ENGLISH);
			if (!columns.containsKey(key)) { columns.put(key, column); }
		}
	}

	private String qualifiedTable(Table source, TableIdentifier fallback) {
		StringBuilder name = new StringBuilder();
		if (source != null && source.getNameParts() != null && !source.getNameParts().isEmpty()) {
			List<String> parts = source.getNameParts();
			for (int index = parts.size() - 1; index >= 0; index--) { appendPart(name, parts.get(index)); }
			return name.toString();
		}
		appendPart(name, fallback.getCatalog());
		appendPart(name, fallback.getSchema());
		appendPart(name, fallback.getTable());
		return name.toString();
	}

	private void appendPart(StringBuilder name, String part) {
		if (part == null || part.isEmpty()) { return; }
		if (name.length() > 0) { name.append('.'); }
		name.append(quote(part));
	}

	private Object identityValue(Map<String, Object> values, String key) {
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) { return entry.getValue(); }
		}
		throw snapshotFailure("Row identity does not contain every unique key column");
	}

	protected DataChangeException snapshotFailure(String message) {
		return new DataChangeException(DataChangeErrorCode.SNAPSHOT_QUERY_FAILED, message);
	}

	private DataChangeException identityFailure() {
		return new DataChangeException(DataChangeErrorCode.ROW_IDENTITY_UNAVAILABLE,
				"ROW_IDENTITY_UNAVAILABLE");
	}
}
