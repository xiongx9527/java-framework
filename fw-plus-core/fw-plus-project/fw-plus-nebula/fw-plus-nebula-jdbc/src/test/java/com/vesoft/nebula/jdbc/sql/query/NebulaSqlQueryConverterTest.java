package com.vesoft.nebula.jdbc.sql.query;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NebulaSqlQueryConverterTest {

	@Test
	public void tagPointQueryShouldRenderPlanColumnsAndTrace() throws Exception {
		NebulaSchemaMappingRegistry mappings = NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.property("age", "age", Types.INTEGER)
						.build())
				.build();
		NebulaQueryStatement statement = NebulaQueryStatement.select("person")
				.selectAll()
				.where(QueryExpression.eq(
						QueryExpression.column("person", "id"),
						QueryExpression.literal("p1")))
				.build();

		NgqlQueryPlan plan = new DefaultNebulaSqlQueryConverter()
				.convert(statement, NebulaQueryBindings.empty(), mappings);

		assertEquals("MATCH (person:person) WHERE id(person) == \"p1\" RETURN id(person) AS id, person.name AS name, person.age AS age", plan.getNgql());
		assertEquals(QueryKind.VERTEX_QUERY, plan.getQueryKind());
		assertEquals(QueryFidelity.EQUIVALENT, plan.getFidelity());
		assertEquals(3, plan.getColumns().size());
		assertEquals("id", plan.getColumns().get(0).getColumnName());
		assertEquals(Types.VARCHAR, plan.getColumns().get(0).getJdbcType());
		assertTrue(plan.getTrace().contains("table person -> TAG person"));
		assertTrue(plan.getTrace().contains("filter id(person) == \"p1\""));
	}
}
