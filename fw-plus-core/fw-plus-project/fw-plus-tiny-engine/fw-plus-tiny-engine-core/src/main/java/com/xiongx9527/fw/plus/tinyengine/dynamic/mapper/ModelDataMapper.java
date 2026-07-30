package com.xiongx9527.fw.plus.tinyengine.dynamic.mapper;

import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableAlteration;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableColumn;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableDefinition;
import com.xiongx9527.fw.plus.tinyengine.dynamic.sql.DynamicSqlProvider;
import org.apache.ibatis.annotations.DeleteProvider;
import org.apache.ibatis.annotations.InsertProvider;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.SelectProvider;
import org.apache.ibatis.annotations.UpdateProvider;

import java.util.List;
import java.util.Map;

public interface ModelDataMapper {

	@SelectProvider(type = DynamicSqlProvider.class, method = "select")
	List<Map<String, Object>> select(Map<String, Object> params);

	@SelectProvider(type = DynamicSqlProvider.class, method = "count")
	long count(Map<String, Object> params);

	@InsertProvider(type = DynamicSqlProvider.class, method = "insert")
	long insert(Map<String, Object> params);

	@UpdateProvider(type = DynamicSqlProvider.class, method = "update")
	int update(Map<String, Object> params);

	@DeleteProvider(type = DynamicSqlProvider.class, method = "delete")
	int delete(Map<String, Object> params);

	@SelectProvider(type = DynamicSqlProvider.class, method = "describeTable")
	List<DynamicTableColumn> describe(@Param("tableName") String tableName);

	@UpdateProvider(type = DynamicSqlProvider.class, method = "createTable")
	void create(DynamicTableDefinition definition);

	@UpdateProvider(type = DynamicSqlProvider.class, method = "alterTable")
	void alter(DynamicTableAlteration alteration);

	@UpdateProvider(type = DynamicSqlProvider.class, method = "dropTable")
	void drop(String tableName);

	@InsertProvider(type = DynamicSqlProvider.class, method = "initializeTable")
	void initialize(Map<String, Object> params);

	@InsertProvider(type = DynamicSqlProvider.class, method = "batchInsert")
	void batchInsert(Map<String, Object> params);
}
