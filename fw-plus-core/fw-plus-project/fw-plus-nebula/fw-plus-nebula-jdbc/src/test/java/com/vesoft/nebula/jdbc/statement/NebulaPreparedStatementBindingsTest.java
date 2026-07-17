package com.vesoft.nebula.jdbc.statement;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.test.FakeNebulaSqlExecutor;
import org.junit.Test;

import java.sql.ParameterMetaData;
import java.sql.PreparedStatement;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
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
}
