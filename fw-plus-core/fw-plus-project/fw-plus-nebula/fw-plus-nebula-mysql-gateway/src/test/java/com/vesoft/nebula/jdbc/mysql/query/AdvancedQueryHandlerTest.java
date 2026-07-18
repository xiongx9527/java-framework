package com.vesoft.nebula.jdbc.mysql.query;

import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;
import com.vesoft.nebula.jdbc.mysql.security.MysqlGatewaySecurityPolicy;
import com.vesoft.nebula.jdbc.mysql.security.MysqlSslMode;
import com.vesoft.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.vesoft.nebula.jdbc.sql.query.TableMapping;
import com.vesoft.nebula.jdbc.sql.write.DefaultNebulaSqlDdlConverter;
import com.vesoft.nebula.jdbc.sql.write.DefaultNebulaSqlWriteConverter;
import com.vesoft.nebula.jdbc.sql.write.NebulaDdlPlan;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationDescriptor;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationPlan;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationResult;
import com.vesoft.nebula.jdbc.sql.write.NebulaSqlMutationExecutor;
import org.junit.Test;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AdvancedQueryHandlerTest {

	@Test
	public void insertShouldExecuteMutationAndReturnAffectedRows() throws Exception {
		RecordingMutationExecutor mutationExecutor = new RecordingMutationExecutor(2);
		AdvancedQueryHandler handler = advancedHandler(mutationExecutor);

		MysqlQueryResult result = handler.handle(authenticatedSession(),
				"INSERT INTO person(id, name) VALUES ('p1', 'Tom')");

		assertEquals(2, result.getAffectedRows());
		assertEquals("INSERT VERTEX person(name) VALUES \"p1\":(\"Tom\")", mutationExecutor.mutationPlan.getNgql());
		assertEquals(0, mutationExecutor.ddlCalls);
	}

	@Test
	public void createTableShouldExecuteDdlAndReturnOkResult() throws Exception {
		RecordingMutationExecutor mutationExecutor = new RecordingMutationExecutor(0);
		AdvancedQueryHandler handler = advancedHandler(mutationExecutor);

		MysqlQueryResult result = handler.handle(authenticatedSession(),
				"CREATE TABLE person(id varchar, name varchar)");

		assertEquals(0, result.getAffectedRows());
		assertEquals("CREATE TAG person(name string)", mutationExecutor.ddlPlan.getNgql());
		assertEquals(0, mutationExecutor.mutationCalls);
	}

	@Test
	public void transactionStatementShouldReturnUnsupportedErrorBeforeExecution() throws Exception {
		RecordingMutationExecutor mutationExecutor = new RecordingMutationExecutor(0);
		AdvancedQueryHandler handler = advancedHandler(mutationExecutor);

		try {
			handler.handle(authenticatedSession(), "START TRANSACTION");
			fail("Expected unsupported transaction error");
		} catch (MysqlGatewayException exception) {
			assertEquals(1196, exception.getErrorCode());
			assertEquals("HY000", exception.getSQLState());
		}
		assertEquals(0, mutationExecutor.mutationCalls);
		assertEquals(0, mutationExecutor.ddlCalls);
	}

	@Test
	public void selectShouldStillUseReadonlyExecutionPort() throws Exception {
		RecordingExecutionPort executionPort = new RecordingExecutionPort();
		AdvancedQueryHandler handler = new AdvancedQueryHandler(
				executionPort,
				new StaticMysqlMetadataHandler("basketballplayer"),
				new DruidNebulaSqlParser(),
				new DefaultNebulaSqlWriteConverter(),
				new DefaultNebulaSqlDdlConverter(),
				new RecordingMutationExecutor(1),
				personMappings()
		);

		MysqlQueryResult result = handler.handle(authenticatedSession(), "select name from person");

		assertEquals("select name from person", executionPort.sql);
		assertEquals(1, result.getRows().size());
	}

	@Test
	public void shouldRejectWriteBeforeExecutionWhenPolicyDeniesWrite() throws Exception {
		RecordingMutationExecutor mutationExecutor = new RecordingMutationExecutor(1);
		AdvancedQueryHandler handler = advancedHandler(mutationExecutor,
				MysqlGatewaySecurityPolicy.builder().allowWrite(false).build());

		try {
			handler.handle(authenticatedSession(), "INSERT INTO person(id, name) VALUES ('p1', 'Tom')");
			fail("Expected command denied error");
		} catch (MysqlGatewayException exception) {
			assertEquals(1142, exception.getErrorCode());
			assertEquals("42000", exception.getSQLState());
		}
		assertEquals(0, mutationExecutor.mutationCalls);
	}

	@Test
	public void shouldRejectDdlBeforeExecutionWhenPolicyDeniesDdl() throws Exception {
		RecordingMutationExecutor mutationExecutor = new RecordingMutationExecutor(0);
		AdvancedQueryHandler handler = advancedHandler(mutationExecutor,
				MysqlGatewaySecurityPolicy.builder().allowDdl(false).build());

		try {
			handler.handle(authenticatedSession(), "CREATE TABLE person(id varchar, name varchar)");
			fail("Expected command denied error");
		} catch (MysqlGatewayException exception) {
			assertEquals(1142, exception.getErrorCode());
			assertEquals("42000", exception.getSQLState());
		}
		assertEquals(0, mutationExecutor.ddlCalls);
	}

	@Test
	public void shouldAllowMetadataWhenPolicyAllowsMetadata() throws Exception {
		AdvancedQueryHandler handler = advancedHandler(new RecordingMutationExecutor(0),
				MysqlGatewaySecurityPolicy.builder().allowMetadata(true).build());

		MysqlQueryResult result = handler.handle(authenticatedSession(), "SHOW DATABASES");

		assertEquals(1, result.getRows().size());
	}

	@Test
	public void shouldRejectNonSslSessionWhenSslRequired() throws Exception {
		RecordingMutationExecutor mutationExecutor = new RecordingMutationExecutor(1);
		AdvancedQueryHandler handler = advancedHandler(mutationExecutor,
				MysqlGatewaySecurityPolicy.builder().sslMode(MysqlSslMode.REQUIRED).build());

		try {
			handler.handle(authenticatedSession(), "select name from person");
			fail("Expected ssl required error");
		} catch (MysqlGatewayException exception) {
			assertEquals(3159, exception.getErrorCode());
			assertEquals("HY000", exception.getSQLState());
		}
	}

	private AdvancedQueryHandler advancedHandler(RecordingMutationExecutor mutationExecutor) {
		return advancedHandler(mutationExecutor, MysqlGatewaySecurityPolicy.allowAll());
	}

	private AdvancedQueryHandler advancedHandler(RecordingMutationExecutor mutationExecutor,
			MysqlGatewaySecurityPolicy securityPolicy) {
		return new AdvancedQueryHandler(
				new RecordingExecutionPort(),
				new StaticMysqlMetadataHandler("basketballplayer"),
				new DruidNebulaSqlParser(),
				new DefaultNebulaSqlWriteConverter(),
				new DefaultNebulaSqlDdlConverter(),
				mutationExecutor,
				personMappings(),
				securityPolicy
		);
	}

	private NebulaSchemaMappingRegistry personMappings() {
		return NebulaSchemaMappingRegistry.builder()
				.addTable(TableMapping.tag("person", "person")
						.idColumn("id", Types.VARCHAR)
						.property("name", "name", Types.VARCHAR)
						.build())
				.build();
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

	private static class RecordingMutationExecutor implements NebulaSqlMutationExecutor {
		private final int affectedRows;
		private int mutationCalls;
		private int ddlCalls;
		private NebulaMutationPlan mutationPlan;
		private NebulaDdlPlan ddlPlan;

		private RecordingMutationExecutor(int affectedRows) {
			this.affectedRows = affectedRows;
		}

		@Override
		public NebulaMutationResult executeMutation(NebulaMutationPlan plan) {
			this.mutationCalls++;
			this.mutationPlan = plan;
			NebulaMutationDescriptor descriptor = plan.getDescriptor();
			assertNotNull(descriptor);
			return new NebulaMutationResult(true, affectedRows, descriptor, null, null, "trace-write");
		}

		@Override
		public NebulaMutationResult executeDdl(NebulaDdlPlan plan) throws SQLException {
			this.ddlCalls++;
			this.ddlPlan = plan;
			return new NebulaMutationResult(true, affectedRows, null, plan.getDescriptor(), plan.getWarnings(), "trace-ddl");
		}
	}
}
