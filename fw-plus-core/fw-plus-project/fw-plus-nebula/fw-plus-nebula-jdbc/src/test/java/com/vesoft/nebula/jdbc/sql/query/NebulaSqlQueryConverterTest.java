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
