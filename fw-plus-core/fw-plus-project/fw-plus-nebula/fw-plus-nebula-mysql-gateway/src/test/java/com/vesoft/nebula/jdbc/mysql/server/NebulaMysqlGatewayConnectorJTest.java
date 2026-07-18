package com.vesoft.nebula.jdbc.mysql.server;

import com.vesoft.nebula.jdbc.mysql.command.MysqlCommandDispatcher;
import com.vesoft.nebula.jdbc.mysql.auth.MysqlGatewayUser;
import com.vesoft.nebula.jdbc.mysql.auth.StaticMysqlGatewayAuthenticator;
import com.vesoft.nebula.jdbc.mysql.query.AdvancedQueryHandler;
import com.vesoft.nebula.jdbc.mysql.query.MysqlColumn;
import com.vesoft.nebula.jdbc.mysql.query.MysqlGatewaySessionContext;
import com.vesoft.nebula.jdbc.mysql.query.MysqlQueryResult;
import com.vesoft.nebula.jdbc.mysql.query.NebulaSqlExecutionPort;
import com.vesoft.nebula.jdbc.mysql.query.StaticMysqlMetadataHandler;
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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NebulaMysqlGatewayConnectorJTest {
	@Test
	public void shouldExecuteQueryAndWriteThroughJavaMysqlJdbcDriver() throws Exception {
			RecordingMutationExecutor mutationExecutor = new RecordingMutationExecutor();
			NebulaMysqlGatewayServer server = new NebulaMysqlGatewayServer(
					new NebulaMysqlGatewayServerConfig(
							"127.0.0.1",
							0,
							new StaticMysqlGatewayAuthenticator(Collections.singletonList(
									new MysqlGatewayUser("writer", "secret",
											Collections.singletonList("basketballplayer")))),
							null),
					new MysqlCommandDispatcher(new AdvancedQueryHandler(
						new StaticExecutionPort(),
						new StaticMysqlMetadataHandler("basketballplayer",
								Collections.singletonMap("person", Collections.singletonList(
										new MysqlColumn("name", "name", Types.VARCHAR, true, true)))),
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
				))
		);

		server.start();
		try {
			String url = "jdbc:mysql://127.0.0.1:" + server.getBoundPort()
					+ "/basketballplayer?useSSL=false&useServerPrepStmts=false&serverTimezone=UTC"
					+ "&connectTimeout=5000&socketTimeout=5000";
			Connection connection = DriverManager.getConnection(url, "writer", "secret");
			try {
				Statement statement = connection.createStatement();
				ResultSet database = statement.executeQuery("SELECT DATABASE()");
				assertTrue(database.next());
				assertEquals("basketballplayer", database.getString(1));

				ResultSet rows = statement.executeQuery("select name from person");
				assertTrue(rows.next());
				assertEquals("Tom", rows.getString("name"));

				assertEquals(1, statement.executeUpdate("INSERT INTO person(id, name) VALUES ('p1', 'Tom')"));

				PreparedStatement preparedStatement = connection.prepareStatement(
						"INSERT INTO person(id, name) VALUES (?, ?)");
				preparedStatement.setString(1, "p2");
				preparedStatement.setString(2, "Ada");
				assertEquals(1, preparedStatement.executeUpdate());
			} finally {
				connection.close();
			}
		} finally {
			server.stop();
		}

		assertEquals(2, mutationExecutor.mutationCalls);
	}

	private static class StaticExecutionPort implements NebulaSqlExecutionPort {
		@Override
		public MysqlQueryResult executeQuery(MysqlGatewaySessionContext context, String sql) {
			return MysqlQueryResult.of(
					Collections.singletonList(new MysqlColumn("name", "name", Types.VARCHAR, true, true)),
					Collections.<java.util.List<Object>>singletonList(Collections.<Object>singletonList("Tom"))
			);
		}
	}

	private static class RecordingMutationExecutor implements NebulaSqlMutationExecutor {
		private int mutationCalls;

		@Override
		public NebulaMutationResult executeMutation(NebulaMutationPlan plan) {
			mutationCalls++;
			return new NebulaMutationResult(true, 1, plan.getDescriptor(), null,
					Collections.<String>emptyList(), "trace-write");
		}

		@Override
		public NebulaMutationResult executeDdl(NebulaDdlPlan plan) {
			return new NebulaMutationResult(true, 0, null, plan.getDescriptor(),
					Arrays.<String>asList(), "trace-ddl");
		}
	}
}
