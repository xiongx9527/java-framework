package com.vesoft.nebula.jdbc.mysql.prepare;

import com.vesoft.nebula.jdbc.mysql.query.AdvancedQueryHandler;
import com.vesoft.nebula.jdbc.mysql.query.MysqlColumn;
import com.vesoft.nebula.jdbc.mysql.query.MysqlGatewayException;
import com.vesoft.nebula.jdbc.mysql.query.MysqlGatewaySessionContext;
import com.vesoft.nebula.jdbc.mysql.query.MysqlQueryResult;
import com.vesoft.nebula.jdbc.mysql.query.NebulaSqlExecutionPort;
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
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

public class MysqlPreparedStatementHandlerTest {

	@Test
	public void shouldPrepareExecuteSelectAndCloseStatement() throws Exception {
		RecordingExecutionPort executionPort = new RecordingExecutionPort();
		MysqlPreparedStatementHandler handler = handler(executionPort, new SuccessfulMutationExecutor());
		MysqlProtocolSession session = authenticatedSession();

		MysqlPreparedStatement prepared = handler.prepare(session, "select name from person where id = ?");
		MysqlQueryResult result = handler.execute(session, prepared.getStatementId(),
				Collections.<Object>singletonList("p1"));

		assertEquals("select name from person where id = \"p1\"", executionPort.sql);
		assertEquals(1, result.getRows().size());
		handler.close(session, prepared.getStatementId());
		assertFalse(handler.contains(session, prepared.getStatementId()));
	}

	@Test
	public void shouldPrepareExecuteWriteWithBoundParameters() throws Exception {
		SuccessfulMutationExecutor mutationExecutor = new SuccessfulMutationExecutor();
		MysqlPreparedStatementHandler handler = handler(new RecordingExecutionPort(), mutationExecutor);
		MysqlProtocolSession session = authenticatedSession();

		MysqlPreparedStatement prepared = handler.prepare(session,
				"INSERT INTO person(id, name) VALUES (?, ?)");
		MysqlQueryResult result = handler.execute(session, prepared.getStatementId(),
				Arrays.<Object>asList("p1", "Tom"));

		assertEquals(1, result.getAffectedRows());
		assertEquals("INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")",
				mutationExecutor.mutationPlan.getNgql());
	}

	@Test
	public void shouldRejectParameterCountMismatch() throws Exception {
		MysqlPreparedStatementHandler handler = handler(new RecordingExecutionPort(), new SuccessfulMutationExecutor());
		MysqlProtocolSession session = authenticatedSession();
		MysqlPreparedStatement prepared = handler.prepare(session,
				"INSERT INTO person(id, name) VALUES (?, ?)");

		try {
			handler.execute(session, prepared.getStatementId(), Collections.<Object>singletonList("p1"));
			fail("Expected parameter count error");
		} catch (MysqlGatewayException exception) {
			assertEquals(1210, exception.getErrorCode());
			assertEquals("HY000", exception.getSQLState());
		}
	}

	@Test
	public void shouldResetKeepStatementAndClearCloseRemoveStatement() throws Exception {
		MysqlPreparedStatementHandler handler = handler(new RecordingExecutionPort(), new SuccessfulMutationExecutor());
		MysqlProtocolSession session = authenticatedSession();
		MysqlPreparedStatement prepared = handler.prepare(session, "select name from person where id = ?");

		handler.reset(session, prepared.getStatementId());
		assertEquals(prepared, handler.get(session, prepared.getStatementId()));
		handler.close(session, prepared.getStatementId());
		assertFalse(handler.contains(session, prepared.getStatementId()));
	}

	private MysqlPreparedStatementHandler handler(RecordingExecutionPort executionPort,
			SuccessfulMutationExecutor mutationExecutor) {
		return new MysqlPreparedStatementHandler(new AdvancedQueryHandler(
				executionPort,
				new StaticMysqlMetadataHandler("basketballplayer"),
				new DruidNebulaSqlParser(),
				new DefaultNebulaSqlWriteConverter(),
				new DefaultNebulaSqlDdlConverter(),
				mutationExecutor,
				NebulaSchemaMappingRegistry.builder()
						.addTable(TableMapping.tag("person", "person")
								.idColumn("id", Types.VARCHAR)
								.property("name", "name", Types.VARCHAR)
								.build())
						.build()
		));
	}

	private MysqlProtocolSession authenticatedSession() {
		MysqlProtocolSession session = new MysqlProtocolSession(1L);
		session.authenticate("writer", "basketballplayer", 0);
		return session;
	}

	private static class RecordingExecutionPort implements NebulaSqlExecutionPort {
		private String sql;

		@Override
		public MysqlQueryResult executeQuery(MysqlGatewaySessionContext context, String sql) {
			this.sql = sql;
			return MysqlQueryResult.of(
					Collections.singletonList(new MysqlColumn("name", "name", Types.VARCHAR, true, true)),
					Collections.<java.util.List<Object>>singletonList(Collections.<Object>singletonList("Tom"))
			);
		}
	}

	private static class SuccessfulMutationExecutor implements NebulaSqlMutationExecutor {
		private NebulaMutationPlan mutationPlan;

		@Override
		public NebulaMutationResult executeMutation(NebulaMutationPlan plan) {
			this.mutationPlan = plan;
			return new NebulaMutationResult(true, 1, plan.getDescriptor(), null, null, "trace-write");
		}

		@Override
		public NebulaMutationResult executeDdl(NebulaDdlPlan plan) {
			return new NebulaMutationResult(true, 0, null, plan.getDescriptor(), plan.getWarnings(), "trace-ddl");
		}
	}
}
