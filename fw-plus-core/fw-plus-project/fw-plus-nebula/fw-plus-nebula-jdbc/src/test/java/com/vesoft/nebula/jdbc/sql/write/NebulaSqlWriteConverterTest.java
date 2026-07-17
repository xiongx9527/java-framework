package com.vesoft.nebula.jdbc.sql.write;

import com.vesoft.nebula.jdbc.sql.parser.NebulaSqlParseResult;
import com.vesoft.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.vesoft.nebula.jdbc.sql.query.TableMapping;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
}
