package com.xiongx9527.fw.plus.tinyengine.dynamic;

import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicColumnDefinition;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableAlteration;
import com.xiongx9527.fw.plus.tinyengine.dynamic.model.DynamicTableDefinition;
import com.xiongx9527.fw.plus.tinyengine.dynamic.sql.DynamicSqlProvider;
import com.xiongx9527.fw.plus.tinyengine.dynamic.sql.TinyEngineSqlIdentifierException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TinyEngineDynamicSqlProviderTest {

	private final DynamicSqlProvider provider = new DynamicSqlProvider();

	@Test
	void shouldBuildCrudSqlWithValidatedIdentifiersAndBoundValues() {
		Map<String, Object> conditions = new LinkedHashMap<String, Object>();
		conditions.put("status", "active' OR 1=1 --");
		Map<String, Object> data = new LinkedHashMap<String, Object>();
		data.put("name", "O'Reilly");
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put("tableName", "dynamic_customer");
		params.put("fields", Arrays.asList("id", "name"));
		params.put("conditions", conditions);
		params.put("data", data);
		params.put("orderBy", "name");
		params.put("orderType", "DESC");
		params.put("offset", Integer.valueOf(0));
		params.put("pageSize", Integer.valueOf(10));

		String query = provider.select(params);
		String insert = provider.insert(params);
		String update = provider.update(params);
		String delete = provider.delete(params);

		assertTrue(query.contains("FROM dynamic_customer"));
		assertTrue(query.contains("status = #{conditions.status}"));
		assertTrue(query.contains("ORDER BY name DESC"));
		assertTrue(query.contains("LIMIT #{offset}, #{pageSize}"));
		assertTrue(insert.contains("name") && insert.contains("#{data.name}"));
		assertTrue(update.contains("name = #{data.name}") && update.contains("status = #{conditions.status}"));
		assertTrue(delete.contains("status = #{conditions.status}"));
		assertFalse(query.contains("active' OR 1=1"));
		assertFalse(insert.contains("O'Reilly"));
	}

	@Test
	void shouldRejectUnsafeIdentifiersForEveryCrudPathBeforeBuildingSql() {
		final Map<String, Object> unsafe = new LinkedHashMap<String, Object>();
		unsafe.put("tableName", "dynamic_customer; DROP TABLE users");
		unsafe.put("fields", Collections.singletonList("id"));
		unsafe.put("conditions", Collections.<String, Object>emptyMap());
		unsafe.put("data", Collections.singletonMap("name", "safe value"));

		assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.select(unsafe));
		assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.insert(unsafe));
		assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.update(unsafe));
		assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.delete(unsafe));
	}

	@Test
	void shouldRejectUnsafeSelectFieldWithLegalTableNameBeforeBuildingSql() {
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put("tableName", "dynamic_customer");
		params.put("fields", Collections.singletonList("name; DROP TABLE users"));
		params.put("conditions", Collections.<String, Object>emptyMap());

		assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.select(params));
	}

	@Test
	void shouldRejectUnsafeCrudFieldAndConditionIdentifiersWithLegalTableNameBeforeBuildingSql() {
		String[] unsafeIdentifiers = {
				"name; DROP TABLE users", "name/*comment*/", "display name", "LOWER(name)", "name UNION SELECT id"
		};
		for (String unsafeIdentifier : unsafeIdentifiers) {
			Map<String, Object> unsafeField = baseCrudParams();
			unsafeField.put("fields", Collections.singletonList(unsafeIdentifier));
			assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.select(unsafeField));

			Map<String, Object> unsafeData = baseCrudParams();
			unsafeData.put("data", Collections.<String, Object>singletonMap(unsafeIdentifier, "safe value"));
			assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.insert(unsafeData));
			assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.update(unsafeData));

			Map<String, Object> unsafeConditions = baseCrudParams();
			unsafeConditions.put("conditions", Collections.<String, Object>singletonMap(unsafeIdentifier, "safe value"));
			assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.select(unsafeConditions));
			assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.update(unsafeConditions));
			assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.delete(unsafeConditions));
		}
	}

	@Test
	void shouldRejectUnsafeOrderByAndOrderTypeWithLegalTableNameBeforeBuildingSql() {
		String[] unsafeOrderByValues = {
				"name; DROP TABLE users", "name/*comment*/", "display name", "LOWER(name)", "name UNION SELECT id"
		};
		for (String unsafeOrderBy : unsafeOrderByValues) {
			Map<String, Object> params = baseCrudParams();
			params.put("orderBy", unsafeOrderBy);
			params.put("orderType", "ASC");
			assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.select(params));
		}

		String[] unsafeOrderTypeValues = {
				"ASC; DROP TABLE users", "ASC/*comment*/", "ASC DESC", "LOWER(ASC)", "ASC UNION SELECT id"
		};
		for (String unsafeOrderType : unsafeOrderTypeValues) {
			Map<String, Object> params = baseCrudParams();
			params.put("orderBy", "name");
			params.put("orderType", unsafeOrderType);
			assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.select(params));
		}
	}

	@Test
	void shouldGuardEveryTableLifecycleAndBatchPath() {
		DynamicTableDefinition definition = new DynamicTableDefinition("dynamic_customer",
				Collections.singletonList(new DynamicColumnDefinition("display_name", "VARCHAR", true)));
		DynamicTableAlteration alteration = new DynamicTableAlteration("dynamic_customer",
				Collections.singletonList(new DynamicColumnDefinition("enabled", "BOOLEAN", false)),
				Collections.singletonList("legacy_name"));
		Map<String, Object> row = Collections.<String, Object>singletonMap("display_name", "Alice");

		assertTrue(provider.createTable(definition).contains("CREATE TABLE IF NOT EXISTS dynamic_customer"));
		assertTrue(provider.alterTable(alteration).contains("ALTER TABLE dynamic_customer"));
		assertTrue(provider.dropTable("dynamic_customer").contains("DROP TABLE IF EXISTS dynamic_customer"));
		assertTrue(provider.initializeTable("dynamic_customer", row).contains("#{data.display_name}"));
		assertTrue(provider.describeTable("dynamic_customer").contains("#{tableName}"));
		assertTrue(provider.batchInsert("dynamic_customer", Collections.singletonList(row)).contains("#{rows[0].display_name}"));

		DynamicTableDefinition unsafeColumn = new DynamicTableDefinition("dynamic_customer",
				Collections.singletonList(new DynamicColumnDefinition("name; DROP TABLE users", "VARCHAR", true)));
		assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.createTable(unsafeColumn));
		assertThrows(TinyEngineSqlIdentifierException.class,
				() -> provider.alterTable(new DynamicTableAlteration("dynamic_customer", Collections.<DynamicColumnDefinition>emptyList(),
						Collections.singletonList("legacy_name; DROP TABLE users"))));
		assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.dropTable("dynamic_customer; DROP TABLE users"));
		assertThrows(TinyEngineSqlIdentifierException.class,
				() -> provider.initializeTable("dynamic_customer", Collections.<String, Object>singletonMap("name; DROP", "Alice")));
		assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.describeTable("dynamic_customer; DROP TABLE users"));
		assertThrows(TinyEngineSqlIdentifierException.class,
				() -> provider.batchInsert("dynamic_customer", Collections.singletonList(Collections.<String, Object>singletonMap("name; DROP", "Alice"))));
	}

	@Test
	void shouldMapControlledColumnTypesAndRejectRawTypeFragments() {
		DynamicTableDefinition definition = new DynamicTableDefinition("dynamic_customer", Arrays.asList(
				new DynamicColumnDefinition("display_name", "VARCHAR", false),
				new DynamicColumnDefinition("code", "char", true),
				new DynamicColumnDefinition("amount", "DECIMAL", true)));

		String sql = provider.createTable(definition);

		assertTrue(sql.contains("display_name VARCHAR(255) NOT NULL"));
		assertTrue(sql.contains("code CHAR(1)"));
		assertTrue(sql.contains("amount DECIMAL"));

		String[] unsafeTypes = {
				"VARCHAR(64)", "VARCHAR(255); DROP TABLE users", "TEXT COMMENT 'injected'", "INT UNSIGNED"
		};
		for (String unsafeType : unsafeTypes) {
			DynamicTableDefinition unsafeDefinition = new DynamicTableDefinition("dynamic_customer",
					Collections.singletonList(new DynamicColumnDefinition("unsafe_column", unsafeType, true)));
			assertThrows(TinyEngineSqlIdentifierException.class, () -> provider.createTable(unsafeDefinition));
		}
	}

	private Map<String, Object> baseCrudParams() {
		Map<String, Object> params = new LinkedHashMap<String, Object>();
		params.put("tableName", "dynamic_customer");
		params.put("fields", Collections.singletonList("name"));
		params.put("conditions", Collections.<String, Object>emptyMap());
		params.put("data", Collections.<String, Object>singletonMap("name", "safe value"));
		return params;
	}
}
