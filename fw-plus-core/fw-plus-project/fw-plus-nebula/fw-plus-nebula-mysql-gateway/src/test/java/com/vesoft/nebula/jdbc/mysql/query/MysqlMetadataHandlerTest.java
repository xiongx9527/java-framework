package com.vesoft.nebula.jdbc.mysql.query;

import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;
import org.junit.Test;

import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class MysqlMetadataHandlerTest {
	@Test
	public void shouldReturnDatabasesTablesAndDescribeRows() throws Exception {
		StaticMysqlMetadataHandler handler = new StaticMysqlMetadataHandler(
				"basketballplayer",
				Collections.singletonMap("player", Arrays.asList(
						new MysqlColumn("name", "name", Types.VARCHAR, true, true),
						new MysqlColumn("age", "age", Types.INTEGER, true, true)
				))
		);
		MysqlProtocolSession session = session();

		assertEquals("basketballplayer", handler.showDatabases(session).getRows().get(0).get(0));
		assertEquals("player", handler.showTables(session).getRows().get(0).get(0));
		assertEquals("name", handler.describe(session, "player").getRows().get(0).get(0));
		assertEquals("varchar", handler.describe(session, "player").getRows().get(0).get(1));
		assertEquals("int", handler.describe(session, "player").getRows().get(1).get(1));
	}

	@Test
	public void shouldReturnMysqlTableNotFoundError() throws Exception {
		StaticMysqlMetadataHandler handler = new StaticMysqlMetadataHandler("basketballplayer");

		try {
			handler.describe(session(), "missing");
			fail("Expected missing table error");
		} catch (MysqlGatewayException exception) {
			assertEquals(1146, exception.getErrorCode());
			assertEquals("42S02", exception.getSQLState());
		}
	}

	@Test
	public void shouldReturnVariablesDatabaseAndInformationSchemaRows() throws Exception {
		StaticMysqlMetadataHandler handler = new StaticMysqlMetadataHandler(
				"basketballplayer",
				Collections.singletonMap("player", Arrays.asList(
						new MysqlColumn("name", "name", Types.VARCHAR, true, true),
						new MysqlColumn("age", "age", Types.INTEGER, true, true)
				))
		);
		MysqlProtocolSession session = session();

		assertEquals("version", handler.showVariables(session).getRows().get(0).get(0));
		assertEquals("basketballplayer", handler.selectDatabase(session).getRows().get(0).get(0));
		assertEquals("player", handler.informationSchemaTables(session).getRows().get(0).get(2));
		assertEquals("name", handler.informationSchemaColumns(session, "player").getRows().get(0).get(3));
		assertEquals("varchar", handler.informationSchemaColumns(session, "player").getRows().get(0).get(7));
	}

	private MysqlProtocolSession session() {
		MysqlProtocolSession session = new MysqlProtocolSession(1L);
		session.authenticate("reader", "basketballplayer", 0);
		return session;
	}
}
