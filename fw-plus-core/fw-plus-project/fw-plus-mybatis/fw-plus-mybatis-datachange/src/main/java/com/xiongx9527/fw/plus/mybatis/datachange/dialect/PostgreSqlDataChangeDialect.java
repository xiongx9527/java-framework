package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class PostgreSqlDataChangeDialect extends AbstractStandardDataChangeDialect {
	@Override
	public boolean supports(DatabaseMetadata metadata) {
		return metadata != null && "postgresql".equals(metadata.getProductName());
	}

	@Override
	protected String quote(String identifier) {
		String value = identifier == null ? "" : identifier;
		if (value.length() >= 2 && value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
			value = value.substring(1, value.length() - 1).replace("\"\"", "\"");
		} else {
			value = value.toLowerCase(java.util.Locale.ENGLISH);
		}
		return '"' + value.replace("\"", "\"\"") + '"';
	}

	@Override
	public List<String> resolveIdentityColumns(Connection connection, ChangeTarget target,
			List<String> defaultColumns) {
		ConflictTarget conflict = target == null ? ConflictTarget.none() : target.getConflictTarget();
		if (conflict.getType() == ConflictTarget.Type.COLUMNS) {
			return immutable(conflict.getColumns());
		}
		if (conflict.getType() == ConflictTarget.Type.EXPRESSION) { throw identityUnavailable(); }
		if (conflict.getType() != ConflictTarget.Type.CONSTRAINT) { return immutable(defaultColumns); }
		if (connection == null) { throw identityUnavailable(); }
		try {
			DatabaseMetaData metadata = connection.getMetaData();
			List<IndexColumn> columns = new ArrayList<IndexColumn>();
			try (ResultSet rows = metadata.getIndexInfo(target.getTable().getCatalog(),
					target.getTable().getSchema(), target.getTable().getTable(), true, false)) {
				while (rows.next()) {
					String indexName = rows.getString("INDEX_NAME");
					if (!sameIdentifier(conflict.getConstraintName(), indexName)) { continue; }
					String columnName = rows.getString("COLUMN_NAME");
					if (columnName == null || columnName.trim().isEmpty()) { throw identityUnavailable(); }
					columns.add(new IndexColumn(rows.getShort("ORDINAL_POSITION"), columnName));
				}
			}
			if (columns.isEmpty()) { throw identityUnavailable(); }
			Collections.sort(columns, new Comparator<IndexColumn>() {
				@Override
				public int compare(IndexColumn left, IndexColumn right) {
					return Short.compare(left.position, right.position);
				}
			});
			List<String> names = new ArrayList<String>();
			for (IndexColumn column : columns) { names.add(column.name); }
			return immutable(names);
		} catch (Exception exception) {
			throw identityUnavailable();
		}
	}

	private boolean sameIdentifier(String expected, String actual) {
		return expected != null && actual != null
				&& unquote(expected).equalsIgnoreCase(unquote(actual));
	}

	private String unquote(String value) {
		String trimmed = value == null ? "" : value.trim();
		if (trimmed.length() >= 2 && trimmed.charAt(0) == '"'
				&& trimmed.charAt(trimmed.length() - 1) == '"') {
			return trimmed.substring(1, trimmed.length() - 1).replace("\"\"", "\"");
		}
		return trimmed.toLowerCase(Locale.ENGLISH);
	}

	private List<String> immutable(List<String> columns) {
		return Collections.unmodifiableList(new ArrayList<String>(
				columns == null ? Collections.<String>emptyList() : columns));
	}

	private DataChangeException identityUnavailable() {
		return new DataChangeException(DataChangeErrorCode.ROW_IDENTITY_UNAVAILABLE, "ROW_IDENTITY_UNAVAILABLE");
	}

	private static final class IndexColumn {
		private final short position;
		private final String name;

		private IndexColumn(short position, String name) {
			this.position = position;
			this.name = name;
		}
	}
}
