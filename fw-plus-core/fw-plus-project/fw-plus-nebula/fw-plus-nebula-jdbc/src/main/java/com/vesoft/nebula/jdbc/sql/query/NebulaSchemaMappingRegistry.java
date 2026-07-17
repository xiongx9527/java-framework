package com.vesoft.nebula.jdbc.sql.query;

import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NebulaSchemaMappingRegistry {
	private final Map<String, TableMapping> tables;

	private NebulaSchemaMappingRegistry(Map<String, TableMapping> tables) {
		this.tables = Collections.unmodifiableMap(new LinkedHashMap<String, TableMapping>(tables));
	}

	public static Builder builder() {
		return new Builder();
	}

	public TableMapping getRequired(String sqlTable) throws SQLException {
		TableMapping tableMapping = tables.get(sqlTable);
		if (tableMapping == null) {
			throw new SQLException("NEBULA_SQL_QUERY_MISSING_TABLE_MAPPING: " + sqlTable);
		}
		return tableMapping;
	}

	public static class Builder {
		private final Map<String, TableMapping> tables = new LinkedHashMap<String, TableMapping>();

		public Builder addTable(TableMapping tableMapping) {
			tables.put(tableMapping.getSqlTable(), tableMapping);
			return this;
		}

		public NebulaSchemaMappingRegistry build() {
			return new NebulaSchemaMappingRegistry(tables);
		}
	}
}
