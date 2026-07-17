package com.vesoft.nebula.jdbc.framework;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.sql.NebulaSqlExecutorProvider;
import com.vesoft.nebula.jdbc.statement.NebulaPreparedStatementImpl;
import com.vesoft.nebula.jdbc.test.FakeNebulaSqlExecutor;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.junit.After;
import org.junit.Test;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NebulaMyBatisContractTest {

	@After
	public void tearDown() {
		NebulaSqlExecutorProvider.reset();
	}

	@Test
	public void xmlMapperShouldBindParameterAndMapPojo() throws Exception {
		FakeNebulaSqlExecutor executor = FakeNebulaSqlExecutor.singlePlayer("Tim", 18);
		NebulaSqlExecutorProvider.setExecutor(executor);
		SqlSessionFactory sqlSessionFactory = sqlSessionFactory(mockOpenConnection());

		SqlSession session = sqlSessionFactory.openSession();
		try {
			PlayerMapper mapper = session.getMapper(PlayerMapper.class);
			PlayerRecord player = mapper.selectById(1001L);

			assertEquals("Tim", player.getPlayerName());
			assertEquals(Integer.valueOf(18), player.getAge());
			assertEquals("SELECT name AS playerName, age FROM player WHERE id = ?", normalizeSql(executor.getLastSql()));
			assertEquals(Long.valueOf(1001L), executor.getLastBindings().get(1).getValue());
		} finally {
			session.close();
		}
	}

	@Test
	public void annotationMapperShouldBindParameterAndMapResultObject() throws Exception {
		FakeNebulaSqlExecutor executor = FakeNebulaSqlExecutor.singlePlayer("Tim", 18);
		NebulaSqlExecutorProvider.setExecutor(executor);
		SqlSessionFactory sqlSessionFactory = sqlSessionFactory(mockOpenConnection());

		SqlSession session = sqlSessionFactory.openSession();
		try {
			PlayerMapper mapper = session.getMapper(PlayerMapper.class);
			PlayerRecord player = mapper.selectAnnotatedById(1001L);

			assertEquals("Tim", player.getPlayerName());
			assertEquals(Integer.valueOf(18), player.getAge());
			assertEquals(Long.valueOf(1001L), executor.getLastBindings().get(1).getValue());
		} finally {
			session.close();
		}
	}

	private SqlSessionFactory sqlSessionFactory(Connection connection) throws Exception {
		Environment environment = new Environment(
				"test",
				new JdbcTransactionFactory(),
				new SingleConnectionDataSource(connection, true)
		);
		Configuration configuration = new Configuration(environment);
		try (InputStream mapperXml = Resources.getResourceAsStream("com/vesoft/nebula/jdbc/framework/PlayerMapper.xml")) {
			XMLMapperBuilder mapperBuilder = new XMLMapperBuilder(
					mapperXml,
					configuration,
					"com/vesoft/nebula/jdbc/framework/PlayerMapper.xml",
					configuration.getSqlFragments()
			);
			mapperBuilder.parse();
		}
		return new SqlSessionFactoryBuilder().build(configuration);
	}

	private String normalizeSql(String sql) {
		return sql == null ? null : sql.replaceAll("\\s+", " ").trim();
	}

	private NebulaConnection mockOpenConnection() throws Exception {
		NebulaConnection connection = mock(NebulaConnection.class);
		when(connection.isClosed()).thenReturn(false);
		when(connection.prepareStatement(anyString())).thenAnswer(invocation -> {
			String sql = invocation.getArgument(0);
			PreparedStatement statement = new NebulaPreparedStatementImpl(connection, sql);
			return statement;
		});
		return connection;
	}
}
