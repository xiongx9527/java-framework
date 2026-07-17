package com.vesoft.nebula.jdbc.mysql.query;

import org.junit.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class MysqlQueryResultTest {
	@Test
	public void shouldKeepColumnDefinitionsAndNullRowValues() {
		MysqlColumn column = new MysqlColumn("name", "person_name", Types.VARCHAR, true, true);
		MysqlQueryResult result = MysqlQueryResult.of(
				Collections.singletonList(column),
				Collections.singletonList(Collections.<Object>singletonList(null))
		);

		assertEquals(1, result.getColumns().size());
		assertEquals("name", result.getColumns().get(0).getLabel());
		assertEquals("person_name", result.getColumns().get(0).getName());
		assertEquals(Types.VARCHAR, result.getColumns().get(0).getJdbcType());
		assertEquals(1, result.getRows().size());
		assertNull(result.getRows().get(0).get(0));
	}

	@Test
	public void shouldExposeSessionContextForExecutionPorts() {
		MysqlGatewaySessionContext context = new MysqlGatewaySessionContext(
				7L,
				"reader",
				"basketballplayer",
				Arrays.asList("basketballplayer", "demo")
		);

		assertEquals(7L, context.getConnectionId());
		assertEquals("reader", context.getUsername());
		assertEquals("basketballplayer", context.getDatabase());
		assertEquals(Arrays.asList("basketballplayer", "demo"), context.getAllowedDatabases());
	}
}
