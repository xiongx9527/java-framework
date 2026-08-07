package com.xiongx9527.fw.plus.mybatis.datachange.diff;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeOperation;
import com.xiongx9527.fw.plus.mybatis.datachange.api.FieldChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.config.LogicalDeleteConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.RowSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ChangeDiffEngine {

	public TableChange diff(TableIdentifier table, TableMonitor monitor, Map<String, String> propertyColumns,
			List<RowSnapshot> beforeRows, List<RowSnapshot> afterRows) {
		Map<RowIdentity, RowSnapshot> before = index(beforeRows);
		Map<RowIdentity, RowSnapshot> after = index(afterRows);
		Set<RowIdentity> identities = new LinkedHashSet<RowIdentity>(before.keySet());
		identities.addAll(after.keySet());
		List<RowChange> changes = new ArrayList<RowChange>();
		Map<String, String> columnProperties = columnProperties(propertyColumns);

		for (RowIdentity identity : identities) {
			RowSnapshot beforeRow = before.get(identity);
			RowSnapshot afterRow = after.get(identity);
			DataChangeOperation operation = operation(monitor, beforeRow, afterRow);
			List<FieldChange> fields = changes(monitor, columnProperties, beforeRow, afterRow);
			if (!fields.isEmpty() || operation == DataChangeOperation.LOGICAL_DELETE) {
				changes.add(new RowChange(operation, identity, fields));
			}
		}
		return new TableChange(table, changes);
	}

	private Map<RowIdentity, RowSnapshot> index(List<RowSnapshot> rows) {
		Map<RowIdentity, RowSnapshot> indexed = new LinkedHashMap<RowIdentity, RowSnapshot>();
		for (RowSnapshot row : rows == null ? Collections.<RowSnapshot>emptyList() : rows) {
			if (row != null) {
				indexed.put(row.getIdentity(), row);
			}
		}
		return indexed;
	}

	private List<FieldChange> changes(TableMonitor monitor, Map<String, String> columnProperties,
			RowSnapshot before, RowSnapshot after) {
		List<FieldChange> fields = new ArrayList<FieldChange>();
		for (String column : monitoredColumns(monitor)) {
			Object beforeValue = value(before, column);
			Object afterValue = value(after, column);
			if (!Objects.equals(beforeValue, afterValue)) {
				String property = columnProperties.get(column);
				fields.add(new FieldChange(column, property == null ? column : property, beforeValue, afterValue));
			}
		}
		return fields;
	}

	private Set<String> monitoredColumns(TableMonitor monitor) {
		Set<String> columns = new LinkedHashSet<String>();
		if (monitor != null) {
			columns.addAll(monitor.getIncludeColumns());
			columns.removeAll(monitor.getExcludeColumns());
		}
		return columns;
	}

	private Map<String, String> columnProperties(Map<String, String> propertyColumns) {
		Map<String, String> columns = new LinkedHashMap<String, String>();
		if (propertyColumns != null) {
			for (Map.Entry<String, String> entry : propertyColumns.entrySet()) {
				if (entry.getValue() != null) {
					columns.put(entry.getValue(), entry.getKey());
				}
			}
		}
		return columns;
	}

	private DataChangeOperation operation(TableMonitor monitor, RowSnapshot before, RowSnapshot after) {
		if (before == null) {
			return DataChangeOperation.INSERT;
		}
		if (after == null) {
			return DataChangeOperation.PHYSICAL_DELETE;
		}
		LogicalDeleteConfiguration logicalDelete = monitor == null ? null : monitor.getLogicalDelete();
		if (logicalDelete != null && Objects.equals(value(after, logicalDelete.getColumn()), logicalDelete.getDeletedValue())
				&& !Objects.equals(value(before, logicalDelete.getColumn()), logicalDelete.getDeletedValue())) {
			return DataChangeOperation.LOGICAL_DELETE;
		}
		return DataChangeOperation.UPDATE;
	}

	private Object value(RowSnapshot row, String column) {
		return row == null ? null : row.getValues().get(column);
	}
}
