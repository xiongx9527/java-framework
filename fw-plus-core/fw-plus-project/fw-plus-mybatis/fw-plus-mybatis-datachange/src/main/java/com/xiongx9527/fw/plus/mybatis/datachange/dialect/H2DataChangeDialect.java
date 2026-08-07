package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.statement.select.Join;
import org.apache.ibatis.mapping.ParameterMapping;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class H2DataChangeDialect extends AbstractJSqlParserDataChangeDialect {
	@Override
	public boolean supports(DatabaseMetadata metadata) {
		return metadata != null && "h2".equals(metadata.getProductName());
	}

	@Override
	public TableIdentifier normalize(TableIdentifier table) {
		if (table == null) { return null; }
		return new TableIdentifier(table.getCatalog(), table.getSchema(), table.getTable());
	}

	@Override
	public SnapshotQuery buildBeforeQuery(ChangeTarget target) {
		if (target.getOperation() == StatementOperation.INSERT) {
			throw snapshotFailure("An INSERT statement has no before snapshot");
		}
		List<String> columns = snapshotColumns(target);
		StringBuilder sql = selectPrefix(target, columns);
		for (Join join : target.getJoins()) {
			sql.append(' ').append(join.toString().trim());
		}
		if (target.getWhere() != null) {
			sql.append(" WHERE ").append(target.getWhere());
		}
		appendLimit(sql, target);
		if (target.isLockRows()) {
			sql.append(" FOR UPDATE");
		}
		List<SnapshotParameter> parameters = new ArrayList<SnapshotParameter>();
		for (ParameterMapping mapping : target.getPredicateParameterMappings()) {
			parameters.add(SnapshotParameter.boundSql(mapping));
		}
		return new SnapshotQuery(sql.toString(), parameters);
	}

	@Override
	public SnapshotQuery buildAfterQuery(ChangeTarget target, List<RowIdentity> identities) {
		List<String> keys = target.getKeyColumnsHint();
		if (keys.isEmpty()) {
			throw snapshotFailure("A unique key is required for an after snapshot");
		}
		StringBuilder sql = selectPrefix(target, snapshotColumns(target));
		List<SnapshotParameter> parameters = new ArrayList<SnapshotParameter>();
		sql.append(" WHERE ");
		if (identities == null || identities.isEmpty()) {
			sql.append("1 = 0");
		} else {
			for (int rowIndex = 0; rowIndex < identities.size(); rowIndex++) {
				if (rowIndex > 0) { sql.append(" OR "); }
				sql.append('(');
				Map<String, Object> values = identities.get(rowIndex).getKeyValues();
				for (int keyIndex = 0; keyIndex < keys.size(); keyIndex++) {
					if (keyIndex > 0) { sql.append(" AND "); }
					String key = keys.get(keyIndex);
					sql.append(quote(key)).append(" = ?");
					parameters.add(SnapshotParameter.value(value(values, key)));
				}
				sql.append(')');
			}
		}
		appendLimit(sql, target);
		return new SnapshotQuery(sql.toString(), parameters);
	}

	@Override
	public int getOrder() {
		return 100;
	}

	private StringBuilder selectPrefix(ChangeTarget target, List<String> columns) {
		StringBuilder sql = new StringBuilder("SELECT ");
		for (int index = 0; index < columns.size(); index++) {
			if (index > 0) { sql.append(", "); }
			sql.append(quote(columns.get(index)));
		}
		sql.append(" FROM ").append(qualifiedTable(target.getTable()));
		Alias alias = target.getSourceTable() == null ? null : target.getSourceTable().getAlias();
		if (alias != null) {
			sql.append(alias.isUseAs() ? " AS " : " ").append(alias.getName());
		}
		return sql;
	}

	private List<String> snapshotColumns(ChangeTarget target) {
		Map<String, String> columns = new LinkedHashMap<String, String>();
		addColumns(columns, target.getKeyColumnsHint());
		addColumns(columns, target.getMonitoredColumns());
		if (columns.isEmpty()) {
			throw snapshotFailure("Snapshot columns must not be empty");
		}
		return new ArrayList<String>(columns.values());
	}

	private void addColumns(Map<String, String> columns, List<String> additions) {
		for (String column : additions) {
			String key = column.toLowerCase(Locale.ENGLISH);
			if (!columns.containsKey(key)) {
				columns.put(key, column);
			}
		}
	}

	private void appendLimit(StringBuilder sql, ChangeTarget target) {
		long limit = (long) target.getMaxRows() + 1L;
		sql.append(" FETCH FIRST ").append(limit).append(" ROWS ONLY");
	}

	private String qualifiedTable(TableIdentifier table) {
		StringBuilder name = new StringBuilder();
		appendPart(name, table.getCatalog());
		appendPart(name, table.getSchema());
		appendPart(name, table.getTable());
		return name.toString();
	}

	private void appendPart(StringBuilder name, String part) {
		if (part == null || part.isEmpty()) { return; }
		if (name.length() > 0) { name.append('.'); }
		name.append(quote(part));
	}

	private String quote(String identifier) {
		String normalized = identifier == null ? "" : identifier.toUpperCase(Locale.ENGLISH);
		return '"' + normalized.replace("\"", "\"\"") + '"';
	}

	private Object value(Map<String, Object> values, String key) {
		for (Map.Entry<String, Object> entry : values.entrySet()) {
			if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(key)) {
				return entry.getValue();
			}
		}
		throw snapshotFailure("Row identity does not contain every unique key column");
	}

	private DataChangeException snapshotFailure(String message) {
		return new DataChangeException(DataChangeErrorCode.SNAPSHOT, message);
	}
}
