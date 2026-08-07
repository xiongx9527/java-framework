package com.xiongx9527.fw.plus.mybatis.datachange.config;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DataChangeConfiguration {
	private boolean enabled;
	private List<TableMonitor> tables = new ArrayList<TableMonitor>();
	private int maxSnapshotRows = 1000;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public List<TableMonitor> getTables() {
		return Collections.unmodifiableList(new ArrayList<TableMonitor>(tables));
	}

	public void setTables(List<TableMonitor> tables) {
		this.tables = tables == null ? new ArrayList<TableMonitor>() : new ArrayList<TableMonitor>(tables);
	}

	public int getMaxSnapshotRows() {
		return maxSnapshotRows;
	}

	public void setMaxSnapshotRows(int maxSnapshotRows) {
		this.maxSnapshotRows = maxSnapshotRows;
	}

	public void validate() {
		if (maxSnapshotRows <= 0) {
			throw new IllegalArgumentException("maxSnapshotRows must be greater than zero");
		}
		Set<TableIdentifier> identifiers = new HashSet<TableIdentifier>();
		for (TableMonitor table : tables) {
			if (table == null || table.getName() == null || table.getName().trim().isEmpty()) {
				throw new IllegalArgumentException("table name must not be blank");
			}
			TableIdentifier identifier = new TableIdentifier(table.getCatalog(), table.getSchema(), table.getName());
			if (!identifiers.add(identifier)) {
				throw new IllegalArgumentException("duplicate monitored table: " + identifier);
			}
			if (table.getMaxSnapshotRows() != null && table.getMaxSnapshotRows().intValue() <= 0) {
				throw new IllegalArgumentException("maxSnapshotRows must be greater than zero for " + identifier);
			}
			Set<String> keyColumns = normalizedColumns(table.getKeyColumns());
			if (keyColumns.size() != table.getKeyColumns().size()) {
				throw new IllegalArgumentException("duplicate key column for " + identifier);
			}
			Set<String> included = normalizedColumns(table.getIncludeColumns());
			Set<String> excluded = normalizedColumns(table.getExcludeColumns());
			included.retainAll(excluded);
			if (!included.isEmpty()) {
				throw new IllegalArgumentException("include/exclude overlap for " + identifier + ": " + included);
			}
		}
	}

	private Set<String> normalizedColumns(List<String> columns) {
		Set<String> normalized = new LinkedHashSet<String>();
		for (String column : columns) {
			if (column == null || column.trim().isEmpty()) {
				throw new IllegalArgumentException("column name must not be blank");
			}
			normalized.add(column.trim().toLowerCase(Locale.ENGLISH));
		}
		return normalized;
	}
}
