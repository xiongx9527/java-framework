package com.xiongx9527.fw.plus.mf.datachange;

import com.mybatisflex.core.logicdelete.LogicDeleteManager;
import com.mybatisflex.core.logicdelete.LogicDeleteProcessor;
import com.mybatisflex.core.table.ColumnInfo;
import com.mybatisflex.core.table.IdInfo;
import com.mybatisflex.core.table.TableInfo;
import com.mybatisflex.core.table.TableInfoFactory;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeInvocation;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkChangeMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkFieldMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.LogicalDeleteMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import org.apache.ibatis.mapping.SqlCommandType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class MybatisFlexDataChangeAdapter implements DataChangeFrameworkAdapter {
	private static final int ORDER = 100;
	private final MybatisFlexEntityTypeResolver entityTypeResolver = new MybatisFlexEntityTypeResolver();

	@Override
	public boolean supports(DataChangeInvocation invocation) {
		if (invocation == null || invocation.getMappedStatement() == null) { return false; }
		SqlCommandType commandType = invocation.getMappedStatement().getSqlCommandType();
		if (commandType != SqlCommandType.INSERT && commandType != SqlCommandType.UPDATE
				&& commandType != SqlCommandType.DELETE) {
			return false;
		}
		return tableInfo(entityTypeResolver.resolve(invocation)) != null;
	}

	@Override
	public FrameworkChangeMetadata resolve(DataChangeInvocation invocation) {
		TableInfo tableInfo = tableInfo(entityTypeResolver.resolve(invocation));
		if (tableInfo == null) { return null; }
		Map<String, String> mappings = propertyColumnMappings(tableInfo);
		return new FrameworkChangeMetadata(tableIdentifier(tableInfo), keyColumns(tableInfo),
				logicalDelete(tableInfo), new MybatisFlexGeneratedKeyAccessor(mappings), mappings,
				fieldMetadata(tableInfo, tableInfo.getVersionColumn()), fillFields(tableInfo, true),
				fillFields(tableInfo, false));
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	private TableInfo tableInfo(Class<?> entityType) {
		return entityType == null ? null : TableInfoFactory.ofEntityClass(entityType);
	}

	private Map<String, String> propertyColumnMappings(TableInfo tableInfo) {
		Map<String, String> mappings = new LinkedHashMap<String, String>();
		Map<String, String> tableMappings = tableInfo.getPropertyColumnMapping();
		for (Field field : TableInfoFactory.getColumnFields(tableInfo.getEntityClass())) {
			String property = field.getName();
			String column = tableMappings.get(property);
			ColumnInfo columnInfo = columnInfo(tableInfo, property);
			if (column != null && columnInfo != null && !columnInfo.isIgnore()) {
				mappings.put(property, column);
			}
		}
		return mappings;
	}

	private ColumnInfo columnInfo(TableInfo tableInfo, String property) {
		List<IdInfo> primaryKeys = tableInfo.getPrimaryKeyList();
		if (primaryKeys != null) {
			for (IdInfo primaryKey : primaryKeys) {
				if (property.equals(primaryKey.getProperty())) { return primaryKey; }
			}
		}
		for (ColumnInfo columnInfo : columns(tableInfo)) {
			if (property.equals(columnInfo.getProperty())) { return columnInfo; }
		}
		return null;
	}

	private List<String> keyColumns(TableInfo tableInfo) {
		String[] primaryColumns = tableInfo.getPrimaryColumns();
		if (primaryColumns == null || primaryColumns.length == 0) {
			return Collections.emptyList();
		}
		List<String> keyColumns = new ArrayList<String>(primaryColumns.length);
		Collections.addAll(keyColumns, primaryColumns);
		return keyColumns;
	}

	private LogicalDeleteMetadata logicalDelete(TableInfo tableInfo) {
		if (tableInfo.getLogicDeleteColumn() == null) { return null; }
		LogicDeleteProcessor processor = LogicDeleteManager.getProcessor();
		return new LogicalDeleteMetadata(tableInfo.getLogicDeleteColumn(),
				processor == null ? null : processor.getLogicDeletedValue(),
				processor == null ? null : processor.getLogicNormalValue());
	}

	private FrameworkFieldMetadata fieldMetadata(TableInfo tableInfo, String column) {
		if (column == null) { return null; }
		for (ColumnInfo columnInfo : columns(tableInfo)) {
			if (!columnInfo.isIgnore() && column.equals(columnInfo.getColumn())) {
				return new FrameworkFieldMetadata(columnInfo.getProperty(), columnInfo.getColumn());
			}
		}
		return null;
	}

	private List<FrameworkFieldMetadata> fillFields(TableInfo tableInfo, boolean insert) {
		Map<String, String> fillColumns = insert ? tableInfo.getOnInsertColumns() : tableInfo.getOnUpdateColumns();
		if (fillColumns == null || fillColumns.isEmpty()) { return Collections.emptyList(); }
		List<FrameworkFieldMetadata> fields = new ArrayList<FrameworkFieldMetadata>();
		Set<String> addedProperties = new LinkedHashSet<String>();
		Set<String> addedColumns = new LinkedHashSet<String>();
		Map<String, String> tableMappings = tableInfo.getPropertyColumnMapping();
		for (Field field : TableInfoFactory.getColumnFields(tableInfo.getEntityClass())) {
			String property = field.getName();
			String column = tableMappings.get(property);
			ColumnInfo columnInfo = columnInfo(tableInfo, property);
			if (column == null || columnInfo == null || columnInfo.isIgnore() || !fillColumns.containsKey(column)
					|| addedProperties.contains(property) || addedColumns.contains(column)) {
				continue;
			}
			fields.add(new FrameworkFieldMetadata(property, column));
			addedProperties.add(property);
			addedColumns.add(column);
		}
		return fields;
	}

	private List<ColumnInfo> columns(TableInfo tableInfo) {
		List<ColumnInfo> columns = tableInfo.getColumnInfoList();
		return columns == null ? Collections.<ColumnInfo>emptyList() : columns;
	}

	private TableIdentifier tableIdentifier(TableInfo tableInfo) {
		String tableName = tableInfo.getTableNameWithSchema();
		String[] parts = tableName == null ? new String[0] : tableName.split("\\.");
		if (parts.length == 0) { return null; }
		if (parts.length == 1) { return new TableIdentifier(null, null, parts[0]); }
		if (parts.length == 2) { return new TableIdentifier(null, parts[0], parts[1]); }
		return new TableIdentifier(parts[parts.length - 3], parts[parts.length - 2], parts[parts.length - 1]);
	}
}
