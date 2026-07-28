package com.xiongx9527.fw.plus.tinyengine.dynamic.service.impl;

import com.xiongx9527.fw.plus.tinyengine.dynamic.mapper.ModelDataMapper;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicColumnDefinition;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableAlteration;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableDefinition;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.DynamicModelService;
import com.xiongx9527.fw.plus.tinyengine.dynamic.sql.TinyEngineSqlIdentifierValidator;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DynamicModelServiceImpl implements DynamicModelService {

	private final ModelDataMapper mapper;

	public DynamicModelServiceImpl(ModelDataMapper mapper) {
		if (mapper == null) {
			throw new IllegalArgumentException("ModelDataMapper must not be null");
		}
		this.mapper = mapper;
	}

	@Override
	public void createTable(DynamicTableDefinition definition) {
		validateDefinition(definition);
		mapper.create(definition);
	}

	@Override
	public void alterTable(DynamicTableAlteration alteration) {
		if (alteration == null) {
			throw new IllegalArgumentException("DynamicTableAlteration must not be null");
		}
		TinyEngineSqlIdentifierValidator.validateTableName(alteration.getTableName());
		for (DynamicColumnDefinition column : alteration.getAddColumns()) {
			validateColumn(column);
		}
		for (String column : alteration.getDropColumns()) {
			TinyEngineSqlIdentifierValidator.validateFieldName(column);
		}
		if (alteration.getAddColumns().isEmpty() && alteration.getDropColumns().isEmpty()) {
			throw new IllegalArgumentException("Table alteration must contain a change");
		}
		mapper.alter(alteration);
	}

	@Override
	public void dropTable(String tableName) {
		mapper.drop(TinyEngineSqlIdentifierValidator.validateTableName(tableName));
	}

	@Override
	public void initializeTable(String tableName, Map<String, Object> data) {
		mapper.initialize(tableDataParams(tableName, data));
	}

	@Override
	public void batchInsert(String tableName, List<Map<String, Object>> rows) {
		TinyEngineSqlIdentifierValidator.validateTableName(tableName);
		if (rows == null || rows.isEmpty()) {
			throw new IllegalArgumentException("Batch rows must not be empty");
		}
		for (Map<String, Object> row : rows) {
			validateData(row);
		}
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put("tableName", tableName);
		params.put("rows", rows);
		mapper.batchInsert(params);
	}

	private void validateDefinition(DynamicTableDefinition definition) {
		if (definition == null) {
			throw new IllegalArgumentException("DynamicTableDefinition must not be null");
		}
		TinyEngineSqlIdentifierValidator.validateTableName(definition.getTableName());
		if (definition.getColumns().isEmpty()) {
			throw new IllegalArgumentException("Table definition must contain columns");
		}
		for (DynamicColumnDefinition column : definition.getColumns()) {
			validateColumn(column);
		}
	}

	private void validateColumn(DynamicColumnDefinition column) {
		if (column == null) {
			throw new IllegalArgumentException("Column definition must not be null");
		}
		TinyEngineSqlIdentifierValidator.validateFieldName(column.getName());
	}

	private Map<String, Object> tableDataParams(String tableName, Map<String, Object> data) {
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put("tableName", TinyEngineSqlIdentifierValidator.validateTableName(tableName));
		validateData(data);
		params.put("data", data);
		return params;
	}

	private void validateData(Map<String, Object> data) {
		if (data == null || data.isEmpty()) {
			throw new IllegalArgumentException("Dynamic field values must not be empty");
		}
		for (String field : data.keySet()) {
			TinyEngineSqlIdentifierValidator.validateFieldName(field);
		}
	}
}
