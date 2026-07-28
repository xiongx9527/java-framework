package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.NebulaSqlParseResult;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.TableMapping;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NebulaSqlWriteConverterTest {

	@Test
	public void insertTagShouldRenderVertexMutationPlan() throws Exception {
		NebulaSqlParseResult parsed = new DruidNebulaSqlParser()
				.parse("INSERT INTO person(id, name) VALUES ('p1', 'Tom')");

		NebulaMutationPlan plan = new DefaultNebulaSqlWriteConverter()
				.convert(parsed.getStatement(), NebulaWriteBindings.empty(), personMappings());

		assertEquals(NebulaMutationKind.INSERT_VERTEX, plan.getMutationKind());
		assertEquals("INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")", plan.getNgql());
		assertEquals("person", plan.getDescriptor().getTargetName());
		assertEquals("p1", plan.getDescriptor().getKeys().get("id"));
		assertEquals("Tom", plan.getDescriptor().getProperties().get("name"));
		assertTrue(plan.getTrace().contains("table person -> TAG person"));
	}

	@Test
	public void insertEdgeShouldRenderEdgeMutationPlan() throws Exception {
		NebulaSqlParseResult parsed = new DruidNebulaSqlParser()
				.parse("INSERT INTO follow(src_id, dst_id, rank_id, since) VALUES ('p1', 'p2', 3, 2020)");

		NebulaMutationPlan plan = new DefaultNebulaSqlWriteConverter()
				.convert(parsed.getStatement(), NebulaWriteBindings.empty(), followMappings());

		assertEquals(NebulaMutationKind.INSERT_EDGE, plan.getMutationKind());
		assertEquals("INSERT EDGE follow(since) VALUES \"p1\"->\"p2\"@3:(2020)", plan.getNgql());
		assertEquals("p1", plan.getDescriptor().getKeys().get("src_id"));
		assertEquals("p2", plan.getDescriptor().getKeys().get("dst_id"));
		assertEquals(3, plan.getDescriptor().getKeys().get("rank_id"));
		assertEquals(2020, plan.getDescriptor().getProperties().get("since"));
	}

	@Test
	public void updateAndDeleteTagShouldRequireIdPredicate() throws Exception {
		NebulaMutationPlan update = new DefaultNebulaSqlWriteConverter()
				.convert(new DruidNebulaSqlParser()
								.parse("UPDATE person SET name = 'Jerry' WHERE id = 'p1'")
								.getStatement(),
						NebulaWriteBindings.empty(),
						personMappings());

		assertEquals(NebulaMutationKind.UPDATE_VERTEX, update.getMutationKind());
		assertEquals("UPDATE VERTEX ON person \"p1\" SET name = \"Jerry\"", update.getNgql());
		assertEquals("p1", update.getDescriptor().getKeys().get("id"));
		assertEquals("Jerry", update.getDescriptor().getProperties().get("name"));

		NebulaMutationPlan delete = new DefaultNebulaSqlWriteConverter()
				.convert(new DruidNebulaSqlParser()
								.parse("DELETE FROM person WHERE id = 'p1'")
								.getStatement(),
						NebulaWriteBindings.empty(),
						personMappings());

		assertEquals(NebulaMutationKind.DELETE_VERTEX, delete.getMutationKind());
		assertEquals("DELETE VERTEX \"p1\"", delete.getNgql());
		assertEquals("p1", delete.getDescriptor().getKeys().get("id"));
	}

	@Test
	public void updateAndDeleteEdgeShouldRequireSrcDstPredicate() throws Exception {
		NebulaMutationPlan update = new DefaultNebulaSqlWriteConverter()
				.convert(new DruidNebulaSqlParser()
								.parse("UPDATE follow SET since = 2021 WHERE src_id = 'p1' AND dst_id = 'p2'")
								.getStatement(),
						NebulaWriteBindings.empty(),
						followMappings());

		assertEquals(NebulaMutationKind.UPDATE_EDGE, update.getMutationKind());
		assertEquals("UPDATE EDGE ON follow \"p1\"->\"p2\" SET since = 2021", update.getNgql());
		assertEquals("p1", update.getDescriptor().getKeys().get("src_id"));
		assertEquals("p2", update.getDescriptor().getKeys().get("dst_id"));

		NebulaMutationPlan delete = new DefaultNebulaSqlWriteConverter()
				.convert(new DruidNebulaSqlParser()
								.parse("DELETE FROM follow WHERE src_id = 'p1' AND dst_id = 'p2'")
								.getStatement(),
						NebulaWriteBindings.empty(),
						followMappings());

		assertEquals(NebulaMutationKind.DELETE_EDGE, delete.getMutationKind());
		assertEquals("DELETE EDGE follow \"p1\"->\"p2\"", delete.getNgql());
	}

	@Test
	public void unsafeUpdateAndDeleteShouldReportStableError() throws Exception {
		assertWriteError(NebulaWriteErrorCode.NEBULA_SQL_WRITE_UNSAFE_UPDATE,
				"UPDATE person SET name = 'Jerry'");
		assertWriteError(NebulaWriteErrorCode.NEBULA_SQL_WRITE_UNSAFE_UPDATE,
				"DELETE FROM person");
	}

	private NebulaSchemaMappingRegistry personMappings() {
		return NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.build())
				.build();
	}

	private NebulaSchemaMappingRegistry followMappings() {
		return NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.edge("follow", "follow")
						.srcColumn("src_id", Types.VARCHAR)
						.dstColumn("dst_id", Types.VARCHAR)
						.rankColumn("rank_id", Types.INTEGER)
						.property("since", "since", Types.INTEGER)
						.build())
				.build();
	}

	private void assertWriteError(NebulaWriteErrorCode code, String sql) throws Exception {
		try {
			new DefaultNebulaSqlWriteConverter()
					.convert(new DruidNebulaSqlParser().parse(sql).getStatement(),
							NebulaWriteBindings.empty(),
							personMappings());
			fail("Expected NebulaWriteConversionException");
		} catch (NebulaWriteConversionException exception) {
			assertEquals(code, exception.getWriteErrorCode());
			assertTrue(exception.getMessage().contains(code.name()));
		}
	}
}
