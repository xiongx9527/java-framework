package com.vesoft.nebula.jdbc.sql.query;

import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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

	@Test
	public void edgeQueryShouldRenderFiltersOrderAndPagination() throws Exception {
		NebulaSchemaMappingRegistry mappings = NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.edge("follow", "follow")
						.srcColumn("src_id", Types.VARCHAR)
						.dstColumn("dst_id", Types.VARCHAR)
						.property("since", "since", Types.INTEGER)
						.build())
				.build();
		NebulaQueryStatement statement = NebulaQueryStatement.select("follow")
				.selectAll()
				.where(QueryExpression.and(
						QueryExpression.eq(QueryExpression.column("follow", "src_id"), QueryExpression.literal("p1")),
						QueryExpression.gte(QueryExpression.column("follow", "since"), QueryExpression.literal(2020))))
				.orderBy("since", SortDirection.DESC)
				.limit(10)
				.offset(5)
				.build();

		NgqlQueryPlan plan = new DefaultNebulaSqlQueryConverter()
				.convert(statement, NebulaQueryBindings.empty(), mappings);

		assertEquals("MATCH (src)-[follow:follow]->(dst) WHERE src(follow) == \"p1\" AND follow.since >= 2020 RETURN src(follow) AS src_id, dst(follow) AS dst_id, follow.since AS since ORDER BY follow.since DESC LIMIT 5, 10", plan.getNgql());
		assertEquals(QueryKind.EDGE_QUERY, plan.getQueryKind());
		assertEquals(QueryFidelity.CONTROLLED_EQUIVALENT, plan.getFidelity());
		assertTrue(plan.getTrace().contains("table follow -> EDGE follow"));
		assertTrue(plan.getTrace().contains("pagination offset=5 limit=10"));
	}

	@Test
	public void whitelistedFunctionProjectionShouldRenderMappedNgqlExpression() throws Exception {
		NebulaSchemaMappingRegistry mappings = NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.build())
				.build();
		NebulaQueryStatement statement = NebulaQueryStatement.select("person")
				.alias("p")
				.projection(QueryExpression.function("lower", QueryExpression.column("person", "name")), "lowered_name")
				.where(QueryExpression.eq(QueryExpression.column("person", "id"), QueryExpression.literal("p1")))
				.build();

		NgqlQueryPlan plan = new DefaultNebulaSqlQueryConverter()
				.convert(statement, NebulaQueryBindings.empty(), mappings);

		assertEquals("MATCH (p:person) WHERE id(p) == \"p1\" RETURN lower(p.name) AS lowered_name", plan.getNgql());
		assertEquals(1, plan.getColumns().size());
		assertEquals("lowered_name", plan.getColumns().get(0).getColumnName());
		assertEquals(Types.VARCHAR, plan.getColumns().get(0).getJdbcType());
		assertTrue(plan.getTrace().contains("function lower -> lower"));
	}

	@Test
	public void tagEdgeTagJoinShouldRenderSingleHopRelationQuery() throws Exception {
		NebulaSchemaMappingRegistry mappings = NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.build())
				.addTable(TableMapping.edge("follow", "follow")
						.srcColumn("src_id", Types.VARCHAR)
						.dstColumn("dst_id", Types.VARCHAR)
						.property("since", "since", Types.INTEGER)
						.build())
				.build();
		NebulaQueryStatement statement = NebulaQueryStatement.tagEdgeTag(
						"person", "p", "follow", "f", "person", "friend")
				.projection(QueryExpression.column("p", "name"), "person_name")
				.projection(QueryExpression.column("f", "since"), "since")
				.projection(QueryExpression.column("friend", "name"), "friend_name")
				.where(QueryExpression.eq(QueryExpression.column("p", "id"), QueryExpression.literal("p1")))
				.build();

		NgqlQueryPlan plan = new DefaultNebulaSqlQueryConverter()
				.convert(statement, NebulaQueryBindings.empty(), mappings);

		assertEquals("MATCH (p:person)-[f:follow]->(friend:person) WHERE id(p) == \"p1\" RETURN p.name AS person_name, f.since AS since, friend.name AS friend_name", plan.getNgql());
		assertEquals(QueryKind.RELATION_QUERY, plan.getQueryKind());
		assertEquals(QueryFidelity.CONTROLLED_EQUIVALENT, plan.getFidelity());
		assertEquals(3, plan.getColumns().size());
		assertTrue(plan.getTrace().contains("join person-follow-person -> tag-edge-tag"));
	}

	@Test
	public void aggregateGroupByHavingShouldRenderWithPipeline() throws Exception {
		NebulaSchemaMappingRegistry mappings = NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("city", "city", Types.VARCHAR)
						.build())
				.build();
		NebulaQueryStatement statement = NebulaQueryStatement.select("person")
				.alias("p")
				.projection(QueryExpression.column("p", "city"), "city")
				.projection(QueryExpression.aggregate("count", QueryExpression.column("p", "id")), "person_count")
				.groupBy(QueryExpression.column("p", "city"))
				.having(QueryExpression.gte(QueryExpression.column(null, "person_count"), QueryExpression.literal(2)))
				.build();

		NgqlQueryPlan plan = new DefaultNebulaSqlQueryConverter()
				.convert(statement, NebulaQueryBindings.empty(), mappings);

		assertEquals("MATCH (p:person) WITH p.city AS city, count(id(p)) AS person_count WHERE person_count >= 2 RETURN city, person_count", plan.getNgql());
		assertEquals(QueryKind.AGGREGATE_QUERY, plan.getQueryKind());
		assertEquals(2, plan.getColumns().size());
		assertEquals(Types.BIGINT, plan.getColumns().get(1).getJdbcType());
		assertTrue(plan.getTrace().contains("aggregate count -> count"));
		assertTrue(plan.getTrace().contains("group [p.city]"));
	}

	@Test
	public void unionShouldRenderCompatibleSelectBranches() throws Exception {
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
		NebulaQueryStatement left = NebulaQueryStatement.select("person")
				.projection(QueryExpression.column("person", "name"), "name")
				.build();
		NebulaQueryStatement right = NebulaQueryStatement.select("player")
				.projection(QueryExpression.column("player", "name"), "name")
				.build();

		NgqlQueryPlan plan = new DefaultNebulaSqlQueryConverter()
				.convert(NebulaQueryStatement.union(left, right), NebulaQueryBindings.empty(), mappings);

		assertEquals("MATCH (person:person) RETURN person.name AS name UNION MATCH (player:player) RETURN player.name AS name", plan.getNgql());
		assertEquals(QueryKind.SET_QUERY, plan.getQueryKind());
		assertEquals(QueryFidelity.CONTROLLED_EQUIVALENT, plan.getFidelity());
		assertEquals(1, plan.getColumns().size());
		assertTrue(plan.getTrace().contains("union UNION"));
	}

	@Test
	public void unsupportedAndMissingMappingsShouldReportStableCodes() throws Exception {
		final NebulaSchemaMappingRegistry mappings = NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.build())
				.build();

		assertConversionError(NebulaQueryErrorCode.MISSING_COLUMN_MAPPING, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				new DefaultNebulaSqlQueryConverter().convert(
						NebulaQueryStatement.select("person")
								.selectAll()
								.where(QueryExpression.eq(
										QueryExpression.column("person", "missing"),
										QueryExpression.literal("value")))
								.build(),
						NebulaQueryBindings.empty(),
						mappings);
			}
		});

		assertConversionError(NebulaQueryErrorCode.QUERY_ONLY, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				new DefaultNebulaSqlQueryConverter().convert(
						NebulaQueryStatement.nonQuery(StatementKind.INSERT),
						NebulaQueryBindings.empty(),
						mappings);
			}
		});

		assertConversionError(NebulaQueryErrorCode.UNSUPPORTED_JOIN, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				new DefaultNebulaSqlQueryConverter().convert(
						NebulaQueryStatement.select("person")
								.selectAll()
								.unsupported(UnsupportedQueryFeature.JOIN, "JOIN condition is not graph-mappable")
								.build(),
						NebulaQueryBindings.empty(),
						mappings);
			}
		});
		assertUnsupportedFeature(mappings, UnsupportedQueryFeature.SUBQUERY, NebulaQueryErrorCode.UNSUPPORTED_SUBQUERY);
		assertUnsupportedFeature(mappings, UnsupportedQueryFeature.UNION, NebulaQueryErrorCode.UNSUPPORTED_UNION);
		assertUnsupportedFeature(mappings, UnsupportedQueryFeature.FUNCTION, NebulaQueryErrorCode.UNSUPPORTED_FUNCTION);
		assertConversionError(NebulaQueryErrorCode.UNSUPPORTED_FUNCTION, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				new DefaultNebulaSqlQueryConverter().convert(
						NebulaQueryStatement.select("person")
								.projection(QueryExpression.function("mysql_only", QueryExpression.column("person", "name")), "value")
								.build(),
						NebulaQueryBindings.empty(),
						mappings);
			}
		});
	}

	private void assertUnsupportedFeature(final NebulaSchemaMappingRegistry mappings,
										  final UnsupportedQueryFeature feature,
										  NebulaQueryErrorCode code) throws Exception {
		assertConversionError(code, new SqlRunnable() {
			@Override
			public void run() throws SQLException {
				new DefaultNebulaSqlQueryConverter().convert(
						NebulaQueryStatement.select("person")
								.selectAll()
								.unsupported(feature, feature.name() + " is not graph-mappable")
								.build(),
						NebulaQueryBindings.empty(),
						mappings);
			}
		});
	}

	private void assertConversionError(NebulaQueryErrorCode code, SqlRunnable runnable) throws Exception {
		try {
			runnable.run();
			fail("Expected NebulaQueryConversionException");
		} catch (NebulaQueryConversionException exception) {
			assertEquals(code, exception.getQueryErrorCode());
			assertTrue(exception.getMessage().contains(code.name()));
		}
	}

	private interface SqlRunnable {
		void run() throws SQLException;
	}
}
