package com.xiongx9527.fw.plus.mybatis.datachange.adapter;

import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FrameworkChangeMetadata {
	private final TableIdentifier table;
	private final List<String> keyColumns;
	private final LogicalDeleteMetadata logicalDelete;
	private final GeneratedKeyAccessor generatedKeyAccessor;
	private final Map<String, String> propertyColumnMappings;
	private final FrameworkFieldMetadata versionField;
	private final List<FrameworkFieldMetadata> insertFillFields;
	private final List<FrameworkFieldMetadata> updateFillFields;

	public FrameworkChangeMetadata(TableIdentifier table, List<String> keyColumns,
			LogicalDeleteMetadata logicalDelete, GeneratedKeyAccessor generatedKeyAccessor) {
		this(table, keyColumns, logicalDelete, generatedKeyAccessor, null, null, null, null);
	}

	public FrameworkChangeMetadata(TableIdentifier table, List<String> keyColumns,
			LogicalDeleteMetadata logicalDelete, GeneratedKeyAccessor generatedKeyAccessor,
			Map<String, String> propertyColumnMappings, FrameworkFieldMetadata versionField,
			List<FrameworkFieldMetadata> insertFillFields, List<FrameworkFieldMetadata> updateFillFields) {
		this.table = table;
		this.keyColumns = Collections.unmodifiableList(new ArrayList<String>(
				keyColumns == null ? Collections.<String>emptyList() : keyColumns));
		this.logicalDelete = logicalDelete;
		this.generatedKeyAccessor = generatedKeyAccessor;
		this.propertyColumnMappings = Collections.unmodifiableMap(new LinkedHashMap<String, String>(
				propertyColumnMappings == null ? Collections.<String, String>emptyMap() : propertyColumnMappings));
		this.versionField = versionField;
		this.insertFillFields = immutableFields(insertFillFields);
		this.updateFillFields = immutableFields(updateFillFields);
	}

	public TableIdentifier getTable() { return table; }
	public List<String> getKeyColumns() { return keyColumns; }
	public LogicalDeleteMetadata getLogicalDelete() { return logicalDelete; }
	public GeneratedKeyAccessor getGeneratedKeyAccessor() { return generatedKeyAccessor; }
	public Map<String, String> getPropertyColumnMappings() { return propertyColumnMappings; }
	public FrameworkFieldMetadata getVersionField() { return versionField; }
	public List<FrameworkFieldMetadata> getInsertFillFields() { return insertFillFields; }
	public List<FrameworkFieldMetadata> getUpdateFillFields() { return updateFillFields; }

	private List<FrameworkFieldMetadata> immutableFields(List<FrameworkFieldMetadata> fields) {
		return Collections.unmodifiableList(new ArrayList<FrameworkFieldMetadata>(
				fields == null ? Collections.<FrameworkFieldMetadata>emptyList() : fields));
	}
}
