package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapterRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.MybatisDataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.api.StatementChange;
import com.xiongx9527.fw.plus.mybatis.datachange.config.DataChangeConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitorRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.BoundSqlContext;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialectRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.H2DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.diff.ChangeDiffEngine;
import com.xiongx9527.fw.plus.mybatis.datachange.dispatch.DataChangeDispatcher;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.JdbcTableMetadataResolver;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.SnapshotService;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataChangeMultiStatementTest {
	private static final AtomicInteger DATABASE_SEQUENCE = new AtomicInteger();

	private JdbcDataSource dataSource;

	@BeforeEach
	void setUp() throws Exception {
		dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:datachange_multi_" + DATABASE_SEQUENCE.incrementAndGet()
				+ ";DB_CLOSE_DELAY=-1");
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY, name VARCHAR(64))");
			statement.execute("INSERT INTO sys_user(id, name) VALUES(1, 'first-before'), (2, 'second-before')");
		}
	}

	@Test
	void multiStatementKeepsIndexesAndJdbcUpdateCounts() throws Exception {
		String sql = "UPDATE sys_user SET name='first-after' WHERE id=1; "
				+ "UPDATE sys_user SET name='second-after' WHERE id=2";

		List<StatementChange> changes = execute(sql, updateCounts(1, 1), new DatabaseMutation() {
			@Override public void run(Statement statement) throws Exception {
				statement.executeUpdate("UPDATE sys_user SET name='first-after' WHERE id=1");
				statement.executeUpdate("UPDATE sys_user SET name='second-after' WHERE id=2");
			}
		});

		assertThat(changes).extracting(StatementChange::getStatementIndex).containsExactly(0, 1);
		assertThat(changes).extracting(StatementChange::getAffectedRows).containsExactly(1, 1);
	}

	@Test
	void successNoInfoRemainsUnknownInsteadOfGuessingRows() throws Exception {
		List<StatementChange> changes = execute("UPDATE sys_user SET name='after' WHERE id=1",
				updateCounts(Statement.SUCCESS_NO_INFO), new DatabaseMutation() {
					@Override public void run(Statement statement) throws Exception {
						statement.executeUpdate("UPDATE sys_user SET name='after' WHERE id=1");
					}
				});

		assertThat(changes).singleElement()
				.extracting(StatementChange::getAffectedRows).isEqualTo(Statement.SUCCESS_NO_INFO);
	}

	@Test
	void resultSetBeforeDmlKeepsTheDmlStatementIndexAndCount() throws Exception {
		Statement jdbcStatement = mock(Statement.class);
		when(jdbcStatement.getResultSet()).thenReturn(mock(ResultSet.class));
		when(jdbcStatement.getMoreResults()).thenReturn(false, false);
		when(jdbcStatement.getUpdateCount()).thenReturn(1, -1);
		List<JdbcStatementResult> results = new JdbcUpdateCountCollector().collect(jdbcStatement, -1);

		List<StatementChange> changes = execute("SELECT name FROM sys_user WHERE id=1; "
				+ "UPDATE sys_user SET name='after' WHERE id=1", results, new DatabaseMutation() {
					@Override public void run(Statement statement) throws Exception {
						statement.executeUpdate("UPDATE sys_user SET name='after' WHERE id=1");
					}
				});

		assertThat(changes).singleElement().satisfies(change -> {
			assertThat(change.getStatementIndex()).isEqualTo(1);
			assertThat(change.getAffectedRows()).isEqualTo(1);
		});
	}

	@Test
	void returningResultSetSharesStatementIndexWithFollowingUpdateCount() throws Exception {
		List<JdbcStatementResult> results = Arrays.asList(JdbcStatementResult.resultSet(),
				JdbcStatementResult.updateCount(1));

		List<StatementChange> changes = execute(
				"UPDATE sys_user SET name='after' WHERE id=1 RETURNING id", results,
				new DatabaseMutation() {
					@Override public void run(Statement statement) throws Exception {
						statement.executeUpdate("UPDATE sys_user SET name='after' WHERE id=1");
					}
				});

		assertThat(changes).singleElement().satisfies(change -> {
			assertThat(change.getStatementIndex()).isZero();
			assertThat(change.getAffectedRows()).isEqualTo(1);
		});
	}

	private List<StatementChange> execute(String sql, List<JdbcStatementResult> results, DatabaseMutation mutation)
				throws Exception {
		DataChangeConfiguration properties = configuration();
		DataChangeFrameworkAdapter adapter = new MybatisDataChangeFrameworkAdapter();
		DataChangeEngine engine = new DataChangeEngine(properties,
				new TableMonitorRegistry(properties.getTables()),
				new DataChangeFrameworkAdapterRegistry(Collections.singletonList(adapter)),
				new DataChangeDialectRegistry(Collections.<DataChangeDialect>singletonList(new H2DataChangeDialect())),
				new JdbcTableMetadataResolver(), new SnapshotService(), new ChangeDiffEngine(),
				new DataChangeDispatcher(Collections.emptyList(), null), null);
		Configuration mybatis = new Configuration();
		MappedStatement mappedStatement = new MappedStatement.Builder(mybatis, "multiStatement",
				new StaticSqlSource(mybatis, sql), SqlCommandType.UPDATE).build();
		DataChangeExecutionContext context = new DataChangeExecutionContext(mappedStatement, null, adapter);
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			engine.initializeConnection(context, connection);
			List<StatementChangePlan> plans = engine.beforeStatement(context,
					new BoundSqlContext(mappedStatement.getBoundSql(null)));
			mutation.run(statement);
			engine.captureStatementResults(context, plans, mock(Statement.class), results);
			return engine.afterStatement(context, plans, results);
		}
	}

	private List<JdbcStatementResult> updateCounts(int... counts) {
		List<JdbcStatementResult> results = new java.util.ArrayList<JdbcStatementResult>();
		for (int count : counts) { results.add(JdbcStatementResult.updateCount(count)); }
		return results;
	}

	private DataChangeConfiguration configuration() {
		TableMonitor monitor = new TableMonitor();
		monitor.setName("SYS_USER");
		monitor.setKeyColumns(Collections.singletonList("ID"));
		monitor.setIncludeColumns(Collections.singletonList("NAME"));
		DataChangeConfiguration properties = new DataChangeConfiguration();
		properties.setEnabled(true);
		properties.setTables(Collections.singletonList(monitor));
		properties.validate();
		return properties;
	}

	private interface DatabaseMutation {
		void run(Statement statement) throws Exception;
	}
}
