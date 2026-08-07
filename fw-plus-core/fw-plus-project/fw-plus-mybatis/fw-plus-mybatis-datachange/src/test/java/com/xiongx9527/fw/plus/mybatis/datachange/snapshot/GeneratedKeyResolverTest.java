package com.xiongx9527.fw.plus.mybatis.datachange.snapshot;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkChangeMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.GeneratedKeyAccessor;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DatabaseMetadata;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.binding.MapperMethod;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GeneratedKeyResolverTest {
	private final GeneratedKeyResolver resolver = new GeneratedKeyResolver();

	@Test
	void prefersFrameworkWrittenKeysAndPreservesCompositeOrderAndTypes() {
		Map<String, Object> parameter = values("TENANT_ID", 7L, "ORDER_NO", "A-1");
		Statement statement = Mockito.mock(Statement.class);

		List<RowIdentity> identities = resolver.resolve(mappedStatement(), parameter, statement,
				metadata(mapAccessor()), Arrays.asList("TENANT_ID", "ORDER_NO"));

		assertThat(identities).singleElement().satisfies(identity -> {
			assertThat(identity.getKeyValues().keySet()).containsExactly("TENANT_ID", "ORDER_NO");
			assertThat(identity.getKeyValues()).containsEntry("TENANT_ID", 7L)
					.containsEntry("ORDER_NO", "A-1");
		});
		verifyNoInteractions(statement);
	}

	@Test
	void prefersExistingParameterKeyOverAccessorValue() {
		Map<String, Object> parameter = values("ID", 11L);
		GeneratedKeyAccessor conflictingAccessor = new GeneratedKeyAccessor() {
			@Override
			public Map<String, Object> readKeyValues(Object ignored, List<String> keyColumns) {
				return values("ID", 99L);
			}
		};
		Statement statement = Mockito.mock(Statement.class);

		List<RowIdentity> identities = resolver.resolve(mappedStatement(), parameter, statement,
				metadata(conflictingAccessor), Collections.singletonList("ID"));

		assertThat(identities).singleElement().satisfies(identity ->
				assertThat(identity.getKeyValues()).containsEntry("ID", 11L));
		verifyNoInteractions(statement);
	}

	@Test
	void readsFrameworkWrittenMappedStatementKeyPropertyBeforeJdbcKeys() {
		GeneratedItem parameter = new GeneratedItem();
		parameter.setId(12L);
		Statement statement = Mockito.mock(Statement.class);

		List<RowIdentity> identities = resolver.resolve(mappedStatement("id", "ID"), parameter, statement,
				metadata(null), Collections.singletonList("ID"));

		assertThat(identities).singleElement().satisfies(identity ->
				assertThat(identity.getKeyValues()).containsEntry("ID", 12L));
		verifyNoInteractions(statement);
	}

	@Test
	void readsFrameworkWrittenKeyFromNamedEntityParamMap() {
		GeneratedItem entity = new GeneratedItem();
		entity.setId(13L);
		MapperMethod.ParamMap<Object> parameters = new MapperMethod.ParamMap<Object>();
		parameters.put("entity", entity);
		parameters.put("param1", entity);
		Statement statement = Mockito.mock(Statement.class);

		List<RowIdentity> identities = resolver.resolve(mappedStatement("entity.id", "ID"), parameters, statement,
				metadata(null), Collections.singletonList("ID"));

		assertThat(identities).singleElement().satisfies(identity ->
				assertThat(identity.getKeyValues()).containsEntry("ID", 13L));
		verifyNoInteractions(statement);
	}

	@Test
	void preservesNamedBatchParamMapInputOrder() {
		GeneratedItem first = item(21L);
		GeneratedItem second = item(22L);
		List<GeneratedItem> items = Arrays.asList(first, second);
		MapperMethod.ParamMap<Object> parameters = new MapperMethod.ParamMap<Object>();
		parameters.put("items", items);
		parameters.put("param1", items);
		Statement statement = Mockito.mock(Statement.class);

		List<RowIdentity> identities = resolver.resolve(mappedStatement("items.id", "ID"), parameters, statement,
				metadata(null), Collections.singletonList("ID"));

		assertThat(identities).extracting(identity -> identity.getKeyValues().get("ID"))
				.containsExactly(21L, 22L);
		verifyNoInteractions(statement);
	}

	@Test
	void mergesExistingCompositeKeyWithPartialGeneratedKeyProperty() {
		GeneratedItem entity = item(31L);
		MapperMethod.ParamMap<Object> parameters = new MapperMethod.ParamMap<Object>();
		parameters.put("TENANT_ID", 7L);
		parameters.put("entity", entity);
		parameters.put("param1", entity);
		Statement statement = Mockito.mock(Statement.class);

		List<RowIdentity> identities = resolver.resolve(mappedStatement("entity.id", "ID"), parameters, statement,
				metadata(null), Arrays.asList("TENANT_ID", "ID"));

		assertThat(identities).singleElement().satisfies(identity -> {
			assertThat(identity.getKeyValues().keySet()).containsExactly("TENANT_ID", "ID");
			assertThat(identity.getKeyValues()).containsEntry("TENANT_ID", 7L).containsEntry("ID", 31L);
		});
		verifyNoInteractions(statement);
	}

	@Test
	void readsJdbcGeneratedKeysInInsertParameterOrder() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:generated_keys;DB_CLOSE_DELAY=-1");
			 Statement ddl = connection.createStatement()) {
			ddl.execute("CREATE TABLE GENERATED_ITEM(ID BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, NAME VARCHAR(20))");
			List<Map<String, Object>> parameters = Arrays.asList(values("NAME", "first"), values("NAME", "second"));
			try (PreparedStatement insert = connection.prepareStatement(
					"INSERT INTO GENERATED_ITEM(NAME) VALUES ('first'), ('second')", Statement.RETURN_GENERATED_KEYS)) {
				insert.executeUpdate();

				List<RowIdentity> identities = resolver.resolve(mappedStatement(), parameters, insert,
						metadata(null), Collections.singletonList("ID"));

				assertThat(identities).hasSize(2);
				assertThat(identities.get(0).getKeyValues().get("ID")).isEqualTo(1L);
				assertThat(identities.get(1).getKeyValues().get("ID")).isEqualTo(2L);
			}
		}
	}

	@Test
	void rejectsEmptyInsertParameterSequence() {
		assertThatThrownBy(() -> resolver.resolve(mappedStatement(), Collections.emptyList(),
				Mockito.mock(Statement.class), metadata(mapAccessor()), Collections.singletonList("ID")))
				.isInstanceOf(DataChangeException.class)
				.hasMessageContaining("GENERATED_KEY_UNAVAILABLE");
	}

	@Test
	void rejectsDuplicateResolvedIdentities() {
		List<Map<String, Object>> parameters = Arrays.asList(values("ID", 7L), values("ID", 7L));

		assertThatThrownBy(() -> resolver.resolve(mappedStatement(), parameters,
				Mockito.mock(Statement.class), metadata(mapAccessor()), Collections.singletonList("ID")))
				.isInstanceOf(DataChangeException.class)
				.hasMessageContaining("GENERATED_KEY_UNAVAILABLE");
	}

	@Test
	void failsClosedWhenGeneratedKeysDoNotCoverEveryInsertParameter() throws Exception {
		Statement statement = Mockito.mock(Statement.class);
		ResultSet generatedKeys = Mockito.mock(ResultSet.class);
		when(statement.getGeneratedKeys()).thenReturn(generatedKeys);
		when(generatedKeys.next()).thenReturn(false);
		List<Map<String, Object>> parameters = Collections.singletonList(values("NAME", "secret-name"));

		assertThatThrownBy(() -> resolver.resolve(mappedStatement(), parameters, statement,
				metadata(mapAccessor()), Collections.singletonList("ID")))
				.isInstanceOf(DataChangeException.class)
				.hasMessageContaining("GENERATED_KEY_UNAVAILABLE")
				.hasMessageNotContaining("secret-name");
	}

	@Test
	void fallsBackToDialectLookupAfterEmptyJdbcGeneratedKeys() throws Exception {
		Statement statement = Mockito.mock(Statement.class);
		ResultSet generatedKeys = Mockito.mock(ResultSet.class);
		when(statement.getGeneratedKeys()).thenReturn(generatedKeys);
		when(generatedKeys.next()).thenReturn(false);
		AtomicBoolean called = new AtomicBoolean();
		DataChangeDialect dialect = dialectReturning(Collections.singletonList(identity("ID", 41L)), called);

		List<RowIdentity> identities = new GeneratedKeyResolver(dialect).resolve(mappedStatement(),
				values("NAME", "hidden"), statement, metadata(null), Collections.singletonList("ID"));

		assertThat(called).isTrue();
		assertThat(identities).singleElement().satisfies(identity ->
				assertThat(identity.getKeyValues()).containsEntry("ID", 41L));
	}

	@Test
	void sanitizesGeneratedKeysCloseFailureWithoutSuppressedDetails() throws Exception {
		Statement statement = Mockito.mock(Statement.class);
		ResultSet generatedKeys = Mockito.mock(ResultSet.class);
		java.sql.ResultSetMetaData resultMetadata = Mockito.mock(java.sql.ResultSetMetaData.class);
		when(statement.getGeneratedKeys()).thenReturn(generatedKeys);
		when(generatedKeys.next()).thenReturn(true, false);
		when(generatedKeys.getMetaData()).thenReturn(resultMetadata);
		when(resultMetadata.getColumnCount()).thenReturn(1);
		when(resultMetadata.getColumnLabel(1)).thenReturn("ID");
		when(generatedKeys.getObject(1)).thenReturn(51L);
		Mockito.doThrow(new java.sql.SQLException("close leaked secret-value")).when(generatedKeys).close();

		assertThatThrownBy(() -> resolver.resolve(mappedStatement(), values("NAME", "hidden"), statement,
				metadata(null), Collections.singletonList("ID")))
				.isInstanceOf(DataChangeException.class)
				.hasMessage("GENERATED_KEY_UNAVAILABLE")
				.hasNoCause()
				.satisfies(error -> assertThat(error.getSuppressed()).isEmpty());
	}

	@Test
	void sanitizesGeneratedKeysRuntimeExceptionWithoutSensitiveDetails() throws Exception {
		Statement statement = Mockito.mock(Statement.class);
		when(statement.getGeneratedKeys()).thenThrow(new RuntimeException("driver leaked secret-value"));

		assertThatThrownBy(() -> resolver.resolve(mappedStatement(), values("NAME", "hidden"), statement,
				metadata(null), Collections.singletonList("ID")))
				.isInstanceOf(DataChangeException.class)
				.hasMessage("GENERATED_KEY_UNAVAILABLE")
				.hasNoCause()
				.satisfies(error -> assertThat(error.getSuppressed()).isEmpty());
	}

	@Test
	void sanitizesAccessorRuntimeExceptionWithoutSensitiveDetails() {
		GeneratedKeyAccessor accessor = new GeneratedKeyAccessor() {
			@Override
			public Map<String, Object> readKeyValues(Object parameterObject, List<String> keyColumns) {
				throw new RuntimeException("accessor leaked secret-value");
			}
		};

		assertThatThrownBy(() -> resolver.resolve(mappedStatement(), values("NAME", "hidden"),
				Mockito.mock(Statement.class), metadata(accessor), Collections.singletonList("ID")))
				.isInstanceOf(DataChangeException.class)
				.hasMessage("GENERATED_KEY_UNAVAILABLE")
				.hasNoCause()
				.satisfies(error -> assertThat(error.getSuppressed()).isEmpty());
	}

	@Test
	void fallsBackToJdbcGeneratedKeysWhenAccessorThrows() throws Exception {
		GeneratedKeyAccessor accessor = new GeneratedKeyAccessor() {
			@Override
			public Map<String, Object> readKeyValues(Object parameterObject, List<String> keyColumns) {
				throw new RuntimeException("accessor leaked secret-value");
			}
		};
		Statement statement = Mockito.mock(Statement.class);
		ResultSet generatedKeys = Mockito.mock(ResultSet.class);
		java.sql.ResultSetMetaData resultMetadata = Mockito.mock(java.sql.ResultSetMetaData.class);
		when(statement.getGeneratedKeys()).thenReturn(generatedKeys);
		when(generatedKeys.next()).thenReturn(true, false);
		when(generatedKeys.getMetaData()).thenReturn(resultMetadata);
		when(resultMetadata.getColumnCount()).thenReturn(1);
		when(resultMetadata.getColumnLabel(1)).thenReturn("ID");
		when(generatedKeys.getObject(1)).thenReturn(61L);

		List<RowIdentity> identities = resolver.resolve(mappedStatement(), values("NAME", "hidden"), statement,
				metadata(accessor), Collections.singletonList("ID"));

		assertThat(identities).singleElement().satisfies(identity ->
				assertThat(identity.getKeyValues()).containsEntry("ID", 61L));
	}

	private MappedStatement mappedStatement() {
		return mappedStatement(null, null);
	}

	private MappedStatement mappedStatement(String keyProperty, String keyColumn) {
		Configuration configuration = new Configuration();
		MappedStatement.Builder builder = new MappedStatement.Builder(configuration, "insertItems",
				new StaticSqlSource(configuration, "INSERT INTO GENERATED_ITEM(NAME) VALUES (?)"),
				SqlCommandType.INSERT);
		if (keyProperty != null) { builder.keyProperty(keyProperty); }
		if (keyColumn != null) { builder.keyColumn(keyColumn); }
		return builder.build();
	}

	private FrameworkChangeMetadata metadata(GeneratedKeyAccessor accessor) {
		return new FrameworkChangeMetadata(null, Collections.<String>emptyList(), null, accessor);
	}

	private GeneratedKeyAccessor mapAccessor() {
		return new GeneratedKeyAccessor() {
			@Override
			public Map<String, Object> readKeyValues(Object parameterObject, List<String> keyColumns) {
				Map<String, Object> result = new LinkedHashMap<String, Object>();
				if (!(parameterObject instanceof Map)) { return result; }
				Map<?, ?> values = (Map<?, ?>) parameterObject;
				for (String key : keyColumns) {
					if (values.containsKey(key)) { result.put(key, values.get(key)); }
				}
				return result;
			}
		};
	}

	private DataChangeDialect dialectReturning(final List<RowIdentity> identities, final AtomicBoolean called) {
		return new DataChangeDialect() {
			@Override
			public boolean supports(DatabaseMetadata metadata) { return true; }

			@Override
			public TableIdentifier normalize(TableIdentifier table) { return table; }

			@Override
			public List<RowIdentity> lookupGeneratedKeys(MappedStatement mappedStatement, Object parameterObject,
					Statement statement, FrameworkChangeMetadata frameworkMetadata, List<String> keyColumns) {
				called.set(true);
				return identities;
			}

			@Override
			public int getOrder() { return 0; }
		};
	}

	private RowIdentity identity(Object... entries) {
		return new RowIdentity(values(entries));
	}

	private Map<String, Object> values(Object... entries) {
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		for (int index = 0; index < entries.length; index += 2) {
			values.put((String) entries[index], entries[index + 1]);
		}
		return values;
	}

	private GeneratedItem item(Long id) {
		GeneratedItem item = new GeneratedItem();
		item.setId(id);
		return item;
	}

	private static final class GeneratedItem {
		private Long id;

		public Long getId() { return id; }
		public void setId(Long id) { this.id = id; }
	}
}
