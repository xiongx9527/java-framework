package com.vesoft.nebula.jdbc.mysql.command;

import com.vesoft.nebula.jdbc.mysql.protocol.MysqlCommand;
import com.vesoft.nebula.jdbc.mysql.query.MysqlQueryResult;
import com.vesoft.nebula.jdbc.mysql.query.NebulaSqlExecutionPort;
import com.vesoft.nebula.jdbc.mysql.query.ReadonlyQueryHandler;
import com.vesoft.nebula.jdbc.mysql.query.StaticMysqlMetadataHandler;
import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class MysqlCommandDispatcherTest {
	@Test
	public void shouldDispatchPingQuitQueryAndUnsupportedCommands() {
		ReadonlyQueryHandler queryHandler = new ReadonlyQueryHandler(
				new EmptyExecutionPort(),
				new StaticMysqlMetadataHandler("basketballplayer")
		);
		MysqlCommandDispatcher dispatcher = new MysqlCommandDispatcher(queryHandler);
		MysqlProtocolSession session = new MysqlProtocolSession(1L);
		session.authenticate("reader", "basketballplayer", 0);

		assertEquals(MysqlCommandResult.Type.OK, dispatcher.dispatch(session, MysqlCommand.COM_PING, null).getType());
		assertEquals(MysqlCommandResult.Type.RESULT_SET, dispatcher.dispatch(session, MysqlCommand.COM_QUERY, "select 1").getType());

		MysqlCommandResult unsupported = dispatcher.dispatch(session, MysqlCommand.COM_STMT_PREPARE, null);
		assertEquals(MysqlCommandResult.Type.ERROR, unsupported.getType());
		assertEquals(1235, unsupported.getErrorCode());

		MysqlCommandResult quit = dispatcher.dispatch(session, MysqlCommand.COM_QUIT, null);
		assertEquals(MysqlCommandResult.Type.OK, quit.getType());
		assertTrue(session.isClosed());
		assertNotNull(quit.getPayload());
	}

	private static class EmptyExecutionPort implements NebulaSqlExecutionPort {
		@Override
		public MysqlQueryResult executeQuery(com.vesoft.nebula.jdbc.mysql.query.MysqlGatewaySessionContext context, String sql) {
			return MysqlQueryResult.of(Collections.<com.vesoft.nebula.jdbc.mysql.query.MysqlColumn>emptyList(), Collections.<java.util.List<Object>>emptyList());
		}
	}
}
