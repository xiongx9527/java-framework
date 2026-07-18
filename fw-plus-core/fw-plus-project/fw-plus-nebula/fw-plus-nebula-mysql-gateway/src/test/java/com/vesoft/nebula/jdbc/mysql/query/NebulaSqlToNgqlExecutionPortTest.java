package com.vesoft.nebula.jdbc.mysql.query;

import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.vesoft.nebula.jdbc.sql.query.NgqlQueryPlan;
import com.vesoft.nebula.jdbc.sql.query.TableMapping;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NebulaSqlToNgqlExecutionPortTest {
	@Test
	public void shouldParseConvertAndDelegateNgqlPlan() throws Exception {
		NebulaSchemaMappingRegistry mappings = NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.property("age", "age", Types.INTEGER)
						.build())
				.build();
		RecordingNgqlExecutor executor = new RecordingNgqlExecutor();
		NebulaSqlToNgqlExecutionPort port = new NebulaSqlToNgqlExecutionPort(mappings, executor);
		MysqlGatewaySessionContext context = new MysqlGatewaySessionContext(
				9L,
				"reader",
				"basketballplayer",
				Collections.singletonList("basketballplayer"));

		MysqlQueryResult result = port.executeQuery(context,
				"SELECT * FROM person p WHERE p.id = 'p1' ORDER BY age DESC LIMIT 10");

		assertSame(context, executor.context);
		assertEquals("MATCH (p:person) WHERE id(p) == \"p1\" RETURN id(p) AS id, p.name AS name, p.age AS age ORDER BY p.age DESC LIMIT 10",
				executor.plan.getNgql());
		assertEquals(1, result.getRows().size());
	}

	@Test
	public void shouldExposeParserAndConverterFailuresAsSqlExceptions() throws Exception {
		NebulaSqlToNgqlExecutionPort port = new NebulaSqlToNgqlExecutionPort(
				NebulaSchemaMappingRegistry.builder().build(),
				new RecordingNgqlExecutor());

		try {
			port.executeQuery(new MysqlGatewaySessionContext(1L, "reader", "basketballplayer", Collections.<String>emptyList()),
					"SELECT FROM");
			fail("Expected parse failure");
		}
		catch (SQLException exception) {
			assertTrue(exception.getMessage().contains("Failed to parse SQL"));
		}
	}

	private static class RecordingNgqlExecutor implements NebulaSqlToNgqlExecutionPort.NgqlQueryExecutor {
		private MysqlGatewaySessionContext context;
		private NgqlQueryPlan plan;

		@Override
		public MysqlQueryResult execute(MysqlGatewaySessionContext context, NgqlQueryPlan plan) {
			this.context = context;
			this.plan = plan;
			return MysqlQueryResult.of(
					Collections.singletonList(new MysqlColumn("name", "name", Types.VARCHAR, true, true)),
					Collections.<java.util.List<Object>>singletonList(Collections.<Object>singletonList("Tim"))
			);
		}
	}
}
