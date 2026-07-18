package com.vesoft.nebula.jdbc.mysql.command;

import com.vesoft.nebula.jdbc.mysql.protocol.MysqlCommand;
import com.vesoft.nebula.jdbc.mysql.query.AdvancedQueryHandler;
import com.vesoft.nebula.jdbc.mysql.query.MysqlQueryResult;
import com.vesoft.nebula.jdbc.mysql.query.NebulaSqlExecutionPort;
import com.vesoft.nebula.jdbc.mysql.query.ReadonlyQueryHandler;
import com.vesoft.nebula.jdbc.mysql.query.StaticMysqlMetadataHandler;
import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;
import com.vesoft.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.vesoft.nebula.jdbc.sql.query.TableMapping;
import com.vesoft.nebula.jdbc.sql.write.DefaultNebulaSqlDdlConverter;
import com.vesoft.nebula.jdbc.sql.write.DefaultNebulaSqlWriteConverter;
import com.vesoft.nebula.jdbc.sql.write.NebulaDdlPlan;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationPlan;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationResult;
import com.vesoft.nebula.jdbc.sql.write.NebulaSqlMutationExecutor;
import org.junit.Test;

import java.sql.Types;
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

	@Test
	public void shouldDispatchAdvancedQueryHandlerOkResultAsOkPacket() {
		MysqlCommandDispatcher dispatcher = new MysqlCommandDispatcher(new AdvancedQueryHandler(
				new EmptyExecutionPort(),
				new StaticMysqlMetadataHandler("basketballplayer"),
				new DruidNebulaSqlParser(),
				new DefaultNebulaSqlWriteConverter(),
				new DefaultNebulaSqlDdlConverter(),
				new SuccessfulMutationExecutor(),
				NebulaSchemaMappingRegistry.builder()
						.addTable(TableMapping.tag("person", "person")
								.idColumn("id", Types.VARCHAR)
								.property("name", "name", Types.VARCHAR)
								.build())
						.build()
		));
		MysqlProtocolSession session = new MysqlProtocolSession(1L);
		session.authenticate("writer", "basketballplayer", 0);

		MysqlCommandResult result = dispatcher.dispatch(session, MysqlCommand.COM_QUERY,
				"INSERT INTO person(id, name) VALUES ('p1', 'Tom')");

		assertEquals(MysqlCommandResult.Type.OK, result.getType());
		assertNotNull(result.getPayload());
	}

	private static class EmptyExecutionPort implements NebulaSqlExecutionPort {
		@Override
		public MysqlQueryResult executeQuery(com.vesoft.nebula.jdbc.mysql.query.MysqlGatewaySessionContext context, String sql) {
			return MysqlQueryResult.of(Collections.<com.vesoft.nebula.jdbc.mysql.query.MysqlColumn>emptyList(), Collections.<java.util.List<Object>>emptyList());
		}
	}

	private static class SuccessfulMutationExecutor implements NebulaSqlMutationExecutor {
		@Override
		public NebulaMutationResult executeMutation(NebulaMutationPlan plan) {
			return new NebulaMutationResult(true, 1, plan.getDescriptor(), null, null, "trace-write");
		}

		@Override
		public NebulaMutationResult executeDdl(NebulaDdlPlan plan) {
			return new NebulaMutationResult(true, 0, null, plan.getDescriptor(), plan.getWarnings(), "trace-ddl");
		}
	}
}
