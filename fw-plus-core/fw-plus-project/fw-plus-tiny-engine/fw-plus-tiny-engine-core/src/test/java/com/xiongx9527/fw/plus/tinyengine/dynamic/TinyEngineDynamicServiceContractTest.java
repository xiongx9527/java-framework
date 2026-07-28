package com.xiongx9527.fw.plus.tinyengine.dynamic;

import com.xiongx9527.fw.plus.tinyengine.dynamic.mapper.ModelDataMapper;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicDelete;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicInsert;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicPageResult;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicQuery;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableAlteration;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableColumn;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableDefinition;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicUpdate;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.impl.DynamicModelServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.impl.DynamicServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class TinyEngineDynamicServiceContractTest {

	@Test
	void shouldDelegateLegalCrudAndPaginationRequestsWithoutEmbeddingValuesInSql() {
		RecordingMapper mapper = new RecordingMapper();
		DynamicServiceImpl service = new DynamicServiceImpl(mapper);
		DynamicQuery query = new DynamicQuery();
		query.setTableName("dynamic_customer");
		query.setFields(Arrays.asList("id", "display_name"));
		query.setConditions(Collections.<String, Object>singletonMap("enabled", Boolean.TRUE));
		query.setOrderBy("display_name");
		query.setOrderType("ASC");
		query.setCurrentPage(Integer.valueOf(2));
		query.setPageSize(Integer.valueOf(5));

		DynamicPageResult result = service.query(query);
		assertEquals(1, result.getList().size());
		assertEquals(1L, result.getTotal());
		assertEquals(5, mapper.lastQuery.get("offset"));
		assertSame(Boolean.TRUE, ((Map<?, ?>) mapper.lastQuery.get("conditions")).get("enabled"));

		DynamicInsert insert = new DynamicInsert("dynamic_customer", Collections.<String, Object>singletonMap("display_name", "Alice"));
		DynamicUpdate update = new DynamicUpdate("dynamic_customer", Collections.<String, Object>singletonMap("display_name", "Bob"),
				Collections.<String, Object>singletonMap("id", Integer.valueOf(1)));
		DynamicDelete delete = new DynamicDelete("dynamic_customer", Collections.<String, Object>singletonMap("id", Integer.valueOf(1)));
		assertEquals(7L, service.insert(insert));
		assertEquals(1, service.update(update));
		assertEquals(1, service.delete(delete));
		assertEquals("Alice", ((Map<?, ?>) mapper.lastInsert.get("data")).get("display_name"));
	}

	@Test
	void shouldSupportLegalTableStructureAndLifecycleRequests() {
		RecordingMapper mapper = new RecordingMapper();
		DynamicServiceImpl dataService = new DynamicServiceImpl(mapper);
		DynamicModelServiceImpl modelService = new DynamicModelServiceImpl(mapper);
		DynamicTableDefinition definition = new DynamicTableDefinition("dynamic_customer",
				Collections.singletonList(new com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicColumnDefinition("display_name", "VARCHAR", true)));

		List<DynamicTableColumn> structure = dataService.getTableStructure("dynamic_customer");
		modelService.createTable(definition);
		modelService.alterTable(new DynamicTableAlteration("dynamic_customer", definition.getColumns(), Collections.<String>emptyList()));
		modelService.initializeTable("dynamic_customer", Collections.<String, Object>singletonMap("display_name", "Alice"));
		modelService.batchInsert("dynamic_customer", Collections.singletonList(Collections.<String, Object>singletonMap("display_name", "Bob")));
		modelService.dropTable("dynamic_customer");

		assertEquals("display_name", structure.get(0).getName());
		assertEquals("dynamic_customer", mapper.lastCreated.getTableName());
		assertEquals("dynamic_customer", mapper.lastDroppedTableName);
	}

	private static class RecordingMapper implements ModelDataMapper {

		private Map<String, Object> lastQuery;
		private Map<String, Object> lastInsert;
		private DynamicTableDefinition lastCreated;
		private String lastDroppedTableName;

		@Override
		public List<Map<String, Object>> select(Map<String, Object> params) {
			lastQuery = params;
			return Collections.<Map<String, Object>>singletonList(new LinkedHashMap<String, Object>());
		}

		@Override
		public long count(Map<String, Object> params) {
			return 1L;
		}

		@Override
		public long insert(Map<String, Object> params) {
			lastInsert = params;
			return 7L;
		}

		@Override
		public int update(Map<String, Object> params) {
			return 1;
		}

		@Override
		public int delete(Map<String, Object> params) {
			return 1;
		}

		@Override
		public List<DynamicTableColumn> describe(String tableName) {
			return Collections.singletonList(new DynamicTableColumn("display_name", "VARCHAR", true));
		}

		@Override
		public void create(DynamicTableDefinition definition) {
			lastCreated = definition;
		}

		@Override
		public void alter(DynamicTableAlteration alteration) {
		}

		@Override
		public void drop(String tableName) {
			lastDroppedTableName = tableName;
		}

		@Override
		public void initialize(Map<String, Object> params) {
		}

		@Override
		public void batchInsert(Map<String, Object> params) {
		}
	}
}
