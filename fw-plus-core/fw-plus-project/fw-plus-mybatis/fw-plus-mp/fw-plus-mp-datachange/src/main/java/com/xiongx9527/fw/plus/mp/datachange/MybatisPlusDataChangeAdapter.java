package com.xiongx9527.fw.plus.mp.datachange;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeInvocation;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkChangeMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkFieldMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.LogicalDeleteMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import org.apache.ibatis.mapping.SqlCommandType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MybatisPlusDataChangeAdapter implements DataChangeFrameworkAdapter {
	private static final int ORDER = 200;
	private final MybatisPlusEntityTypeResolver entityTypeResolver = new MybatisPlusEntityTypeResolver();

	@Override
	public boolean supports(DataChangeInvocation invocation) {
		if (invocation == null || invocation.getMappedStatement() == null) { return false; }
		SqlCommandType commandType = invocation.getMappedStatement().getSqlCommandType();
		if (commandType != SqlCommandType.INSERT && commandType != SqlCommandType.UPDATE
				&& commandType != SqlCommandType.DELETE) {
			return false;
		}
		Class<?> entityType = entityTypeResolver.resolve(invocation);
		return entityType != null && TableInfoHelper.getTableInfo(entityType) != null;
	}

	@Override
	public FrameworkChangeMetadata resolve(DataChangeInvocation invocation) {
		Class<?> entityType = entityTypeResolver.resolve(invocation);
		TableInfo tableInfo = entityType == null ? null : TableInfoHelper.getTableInfo(entityType);
		if (tableInfo == null) { return null; }
		Map<String, String> mappings = propertyColumnMappings(tableInfo);
		List<String> keyColumns = tableInfo.havePK()
				? Collections.singletonList(tableInfo.getKeyColumn()) : Collections.<String>emptyList();
		return new FrameworkChangeMetadata(tableIdentifier(tableInfo.getTableName()), keyColumns,
				logicalDelete(tableInfo), new MybatisPlusGeneratedKeyAccessor(mappings), mappings,
				fieldMetadata(tableInfo.getVersionFieldInfo()), fillFields(tableInfo, true), fillFields(tableInfo, false));
	}

	@Override
	public int getOrder() {
		return ORDER;
	}

	private Map<String, String> propertyColumnMappings(TableInfo tableInfo) {
		Map<String, String> mappings = new LinkedHashMap<String, String>();
		if (tableInfo.havePK()) { mappings.put(tableInfo.getKeyProperty(), tableInfo.getKeyColumn()); }
		for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
			mappings.put(fieldInfo.getProperty(), fieldInfo.getColumn());
		}
		return mappings;
	}

	private LogicalDeleteMetadata logicalDelete(TableInfo tableInfo) {
		if (!tableInfo.isLogicDelete()) { return null; }
		for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
			if (fieldInfo.isLogicDelete()) {
				return new LogicalDeleteMetadata(fieldInfo.getColumn(), fieldInfo.getLogicDeleteValue(),
						fieldInfo.getLogicNotDeleteValue());
			}
		}
		return null;
	}

	private List<FrameworkFieldMetadata> fillFields(TableInfo tableInfo, boolean insert) {
		List<FrameworkFieldMetadata> fields = new ArrayList<FrameworkFieldMetadata>();
		for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
			if (insert ? fieldInfo.isWithInsertFill() : fieldInfo.isWithUpdateFill()) {
				fields.add(fieldMetadata(fieldInfo));
			}
		}
		return fields;
	}

	private FrameworkFieldMetadata fieldMetadata(TableFieldInfo fieldInfo) {
		return fieldInfo == null ? null : new FrameworkFieldMetadata(fieldInfo.getProperty(), fieldInfo.getColumn());
	}

	private TableIdentifier tableIdentifier(String tableName) {
		String[] parts = tableName.split("\\.");
		if (parts.length == 1) { return new TableIdentifier(null, null, parts[0]); }
		if (parts.length == 2) { return new TableIdentifier(null, parts[0], parts[1]); }
		return new TableIdentifier(parts[parts.length - 3], parts[parts.length - 2], parts[parts.length - 1]);
	}
}
