package com.xiongx9527.fw.plus.mybatis.datachange.config;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class TableMonitorRegistry {
	private final List<TableMonitor> tables;
	private final Set<String> tableNames;

	public TableMonitorRegistry(List<TableMonitor> tables) {
		this.tables = new ArrayList<TableMonitor>(tables == null ? Collections.<TableMonitor>emptyList() : tables);
		this.tableNames = new HashSet<String>();
		for (TableMonitor table : this.tables) {
			if (table != null && table.getName() != null) {
				tableNames.add(new TableIdentifier(null, null, table.getName()).getTable());
			}
		}
	}

	public boolean mightMatch(TableIdentifier table) {
		return table != null && tableNames.contains(table.getTable());
	}

	public Optional<TableMonitor> find(TableIdentifier table, DataChangeDialect dialect) {
		if (table == null || dialect == null) {
			return Optional.empty();
		}
		TableIdentifier normalized = dialect.normalize(table);
		for (TableMonitor monitor : tables) {
			if (monitor == null || monitor.getName() == null) {
				continue;
			}
			TableIdentifier identifier = new TableIdentifier(monitor.getCatalog(), monitor.getSchema(), monitor.getName());
			if (normalized.equals(dialect.normalize(identifier))) {
				return Optional.of(monitor);
			}
		}
		return Optional.empty();
	}
}
