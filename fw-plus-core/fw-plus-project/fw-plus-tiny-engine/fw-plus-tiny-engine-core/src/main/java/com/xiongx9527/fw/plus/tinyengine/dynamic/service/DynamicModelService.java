package com.xiongx9527.fw.plus.tinyengine.dynamic.service;

import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableAlteration;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableDefinition;

import java.util.List;
import java.util.Map;

public interface DynamicModelService {

	void createTable(DynamicTableDefinition definition);

	void alterTable(DynamicTableAlteration alteration);

	void dropTable(String tableName);

	void initializeTable(String tableName, Map<String, Object> data);

	void batchInsert(String tableName, List<Map<String, Object>> rows);
}
