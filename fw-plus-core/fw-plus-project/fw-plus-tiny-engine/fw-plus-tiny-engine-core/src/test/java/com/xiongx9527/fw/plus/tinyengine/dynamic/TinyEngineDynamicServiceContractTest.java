package com.xiongx9527.fw.plus.tinyengine.dynamic;

import com.xiongx9527.fw.plus.tinyengine.dynamic.mapper.ModelDataMapper;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicDelete;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicInsert;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicInsertResult;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicPageResult;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicQuery;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableAlteration;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableColumn;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableDefinition;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicUpdate;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.impl.DynamicModelServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.dynamic.service.impl.DynamicServiceImpl;
import com.xiongx9527.fw.plus.tinyengine.model.entity.Model;
import com.xiongx9527.fw.plus.tinyengine.service.ModelService;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUser;
import com.xiongx9527.fw.plus.tinyengine.support.context.TinyEngineUserContext;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TinyEngineDynamicServiceContractTest {

	@Test
	void shouldDelegateLegalCrudAndPaginationRequestsWithoutEmbeddingValuesInSql() {
		RecordingMapper mapper = new RecordingMapper();
		DynamicServiceImpl service = authorizedService(mapper, "tenant-a", "Customer", "tenant-a");
		DynamicQuery query = new DynamicQuery();
		query.setTableName("Customer");
		query.setFields(Arrays.asList("id", "display_name"));
		query.setConditions(Collections.<String, Object>singletonMap("enabled", Boolean.TRUE));
		query.setOrderBy("display_name");
		query.setOrderType("ASC");
		query.setCurrentPage(Integer.valueOf(2));
		query.setPageSize(Integer.valueOf(5));

		DynamicPageResult result = service.query(query);
		assertEquals(1, result.getList().size());
		assertEquals(1L, result.getTotal());
		assertEquals(1, result.getPages());
		assertEquals(5, mapper.lastQuery.get("offset"));
		assertEquals("dynamic_customer", mapper.lastQuery.get("tableName"));
		assertSame(Boolean.TRUE, ((Map<?, ?>) mapper.lastQuery.get("conditions")).get("enabled"));

		DynamicInsert insert = new DynamicInsert("Customer", Collections.<String, Object>singletonMap("display_name", "Alice"));
		DynamicUpdate update = new DynamicUpdate("Customer", Collections.<String, Object>singletonMap("display_name", "Bob"),
				Collections.<String, Object>singletonMap("id", Integer.valueOf(1)));
		DynamicDelete delete = new DynamicDelete("Customer", Collections.<String, Object>singletonMap("id", Integer.valueOf(1)));
		DynamicInsertResult insertResult = service.insert(insert);
		assertEquals(1, insertResult.getInsert());
		assertEquals(new BigInteger("9223372036854775808123"), insertResult.getId());
		assertEquals(1, service.update(update));
		assertEquals(1, service.delete(delete));
		assertEquals("Alice", ((Map<?, ?>) mapper.lastInsert.get("data")).get("display_name"));
	}

	@Test
	void shouldSupportLegalTableStructureAndLifecycleRequests() {
		RecordingMapper mapper = new RecordingMapper();
		DynamicServiceImpl dataService = authorizedService(mapper, "tenant-a", "Customer", "tenant-a");
		DynamicModelServiceImpl modelService = new DynamicModelServiceImpl(mapper);
		DynamicTableDefinition definition = new DynamicTableDefinition("dynamic_customer",
				Collections.singletonList(new com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicColumnDefinition("display_name", "VARCHAR", true)));

		List<DynamicTableColumn> structure = dataService.getTableStructure("Customer");
		modelService.createTable(definition);
		modelService.alterTable(new DynamicTableAlteration("dynamic_customer", definition.getColumns(), Collections.<String>emptyList()));
		modelService.initializeTable("dynamic_customer", Collections.<String, Object>singletonMap("display_name", "Alice"));
		modelService.batchInsert("dynamic_customer", Collections.singletonList(Collections.<String, Object>singletonMap("display_name", "Bob")));
		modelService.dropTable("dynamic_customer");

		assertEquals("display_name", structure.get(0).getName());
		assertEquals("dynamic_customer", mapper.lastCreated.getTableName());
		assertEquals("dynamic_customer", mapper.lastDroppedTableName);
	}

	@Test
	void shouldRejectUnregisteredAndCrossTenantModelsBeforeMapperExecution() {
		RecordingMapper mapper = new RecordingMapper();
		DynamicServiceImpl missing = authorizedService(mapper, "tenant-a", "Missing", "tenant-a");
		DynamicQuery missingQuery = new DynamicQuery();
		missingQuery.setTableName("Missing");
		assertThrows(IllegalArgumentException.class, () -> missing.query(missingQuery));
		assertEquals(0, mapper.executionCount);

		DynamicServiceImpl crossTenant = authorizedService(mapper, "tenant-a", "Customer", "tenant-b");
		DynamicQuery crossTenantQuery = new DynamicQuery();
		crossTenantQuery.setTableName("Customer");
		assertThrows(IllegalArgumentException.class, () -> crossTenant.query(crossTenantQuery));
		assertEquals(0, mapper.executionCount);
	}

	@Test
	void shouldRejectUnsafeIdentifiersAndUnconditionalWritesBeforeMapperExecution() {
		RecordingMapper mapper = new RecordingMapper();
		DynamicServiceImpl service = authorizedService(mapper, "tenant-a", "Customer", "tenant-a");

		DynamicQuery unsafeTable = new DynamicQuery();
		unsafeTable.setTableName("Customer;drop");
		assertThrows(RuntimeException.class, () -> service.query(unsafeTable));

		DynamicQuery unsafeField = new DynamicQuery();
		unsafeField.setTableName("Customer");
		unsafeField.setFields(Collections.singletonList("name desc"));
		assertThrows(RuntimeException.class, () -> service.query(unsafeField));

		DynamicQuery unsafeOrder = new DynamicQuery();
		unsafeOrder.setTableName("Customer");
		unsafeOrder.setOrderBy("name;drop");
		unsafeOrder.setOrderType("ASC");
		assertThrows(RuntimeException.class, () -> service.query(unsafeOrder));

		DynamicQuery unsafeOrderType = new DynamicQuery();
		unsafeOrderType.setTableName("Customer");
		unsafeOrderType.setOrderBy("name");
		unsafeOrderType.setOrderType("ASC;drop");
		assertThrows(RuntimeException.class, () -> service.query(unsafeOrderType));

		assertThrows(IllegalArgumentException.class, () -> service.update(
				new DynamicUpdate("Customer", Collections.<String, Object>singletonMap("name", "Alice"),
						Collections.<String, Object>emptyMap())));
		assertThrows(IllegalArgumentException.class, () -> service.delete(
				new DynamicDelete("Customer", Collections.<String, Object>emptyMap())));
		assertEquals(0, mapper.executionCount);
	}

	@Test
	void shouldFailExplicitlyWhenMapperDoesNotReturnGeneratedKey() {
		RecordingMapper mapper = new RecordingMapper();
		mapper.populateGeneratedKey = false;
		DynamicServiceImpl service = authorizedService(mapper, "tenant-a", "Customer", "tenant-a");

		assertThrows(IllegalStateException.class, () -> service.insert(
				new DynamicInsert("Customer", Collections.<String, Object>singletonMap("name", "Alice"))));
	}

	private DynamicServiceImpl authorizedService(RecordingMapper mapper, String currentTenant,
			String modelName, String modelTenant) {
		ModelService modelService = mock(ModelService.class);
		if (!"Missing".equals(modelName)) {
			Model model = new Model();
			model.setNameEn(modelName);
			model.setTenantId(modelTenant);
			when(modelService.getModelByEnName(modelName)).thenReturn(Collections.singletonList(model));
		}
		TinyEngineUserContext userContext = mock(TinyEngineUserContext.class);
		when(userContext.getCurrentUser()).thenReturn(
				new TinyEngineUser("user-1", "tester", currentTenant, "renter-a", Integer.valueOf(1)));
		return new DynamicServiceImpl(mapper, modelService, userContext);
	}

	private static class RecordingMapper implements ModelDataMapper {

		private Map<String, Object> lastQuery;
		private Map<String, Object> lastInsert;
		private DynamicTableDefinition lastCreated;
		private String lastDroppedTableName;
		private int executionCount;
		private boolean populateGeneratedKey = true;

		@Override
		public List<Map<String, Object>> select(Map<String, Object> params) {
			executionCount++;
			lastQuery = params;
			return Collections.<Map<String, Object>>singletonList(new LinkedHashMap<String, Object>());
		}

		@Override
		public long count(Map<String, Object> params) {
			executionCount++;
			return 1L;
		}

		@Override
		public int insert(Map<String, Object> params) {
			executionCount++;
			lastInsert = params;
			if (populateGeneratedKey) {
				params.put("id", new BigInteger("9223372036854775808123"));
			}
			return 1;
		}

		@Override
		public int update(Map<String, Object> params) {
			executionCount++;
			return 1;
		}

		@Override
		public int delete(Map<String, Object> params) {
			executionCount++;
			return 1;
		}

		@Override
		public List<DynamicTableColumn> describe(String tableName) {
			executionCount++;
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
