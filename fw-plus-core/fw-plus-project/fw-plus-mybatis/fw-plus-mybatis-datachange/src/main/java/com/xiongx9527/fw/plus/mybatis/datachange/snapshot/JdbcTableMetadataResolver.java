package com.xiongx9527.fw.plus.mybatis.datachange.snapshot;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JdbcTableMetadataResolver {
	private final Map<CacheKey, JdbcTableMetadata> cache = new HashMap<CacheKey, JdbcTableMetadata>();

	public synchronized JdbcTableMetadata resolve(Connection connection, TableIdentifier table) {
		try {
			DatabaseMetaData metadata = connection.getMetaData();
			CacheKey key = new CacheKey(dataSourceIdentity(connection, metadata), metadata.getDatabaseProductName(), table);
			JdbcTableMetadata cached = cache.get(key);
			if (cached != null) {
				return cached;
			}
			JdbcTableMetadata resolved = new JdbcTableMetadata(table, keyColumns(metadata, table), columns(metadata, table));
			cache.put(key, resolved);
			return resolved;
		} catch (SQLException exception) {
			throw new DataChangeException(DataChangeErrorCode.SNAPSHOT,
					"Unable to resolve JDBC metadata for " + table, exception);
		}
	}

	private String dataSourceIdentity(Connection connection, DatabaseMetaData metadata) throws SQLException {
		String url = metadata.getURL();
		return url == null ? connection.getClass().getName() + '@' + System.identityHashCode(connection) : url;
	}

	private List<String> keyColumns(DatabaseMetaData metadata, TableIdentifier table) throws SQLException {
		List<KeyColumn> keys = new ArrayList<KeyColumn>();
		ResultSet resultSet = metadata.getPrimaryKeys(table.getCatalog(), table.getSchema(), table.getTable());
		try {
			while (resultSet.next()) {
				keys.add(new KeyColumn(resultSet.getShort("KEY_SEQ"), resultSet.getString("COLUMN_NAME")));
			}
		} finally {
			resultSet.close();
		}
		Collections.sort(keys, new Comparator<KeyColumn>() {
			@Override
			public int compare(KeyColumn left, KeyColumn right) {
				return Short.compare(left.sequence, right.sequence);
			}
		});
		List<String> columns = new ArrayList<String>();
		for (KeyColumn key : keys) {
			columns.add(key.name);
		}
		return columns;
	}

	private List<String> columns(DatabaseMetaData metadata, TableIdentifier table) throws SQLException {
		List<String> columns = new ArrayList<String>();
		ResultSet resultSet = metadata.getColumns(table.getCatalog(), table.getSchema(), table.getTable(), null);
		try {
			while (resultSet.next()) {
				columns.add(resultSet.getString("COLUMN_NAME"));
			}
		} finally {
			resultSet.close();
		}
		return columns;
	}

	private static final class KeyColumn {
		private final short sequence;
		private final String name;

		private KeyColumn(short sequence, String name) {
			this.sequence = sequence;
			this.name = name;
		}
	}

	private static final class CacheKey {
		private final String dataSourceIdentity;
		private final String productName;
		private final String catalog;
		private final String schema;
		private final String table;

		private CacheKey(String dataSourceIdentity, String productName, TableIdentifier table) {
			this.dataSourceIdentity = dataSourceIdentity;
			this.productName = productName;
			this.catalog = table.getCatalog();
			this.schema = table.getSchema();
			this.table = table.getTable();
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) { return true; }
			if (!(other instanceof CacheKey)) { return false; }
			CacheKey that = (CacheKey) other;
			return Objects.equals(dataSourceIdentity, that.dataSourceIdentity)
					&& Objects.equals(productName, that.productName) && Objects.equals(catalog, that.catalog)
					&& Objects.equals(schema, that.schema) && Objects.equals(table, that.table);
		}

		@Override
		public int hashCode() {
			return Objects.hash(dataSourceIdentity, productName, catalog, schema, table);
		}
	}
}
