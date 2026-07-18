package com.vesoft.nebula.jdbc.statement;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.sql.NebulaParameterBindings;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutor;
import com.vesoft.nebula.jdbc.test.FakeNebulaSqlExecutor;
import org.junit.Test;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

public class NebulaPreparedStatementBindingsTest {

	@Test
	public void executeQueryShouldPassTypedParameterSnapshotToExecutor() throws Exception {
		FakeNebulaSqlExecutor executor = FakeNebulaSqlExecutor.singleString("Tim");
		NebulaConnection connection = mock(NebulaConnection.class);
		NebulaPreparedStatementImpl statement = new NebulaPreparedStatementImpl(
				connection,
				"SELECT * FROM player WHERE id = ? AND active = ?",
				executor
		);

		statement.setLong(1, 1001L);
		statement.setBoolean(2, true);
		statement.executeQuery();
		statement.setLong(1, 2002L);

		assertEquals("SELECT * FROM player WHERE id = ? AND active = ?", executor.getLastSql());
		assertEquals(Long.valueOf(1001L), executor.getLastBindings().get(1).getValue());
		assertEquals(Types.BIGINT, executor.getLastBindings().get(1).getJdbcType());
		assertEquals(Boolean.TRUE, executor.getLastBindings().get(2).getValue());
		assertEquals(Types.BOOLEAN, executor.getLastBindings().get(2).getJdbcType());
	}

	@Test
	public void setObjectShouldFallbackToJavaObjectForUnknownTypes() throws Exception {
		FakeNebulaSqlExecutor executor = FakeNebulaSqlExecutor.singleString("Tim");
		Object value = new Object();
		PreparedStatement statement = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"SELECT * FROM player WHERE value = ?",
				executor
		);

		statement.setObject(1, value);
		statement.executeQuery();

		assertSame(value, executor.getLastBindings().get(1).getValue());
		assertEquals(Types.JAVA_OBJECT, executor.getLastBindings().get(1).getJdbcType());
	}

	@Test
	public void setObjectShouldBindCommonScalarTypes() throws Exception {
		FakeNebulaSqlExecutor executor = FakeNebulaSqlExecutor.singleString("Tim");
		BigDecimal amount = new BigDecimal("12.50");
		PreparedStatement statement = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"SELECT * FROM player WHERE amount = ? AND rank = ? AND score = ?",
				executor
		);

		statement.setObject(1, amount);
		statement.setObject(2, Short.valueOf((short) 7));
		statement.setObject(3, Float.valueOf(3.5f));
		statement.executeQuery();

		assertEquals(amount, executor.getLastBindings().get(1).getValue());
		assertEquals(Types.DECIMAL, executor.getLastBindings().get(1).getJdbcType());
		assertEquals(Short.valueOf((short) 7), executor.getLastBindings().get(2).getValue());
		assertEquals(Types.SMALLINT, executor.getLastBindings().get(2).getJdbcType());
		assertEquals(Float.valueOf(3.5f), executor.getLastBindings().get(3).getValue());
		assertEquals(Types.FLOAT, executor.getLastBindings().get(3).getJdbcType());
	}

	@Test
	public void directScalarSettersShouldBindCommonFrameworkTypes() throws Exception {
		FakeNebulaSqlExecutor executor = FakeNebulaSqlExecutor.singleString("Tim");
		BigDecimal amount = new BigDecimal("12.50");
		PreparedStatement statement = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"SELECT * FROM player WHERE b = ? AND s = ? AND f = ? AND amount = ?",
				executor
		);

		statement.setByte(1, (byte) 2);
		statement.setShort(2, (short) 7);
		statement.setFloat(3, 3.5f);
		statement.setBigDecimal(4, amount);
		statement.executeQuery();

		assertEquals(Byte.valueOf((byte) 2), executor.getLastBindings().get(1).getValue());
		assertEquals(Types.TINYINT, executor.getLastBindings().get(1).getJdbcType());
		assertEquals(Short.valueOf((short) 7), executor.getLastBindings().get(2).getValue());
		assertEquals(Types.SMALLINT, executor.getLastBindings().get(2).getJdbcType());
		assertEquals(Float.valueOf(3.5f), executor.getLastBindings().get(3).getValue());
		assertEquals(Types.FLOAT, executor.getLastBindings().get(3).getJdbcType());
		assertEquals(amount, executor.getLastBindings().get(4).getValue());
		assertEquals(Types.DECIMAL, executor.getLastBindings().get(4).getJdbcType());
	}

	@Test
	public void setDatetimeShouldBindTypedParameterSnapshot() throws Exception {
		FakeNebulaSqlExecutor executor = FakeNebulaSqlExecutor.singleString("Tim");
		java.util.Date datetime = new java.util.Date(123456789L);
		NebulaPreparedStatementImpl statement = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"SELECT * FROM player WHERE created_at = ?",
				executor
		);

		statement.setDatetime(1, datetime);
		statement.executeQuery();

		assertEquals(datetime, executor.getLastBindings().get(1).getValue());
		assertEquals(Types.TIMESTAMP, executor.getLastBindings().get(1).getJdbcType());
	}

	@Test
	public void parameterMetaDataShouldExposeBoundTypesAndUnboundDefaults() throws Exception {
		PreparedStatement statement = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"SELECT * FROM player WHERE id = ? AND active = ?"
		);

		statement.setLong(1, 1001L);
		ParameterMetaData metaData = statement.getParameterMetaData();

		assertEquals(2, metaData.getParameterCount());
		assertEquals(Types.BIGINT, metaData.getParameterType(1));
		assertEquals(Types.JAVA_OBJECT, metaData.getParameterType(2));
		assertEquals(Long.class.getName(), metaData.getParameterClassName(1));
		assertEquals(Object.class.getName(), metaData.getParameterClassName(2));
		assertEquals(ParameterMetaData.parameterModeIn, metaData.getParameterMode(1));
		assertEquals(ParameterMetaData.parameterNullableUnknown, metaData.isNullable(1));
	}

	@Test
	public void executeShouldDispatchQueryAndUpdateSqlWithBindings() throws Exception {
		RecordingExecutor queryExecutor = new RecordingExecutor();
		PreparedStatement query = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"SELECT name FROM player WHERE id = ?",
				queryExecutor
		);
		query.setLong(1, 1001L);

		assertTrue(query.execute());
		assertEquals("SELECT name FROM player WHERE id = ?", queryExecutor.sql);
		assertEquals(Long.valueOf(1001L), queryExecutor.bindings.get(1).getValue());

		RecordingExecutor updateExecutor = new RecordingExecutor();
		PreparedStatement update = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"INSERT VERTEX player(name) VALUES ?:(?)",
				updateExecutor
		);
		update.setLong(1, 1001L);
		update.setString(2, "Tim");

		assertFalse(update.execute());
		assertEquals("INSERT VERTEX player(name) VALUES ?:(?)", updateExecutor.sql);
		assertEquals(Long.valueOf(1001L), updateExecutor.bindings.get(1).getValue());
		assertEquals("Tim", updateExecutor.bindings.get(2).getValue());
	}

	@Test
	public void placeholderCountingShouldIgnoreQuotedIdentifiersLiteralsAndComments() throws Exception {
		FakeNebulaSqlExecutor executor = FakeNebulaSqlExecutor.singleString("Tim");
		PreparedStatement statement = new NebulaPreparedStatementImpl(
				mock(NebulaConnection.class),
				"SELECT '?' AS marker, \"?\" AS double_marker, `?column` FROM player -- ? line comment\n"
						+ "WHERE id = ? AND active = ?",
				executor
		);

		statement.setLong(1, 1001L);
		statement.setBoolean(2, true);
		statement.executeQuery();

		assertEquals(2, executor.getLastBindings().size());
		assertEquals(Long.valueOf(1001L), executor.getLastBindings().get(1).getValue());
		assertEquals(Boolean.TRUE, executor.getLastBindings().get(2).getValue());
	}

	private static class RecordingExecutor implements NebulaSqlExecutor {
		private final ResultSet resultSet = mock(ResultSet.class);
		private String sql;
		private NebulaParameterBindings bindings;

		@Override
		public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) {
			this.sql = sql;
			this.bindings = bindings;
			return resultSet;
		}

		@Override
		public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
			this.sql = sql;
			this.bindings = bindings;
			return 1;
		}
	}
}
