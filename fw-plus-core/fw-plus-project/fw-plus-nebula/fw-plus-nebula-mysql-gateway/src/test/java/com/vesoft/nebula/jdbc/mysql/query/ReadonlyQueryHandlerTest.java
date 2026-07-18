package com.vesoft.nebula.jdbc.mysql.query;

import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ReadonlyQueryHandlerTest {
	@Test
	public void shouldForwardSelectAndComplexReadonlySqlToExecutionPort() throws Exception {
		RecordingExecutionPort executionPort = new RecordingExecutionPort();
		ReadonlyQueryHandler handler = new ReadonlyQueryHandler(executionPort, new StaticMysqlMetadataHandler("basketballplayer"));
		MysqlProtocolSession session = authenticatedSession();

		MysqlQueryResult result = handler.handle(session, "select name from player union select name from coach");

		assertEquals("select name from player union select name from coach", executionPort.sql);
		assertEquals("basketballplayer", executionPort.context.getDatabase());
		assertEquals(1, result.getRows().size());
	}

	@Test
	public void shouldRejectWritesBeforeCallingExecutionPort() throws Exception {
		RecordingExecutionPort executionPort = new RecordingExecutionPort();
		ReadonlyQueryHandler handler = new ReadonlyQueryHandler(executionPort, new StaticMysqlMetadataHandler("basketballplayer"));

		assertRejected(handler, "insert into player(name) values('Tim')");
		assertRejected(handler, "/* comment */ update player set age = 20");
		assertRejected(handler, "start transaction");
		assertRejected(handler, "show binlog events");
		assertEquals(0, executionPort.calls);
	}

	private void assertRejected(ReadonlyQueryHandler handler, String sql) throws SQLException {
		try {
			handler.handle(authenticatedSession(), sql);
			fail("Expected read-only rejection for " + sql);
		} catch (MysqlGatewayException exception) {
			assertEquals(1290, exception.getErrorCode());
			assertEquals("HY000", exception.getSQLState());
		}
	}

	private MysqlProtocolSession authenticatedSession() {
		MysqlProtocolSession session = new MysqlProtocolSession(1L);
		session.authenticate("reader", "basketballplayer", 0);
		return session;
	}

	private static class RecordingExecutionPort implements NebulaSqlExecutionPort {
		private int calls;
		private String sql;
		private MysqlGatewaySessionContext context;

		@Override
		public MysqlQueryResult executeQuery(MysqlGatewaySessionContext context, String sql) {
			this.calls++;
			this.context = context;
			this.sql = sql;
			return MysqlQueryResult.of(
					Collections.singletonList(new MysqlColumn("name", "name", Types.VARCHAR, true, true)),
					Collections.<java.util.List<Object>>singletonList(Collections.<Object>singletonList("Tim"))
			);
		}
	}
}
