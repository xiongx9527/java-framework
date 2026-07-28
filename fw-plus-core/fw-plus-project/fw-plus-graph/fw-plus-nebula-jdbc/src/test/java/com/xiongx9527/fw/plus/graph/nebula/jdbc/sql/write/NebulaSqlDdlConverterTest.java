package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.TableMapping;
import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class NebulaSqlDdlConverterTest {

	@Test
	public void createDropAndDescribeTagShouldRenderSchemaPlans() throws Exception {
		DefaultNebulaSqlDdlConverter converter = new DefaultNebulaSqlDdlConverter();

		NebulaDdlPlan create = converter.convert(new DruidNebulaSqlParser()
						.parse("CREATE TABLE person (id VARCHAR(64) PRIMARY KEY, name VARCHAR(64))")
						.getStatement(),
				personMappings());
		assertEquals(NebulaDdlKind.CREATE_TAG, create.getKind());
		assertEquals("CREATE TAG person(name string)", create.getNgql());
		assertEquals("person", create.getDescriptor().getSchemaName());

		NebulaDdlPlan drop = converter.convert(new DruidNebulaSqlParser()
						.parse("DROP TABLE person")
						.getStatement(),
				personMappings());
		assertEquals(NebulaDdlKind.DROP_TAG, drop.getKind());
		assertEquals("DROP TAG person", drop.getNgql());

		NebulaDdlPlan describe = converter.convert(new DruidNebulaSqlParser()
						.parse("DESCRIBE person")
						.getStatement(),
				personMappings());
		assertEquals(NebulaDdlKind.DESCRIBE_TAG, describe.getKind());
		assertEquals("DESCRIBE TAG person", describe.getNgql());
	}

	@Test
	public void createEdgeShouldRenderOnlyPropertyColumns() throws Exception {
		NebulaDdlPlan create = new DefaultNebulaSqlDdlConverter()
				.convert(new DruidNebulaSqlParser()
								.parse("CREATE TABLE follow (src_id VARCHAR(64), dst_id VARCHAR(64), since INT)")
								.getStatement(),
						followMappings());

		assertEquals(NebulaDdlKind.CREATE_EDGE, create.getKind());
		assertEquals("CREATE EDGE follow(since int64)", create.getNgql());
	}

	@Test
	public void unsupportedDdlShouldReportStableError() throws Exception {
		assertDdlError("CREATE TABLE person (id VARCHAR(64) PRIMARY KEY, name VARCHAR(64)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
		assertDdlError("CREATE TABLE person (id VARCHAR(64) PRIMARY KEY, name VARCHAR(64), KEY idx_name(name))");
		assertDdlError("SHOW TABLES");
	}

	private void assertDdlError(String sql) throws Exception {
		try {
			new DefaultNebulaSqlDdlConverter()
					.convert(new DruidNebulaSqlParser().parse(sql).getStatement(), personMappings());
			fail("Expected NebulaWriteConversionException");
		} catch (NebulaWriteConversionException exception) {
			assertEquals(NebulaWriteErrorCode.NEBULA_SQL_DDL_UNSUPPORTED_OPTION, exception.getWriteErrorCode());
			assertTrue(exception.getMessage().contains(NebulaWriteErrorCode.NEBULA_SQL_DDL_UNSUPPORTED_OPTION.name()));
		}
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
						.property("since", "since", Types.INTEGER)
						.build())
				.build();
	}
}
