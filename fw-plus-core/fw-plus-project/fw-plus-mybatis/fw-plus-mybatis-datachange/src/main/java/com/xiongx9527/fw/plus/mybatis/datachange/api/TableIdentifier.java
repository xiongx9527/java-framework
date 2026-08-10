package com.xiongx9527.fw.plus.mybatis.datachange.api;

import java.util.Locale;
import java.util.Objects;

public final class TableIdentifier {
	private final String catalog;
	private final String schema;
	private final String table;

	public TableIdentifier(String catalog, String schema, String table) {
		this.catalog = normalize(catalog);
		this.schema = normalize(schema);
		this.table = normalize(table);
	}

	public String getCatalog() { return catalog; }
	public String getSchema() { return schema; }
	public String getTable() { return table; }

	private String normalize(String value) {
		return value == null ? null : value.trim().toLowerCase(Locale.ENGLISH);
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) { return true; }
		if (!(other instanceof TableIdentifier)) { return false; }
		TableIdentifier that = (TableIdentifier) other;
		return Objects.equals(catalog, that.catalog) && Objects.equals(schema, that.schema)
				&& Objects.equals(table, that.table);
	}

	@Override
	public int hashCode() { return Objects.hash(catalog, schema, table); }

	@Override
	public String toString() {
		StringBuilder value = new StringBuilder();
		append(value, catalog);
		append(value, schema);
		append(value, table);
		return value.toString();
	}

	private void append(StringBuilder value, String part) {
		if (part != null && !part.isEmpty()) {
			if (value.length() > 0) { value.append('.'); }
			value.append(part);
		}
	}
}
