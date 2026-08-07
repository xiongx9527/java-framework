package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MySqlDataChangeDialect extends AbstractStandardDataChangeDialect {
	@Override
	public boolean supports(DatabaseMetadata metadata) {
		return metadata != null && "mysql".equals(metadata.getProductName());
	}

	@Override
	protected String quote(String identifier) {
		String value = unquote(identifier, '`');
		return '`' + value.replace("`", "``") + '`';
	}

	public List<String> resolveOnDuplicateKeyColumns(Connection connection, ChangeTarget target) {
		if (connection == null || target == null || !target.isUpsert()) {
			throw identityUnavailable(null);
		}
		try {
			DatabaseMetaData metadata = connection.getMetaData();
			Map<String, List<IndexColumn>> indexes = uniqueIndexes(metadata, target);
			List<List<String>> matches = new ArrayList<List<String>>();
			for (List<IndexColumn> index : indexes.values()) {
				Collections.sort(index, new Comparator<IndexColumn>() {
					@Override
					public int compare(IndexColumn left, IndexColumn right) {
						return Short.compare(left.position, right.position);
					}
				});
				List<String> columns = new ArrayList<String>();
				boolean match = true;
				for (IndexColumn column : index) {
					columns.add(column.name);
					if (!assigned(target, column.name)) { match = false; }
				}
				if (match && !columns.isEmpty()) { matches.add(columns); }
			}
			if (matches.size() != 1) { throw identityUnavailable(null); }
			return Collections.unmodifiableList(new ArrayList<String>(matches.get(0)));
		} catch (SQLException exception) {
			throw identityUnavailable(exception);
		}
	}

	@Override
	public List<String> resolveIdentityColumns(Connection connection, ChangeTarget target,
			List<String> defaultColumns) {
		if (target != null && target.isUpsert() && target.getConflictColumnsHint().isEmpty()) {
			return resolveOnDuplicateKeyColumns(connection, target);
		}
		List<String> selected = target != null && !target.getConflictColumnsHint().isEmpty()
				? target.getConflictColumnsHint() : defaultColumns;
		return Collections.unmodifiableList(new ArrayList<String>(
				selected == null ? Collections.<String>emptyList() : selected));
	}

	private Map<String, List<IndexColumn>> uniqueIndexes(DatabaseMetaData metadata, ChangeTarget target)
			throws SQLException {
		Map<String, List<IndexColumn>> indexes = new LinkedHashMap<String, List<IndexColumn>>();
		ResultSet rows = metadata.getIndexInfo(identifier(metadata, target.getTable().getCatalog()),
				identifier(metadata, target.getTable().getSchema()),
				identifier(metadata, target.getTable().getTable()), true, false);
		try {
			while (rows.next()) {
				String indexName = rows.getString("INDEX_NAME");
				String columnName = rows.getString("COLUMN_NAME");
				if (indexName == null || columnName == null) { continue; }
				List<IndexColumn> columns = indexes.get(indexName);
				if (columns == null) {
					columns = new ArrayList<IndexColumn>();
					indexes.put(indexName, columns);
				}
				columns.add(new IndexColumn(rows.getShort("ORDINAL_POSITION"), columnName));
			}
		} finally {
			rows.close();
		}
		return indexes;
	}

	private boolean assigned(ChangeTarget target, String column) {
		for (AssignedColumn assigned : target.getAssignedColumns()) {
			if (assigned.getColumn() != null && assigned.getColumn().equalsIgnoreCase(column)) { return true; }
		}
		return false;
	}

	private String identifier(DatabaseMetaData metadata, String value) throws SQLException {
		return value != null && metadata.storesUpperCaseIdentifiers()
				? value.toUpperCase(Locale.ENGLISH) : value;
	}

	private DataChangeException identityUnavailable(Throwable cause) {
		return cause == null
				? new DataChangeException(DataChangeErrorCode.ROW_IDENTITY_UNAVAILABLE, "ROW_IDENTITY_UNAVAILABLE")
				: new DataChangeException(DataChangeErrorCode.ROW_IDENTITY_UNAVAILABLE,
						"ROW_IDENTITY_UNAVAILABLE", cause);
	}

	private String unquote(String identifier, char quote) {
		String value = identifier == null ? "" : identifier;
		return value.length() >= 2 && value.charAt(0) == quote && value.charAt(value.length() - 1) == quote
				? value.substring(1, value.length() - 1).replace("" + quote + quote, "" + quote) : value;
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
