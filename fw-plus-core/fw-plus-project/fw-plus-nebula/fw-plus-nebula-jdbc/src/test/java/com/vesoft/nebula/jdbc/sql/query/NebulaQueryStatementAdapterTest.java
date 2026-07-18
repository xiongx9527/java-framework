package com.vesoft.nebula.jdbc.sql.query;

import com.vesoft.nebula.jdbc.sql.parser.NebulaSqlParseResult;
import com.vesoft.nebula.jdbc.sql.parser.NebulaSqlParser;
import com.vesoft.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NebulaQueryStatementAdapterTest {
	private final NebulaSqlParser parser = new DruidNebulaSqlParser();
	private final NebulaQueryStatementAdapter adapter = new NebulaQueryStatementAdapter();

	@Test
	public void adapterShouldConvertParserSelectToNgqlPlan() throws Exception {
		NebulaSchemaMappingRegistry mappings = NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.property("age", "age", Types.INTEGER)
						.build())
				.build();
		NebulaSqlParseResult parseResult = parser.parse(
				"SELECT * FROM person p WHERE p.id = 'p1' AND p.age >= 18 ORDER BY age DESC LIMIT 5, 10");

		NebulaQueryStatement statement = adapter.adapt(parseResult);
		NgqlQueryPlan plan = new DefaultNebulaSqlQueryConverter()
				.convert(statement, NebulaQueryBindings.empty(), mappings);

		assertEquals("MATCH (p:person) WHERE id(p) == \"p1\" AND p.age >= 18 RETURN id(p) AS id, p.name AS name, p.age AS age ORDER BY p.age DESC LIMIT 5, 10", plan.getNgql());
		assertEquals(QueryKind.VERTEX_QUERY, plan.getQueryKind());
		assertEquals(3, plan.getColumns().size());
	}

	@Test
	public void adapterShouldConvertWhitelistedFunctionProjection() throws Exception {
		NebulaSchemaMappingRegistry mappings = NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.build())
				.build();
		NebulaSqlParseResult parseResult = parser.parse(
				"SELECT lower(p.name) AS lowered_name FROM person p WHERE p.id = 'p1'");

		NebulaQueryStatement statement = adapter.adapt(parseResult);
		NgqlQueryPlan plan = new DefaultNebulaSqlQueryConverter()
				.convert(statement, NebulaQueryBindings.empty(), mappings);

		assertEquals("MATCH (p:person) WHERE id(p) == \"p1\" RETURN lower(p.name) AS lowered_name", plan.getNgql());
		assertEquals(1, plan.getColumns().size());
		assertEquals("lowered_name", plan.getColumns().get(0).getLabel());
	}

	@Test
	public void adapterShouldConvertAggregateGroupByAndHaving() throws Exception {
		NebulaSchemaMappingRegistry mappings = NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("city", "city", Types.VARCHAR)
						.build())
				.build();
		NebulaSqlParseResult parseResult = parser.parse(
				"SELECT p.city AS city, COUNT(p.id) AS person_count FROM person p GROUP BY p.city HAVING person_count >= 2");

		NebulaQueryStatement statement = adapter.adapt(parseResult);
		NgqlQueryPlan plan = new DefaultNebulaSqlQueryConverter()
				.convert(statement, NebulaQueryBindings.empty(), mappings);

		assertEquals("MATCH (p:person) WITH p.city AS city, count(id(p)) AS person_count WHERE person_count >= 2 RETURN city, person_count", plan.getNgql());
		assertEquals(QueryKind.AGGREGATE_QUERY, plan.getQueryKind());
		assertEquals(2, plan.getColumns().size());
	}

	@Test
	public void adapterShouldConvertCompatibleUnionBranches() throws Exception {
		NebulaSchemaMappingRegistry mappings = NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.build())
				.addTable(TableMapping.tag("player", "player")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.build())
				.build();
		NebulaSqlParseResult parseResult = parser.parse(
				"SELECT name AS name FROM person UNION SELECT name AS name FROM player");

		NebulaQueryStatement statement = adapter.adapt(parseResult);
		NgqlQueryPlan plan = new DefaultNebulaSqlQueryConverter()
				.convert(statement, NebulaQueryBindings.empty(), mappings);

		assertEquals("MATCH (person:person) RETURN person.name AS name UNION MATCH (player:player) RETURN player.name AS name", plan.getNgql());
		assertEquals(QueryKind.SET_QUERY, plan.getQueryKind());
		assertEquals(1, plan.getColumns().size());
	}

	@Test
	public void adapterShouldPreserveQueryOnlyBoundaryForNonSelectParserResults() throws Exception {
		NebulaSqlParseResult parseResult = parser.parse("INSERT INTO person(id, name) VALUES ('p1', 'Tom')");
		NebulaQueryStatement statement = adapter.adapt(parseResult);

		try {
			new DefaultNebulaSqlQueryConverter().convert(
					statement,
					NebulaQueryBindings.empty(),
					NebulaSchemaMappingRegistry.builder().build());
			fail("Expected query-only conversion failure");
		}
		catch (NebulaQueryConversionException exception) {
			assertEquals(NebulaQueryErrorCode.QUERY_ONLY, exception.getQueryErrorCode());
		}
	}

	@Test
	public void adapterShouldReportComplexParserFeaturesWithStableCodes() throws Exception {
		assertUnsupported("SELECT p.id FROM person p JOIN follow f ON p.id = f.src_id",
				NebulaQueryErrorCode.UNSUPPORTED_JOIN);
		assertUnsupported("SELECT t.id FROM (SELECT id FROM person) t",
				NebulaQueryErrorCode.UNSUPPORTED_SUBQUERY);
	}

	private void assertUnsupported(String sql, NebulaQueryErrorCode expectedCode) throws Exception {
		NebulaQueryStatement statement = adapter.adapt(parser.parse(sql));
		try {
			new DefaultNebulaSqlQueryConverter().convert(
					statement,
					NebulaQueryBindings.empty(),
					NebulaSchemaMappingRegistry.builder()
							.addTable(TableMapping.tag("person", "person").idColumn("id", Types.VARCHAR).build())
							.build());
			fail("Expected unsupported conversion failure");
		}
		catch (NebulaQueryConversionException exception) {
			assertEquals(expectedCode, exception.getQueryErrorCode());
			assertTrue(exception.getMessage().contains(expectedCode.name()));
		}
	}
}
