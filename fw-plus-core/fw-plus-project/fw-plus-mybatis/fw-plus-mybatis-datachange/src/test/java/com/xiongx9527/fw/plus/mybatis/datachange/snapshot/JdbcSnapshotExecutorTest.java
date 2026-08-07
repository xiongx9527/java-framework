package com.xiongx9527.fw.plus.mybatis.datachange.snapshot;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.BoundSqlContext;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.ChangeTarget;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.H2DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.SnapshotParameter;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.SnapshotQuery;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.DataChangeScopeHolder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.Reader;
import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcSnapshotExecutorTest {

	@Test
	void bindsBoundSqlParameterThroughConfiguredTypeHandler() throws Exception {
		Configuration configuration = new Configuration();
		configuration.getTypeHandlerRegistry().register(Status.class, StatusTypeHandler.class);
		ParameterMapping mapping = new ParameterMapping.Builder(configuration, "status", Status.class).build();
		Map<String, Object> values = new HashMap<String, Object>();
		values.put("status", Status.ACTIVE);
		BoundSql boundSql = new BoundSql(configuration, "UPDATE T SET STATUS = ?",
				Collections.singletonList(mapping), values);
		PreparedStatement statement = Mockito.mock(PreparedStatement.class);

		new SnapshotParameterBinder().bind(statement,
				Collections.singletonList(SnapshotParameter.boundSql(mapping, boundSql)), configuration);

		verify(statement).setString(1, "ACTIVE");
	}

	@Test
	void readsOnlyProjectedColumnsAndPreservesCompositeKeyTypesAndOrder() throws Exception {
		try (Connection connection = h2("snapshot_rows")) {
			SnapshotQuery query = new SnapshotQuery(
					"SELECT TENANT_ID, ORDER_NO, AMOUNT FROM ORDER_LINE WHERE STATUS = ? ORDER BY ORDER_NO",
					Collections.singletonList(SnapshotParameter.value("ACTIVE")));
			JdbcTableMetadata metadata = metadata();

			List<RowSnapshot> rows = new JdbcSnapshotExecutor().query(
					connection, query, metadata, new Configuration(), 10);

			assertThat(rows).hasSize(2);
			assertThat(rows.get(0).getKeyValues().keySet()).containsExactly("TENANT_ID", "ORDER_NO");
			assertThat(rows.get(0).getKeyValues()).containsEntry("TENANT_ID", 7L)
					.containsEntry("ORDER_NO", "A-1");
			assertThat(rows.get(0).getValues()).containsOnlyKeys("TENANT_ID", "ORDER_NO", "AMOUNT");
			assertThat(rows.get(0).getValues().get("AMOUNT")).isEqualTo(new BigDecimal("12.50"));
		}
	}

	@Test
	void rejectsLimitPlusOneWithoutExposingBoundValue() throws Exception {
		try (Connection connection = h2("snapshot_limit")) {
			SnapshotQuery query = new SnapshotQuery(
					"SELECT TENANT_ID, ORDER_NO FROM ORDER_LINE WHERE STATUS <> ? ORDER BY TENANT_ID, ORDER_NO",
					Collections.singletonList(SnapshotParameter.value("secret-status")));

			assertThatThrownBy(() -> new JdbcSnapshotExecutor().query(
					connection, query, metadata(), new Configuration(), 2))
					.isInstanceOf(DataChangeException.class)
					.hasMessageContaining("SNAPSHOT_LIMIT_EXCEEDED")
					.hasMessageNotContaining("secret-status");
		}
	}

	@Test
	void closesJdbcResourcesAndSuppressesNestedAuditOnLimitFailure() throws Exception {
		Connection connection = Mockito.mock(Connection.class);
		PreparedStatement statement = Mockito.mock(PreparedStatement.class);
		ResultSet resultSet = Mockito.mock(ResultSet.class);
		ResultSetMetaData resultSetMetadata = Mockito.mock(ResultSetMetaData.class);
		when(connection.prepareStatement("SELECT ID FROM T")).thenReturn(statement);
		when(statement.executeQuery()).thenAnswer(invocation -> {
			assertThat(DataChangeScopeHolder.isSuppressed()).isTrue();
			return resultSet;
		});
		when(resultSet.next()).thenReturn(true, true);
		when(resultSet.getMetaData()).thenReturn(resultSetMetadata);
		when(resultSetMetadata.getColumnCount()).thenReturn(1);
		when(resultSetMetadata.getColumnLabel(1)).thenReturn("ID");
		when(resultSet.getObject(1)).thenReturn(1L);

		assertThatThrownBy(() -> new JdbcSnapshotExecutor().query(connection,
				new SnapshotQuery("SELECT ID FROM T", Collections.<SnapshotParameter>emptyList()),
				new JdbcTableMetadata(new TableIdentifier(null, null, "T"),
						Collections.singletonList("ID"), Collections.singletonList("ID")),
				new Configuration(), 1))
				.isInstanceOf(DataChangeException.class)
				.hasMessageContaining("SNAPSHOT_LIMIT_EXCEEDED");

		verify(resultSet).close();
		verify(statement).close();
		assertThat(DataChangeScopeHolder.isSuppressed()).isFalse();
	}

	@Test
	void sanitizesCloseFailureWithoutCauseOrSuppressedDetails() throws Exception {
		Connection connection = Mockito.mock(Connection.class);
		PreparedStatement statement = Mockito.mock(PreparedStatement.class);
		ResultSet resultSet = Mockito.mock(ResultSet.class);
		ResultSetMetaData resultSetMetadata = Mockito.mock(ResultSetMetaData.class);
		when(connection.prepareStatement("SELECT ID FROM T")).thenReturn(statement);
		when(statement.executeQuery()).thenReturn(resultSet);
		when(resultSet.next()).thenReturn(true, true);
		when(resultSet.getMetaData()).thenReturn(resultSetMetadata);
		when(resultSetMetadata.getColumnCount()).thenReturn(1);
		when(resultSetMetadata.getColumnLabel(1)).thenReturn("ID");
		when(resultSet.getObject(1)).thenReturn(1L);
		Mockito.doThrow(new SQLException("close leaked secret-value")).when(resultSet).close();

		assertThatThrownBy(() -> queryOneKey(connection, 1))
				.isInstanceOf(DataChangeException.class)
				.hasMessage("SNAPSHOT_LIMIT_EXCEEDED")
				.hasNoCause()
				.satisfies(error -> assertThat(error.getSuppressed()).isEmpty());
	}

	@Test
	void sanitizesJdbcRuntimeExceptionWithoutSensitiveDetails() throws Exception {
		Connection connection = Mockito.mock(Connection.class);
		PreparedStatement statement = Mockito.mock(PreparedStatement.class);
		when(connection.prepareStatement("SELECT ID FROM T")).thenReturn(statement);
		when(statement.executeQuery()).thenThrow(new RuntimeException("driver leaked secret-value"));

		assertThatThrownBy(() -> queryOneKey(connection, 1))
				.isInstanceOf(DataChangeException.class)
				.hasMessage("SNAPSHOT_EXECUTION_FAILED")
				.hasNoCause()
				.satisfies(error -> assertThat(error.getSuppressed()).isEmpty());
	}

	@Test
	void serviceReadsBeforeSnapshotWithOnlyPredicateParameterMappings() throws Exception {
		Configuration configuration = new Configuration();
		List<ParameterMapping> mappings = Arrays.asList(
				new ParameterMapping.Builder(configuration, "amount", BigDecimal.class).build(),
				new ParameterMapping.Builder(configuration, "orderNo", String.class).build());
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("amount", new BigDecimal("99.00"));
		parameters.put("orderNo", "A-1");
		BoundSql boundSql = new BoundSql(configuration,
				"UPDATE ORDER_LINE SET AMOUNT = ? WHERE ORDER_NO = ?", mappings, parameters);
		H2DataChangeDialect dialect = new H2DataChangeDialect();
		ChangeTarget target = dialect.analyze(new BoundSqlContext(boundSql)).get(0).getTargets().get(0)
				.withSnapshotConfiguration(Arrays.asList("TENANT_ID", "ORDER_NO"),
						Collections.singletonList("AMOUNT"), 10, false);

		try (Connection connection = h2("snapshot_service")) {
			List<RowSnapshot> rows = new SnapshotService().readBefore(
					connection, dialect, target, metadata(), configuration);

			assertThat(rows).singleElement().satisfies(row -> {
				assertThat(row.getIdentity().getKeyValues()).containsEntry("ORDER_NO", "A-1");
				assertThat(row.getValues().get("AMOUNT")).isEqualTo(new BigDecimal("12.50"));
			});
		}
	}

	private Connection h2(String name) throws Exception {
		Connection connection = DriverManager.getConnection("jdbc:h2:mem:" + name + ";DB_CLOSE_DELAY=-1");
		try (Reader schema = Resources.getResourceAsReader("datachange/h2-schema.sql")) {
			ScriptRunner runner = new ScriptRunner(connection);
			runner.setLogWriter(null);
			runner.runScript(schema);
		}
		return connection;
	}

	private List<RowSnapshot> queryOneKey(Connection connection, int maxRows) {
		return new JdbcSnapshotExecutor().query(connection,
				new SnapshotQuery("SELECT ID FROM T", Collections.<SnapshotParameter>emptyList()),
				new JdbcTableMetadata(new TableIdentifier(null, null, "T"),
						Collections.singletonList("ID"), Collections.singletonList("ID")),
				new Configuration(), maxRows);
	}

	private JdbcTableMetadata metadata() {
		return new JdbcTableMetadata(new TableIdentifier(null, "PUBLIC", "ORDER_LINE"),
				Arrays.asList("TENANT_ID", "ORDER_NO"),
				Arrays.asList("TENANT_ID", "ORDER_NO", "AMOUNT", "STATUS"));
	}

	private enum Status { ACTIVE }

	public static final class StatusTypeHandler extends BaseTypeHandler<Status> {
		@Override
		public void setNonNullParameter(PreparedStatement statement, int index, Status parameter, JdbcType jdbcType)
				throws SQLException {
			statement.setString(index, parameter.name());
		}

		@Override
		public Status getNullableResult(ResultSet resultSet, String columnName) throws SQLException { return null; }
		@Override
		public Status getNullableResult(ResultSet resultSet, int columnIndex) throws SQLException { return null; }
		@Override
		public Status getNullableResult(CallableStatement statement, int columnIndex) throws SQLException { return null; }
	}
}
