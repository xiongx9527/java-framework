package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapterRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkChangeMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.MybatisDataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.config.DataChangeConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitorRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.AnalyzedStatement;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.BoundSqlContext;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.ChangeTarget;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialectRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DatabaseMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.H2DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.SnapshotQuery;
import com.xiongx9527.fw.plus.mybatis.datachange.diff.ChangeDiffEngine;
import com.xiongx9527.fw.plus.mybatis.datachange.dispatch.DataChangeDispatcher;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.JdbcTableMetadataResolver;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.SnapshotService;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.Configuration;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class DataChangeInterceptorPrepareTest {

	@Test
	void closesPreparedStatementWhenBeforeSnapshotFailsWithoutLeakingCloseFailure() throws Exception {
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:datachange_prepare;DB_CLOSE_DELAY=-1");
		try (Connection connection = dataSource.getConnection()) {
			createTable(connection);
			MappedStatement mappedStatement = mappedStatement();
			BoundSql boundSql = mappedStatement.getBoundSql(null);
			Statement statement = Mockito.mock(Statement.class);
			AtomicInteger closeCalls = new AtomicInteger();
			Mockito.doAnswer(invocation -> {
				closeCalls.incrementAndGet();
				throw new SQLException("secret-binding");
			}).when(statement).close();
			StatementHandler handler = Mockito.mock(StatementHandler.class);
			when(handler.prepare(connection, null)).thenReturn(statement);
			when(handler.getBoundSql()).thenReturn(boundSql);
			Invocation invocation = new Invocation(handler,
					StatementHandler.class.getMethod("prepare", Connection.class, Integer.class),
					new Object[]{connection, null});
			DataChangeFrameworkAdapter adapter = new MybatisDataChangeFrameworkAdapter();
			DataChangeExecutionContext context = new DataChangeExecutionContext(mappedStatement, null, adapter);

			try (DataChangeExecutionContextHolder.Scope ignored = DataChangeExecutionContextHolder.push(context)) {
				assertThatThrownBy(() -> interceptor(adapter).intercept(invocation))
						.isInstanceOf(DataChangeException.class)
						.satisfies(failure -> {
							DataChangeException dataChange = (DataChangeException) failure;
							assertThat(dataChange.getCode()).isEqualTo(DataChangeErrorCode.SNAPSHOT_QUERY_FAILED);
							assertThat(dataChange).hasMessage("SNAPSHOT_QUERY_FAILED")
									.hasMessageNotContaining("secret-binding").hasNoCause();
							assertThat(dataChange.getSuppressed()).isEmpty();
						});
			}

			assertThat(closeCalls).hasValue(1);
			assertThat(DataChangeExecutionContextHolder.current()).isEmpty();
		}
	}

	private DataChangeInterceptor interceptor(DataChangeFrameworkAdapter adapter) {
		DataChangeConfiguration configuration = configuration();
		DataChangeEngine engine = new DataChangeEngine(configuration,
				new TableMonitorRegistry(configuration.getTables()),
				new DataChangeFrameworkAdapterRegistry(
						Collections.singletonList(adapter)),
				new DataChangeDialectRegistry(
						Collections.<DataChangeDialect>singletonList(new FailingBeforeSnapshotDialect())),
				new JdbcTableMetadataResolver(), new SnapshotService(), new ChangeDiffEngine(),
				new DataChangeDispatcher(Collections.emptyList(), null), null);
		return new DataChangeInterceptor(configuration, engine);
	}

	private DataChangeConfiguration configuration() {
		TableMonitor monitor = new TableMonitor();
		monitor.setName("SYS_USER");
		monitor.setKeyColumns(Collections.singletonList("ID"));
		monitor.setIncludeColumns(Collections.singletonList("NAME"));
		DataChangeConfiguration configuration = new DataChangeConfiguration();
		configuration.setEnabled(true);
		configuration.setTables(Collections.singletonList(monitor));
		configuration.validate();
		return configuration;
	}

	private MappedStatement mappedStatement() {
		Configuration configuration = new Configuration();
		return new MappedStatement.Builder(configuration, "renameUser",
				new StaticSqlSource(configuration, "UPDATE sys_user SET name = 'after' WHERE id = 1"),
				SqlCommandType.UPDATE).build();
	}

	private void createTable(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE sys_user (id BIGINT PRIMARY KEY, name VARCHAR(64))");
			statement.execute("INSERT INTO sys_user(id, name) VALUES(1, 'before')");
		}
	}

	private static final class FailingBeforeSnapshotDialect implements DataChangeDialect {
		private final H2DataChangeDialect delegate = new H2DataChangeDialect();

		@Override
		public boolean supports(DatabaseMetadata metadata) { return delegate.supports(metadata); }

		@Override
		public TableIdentifier normalize(TableIdentifier table) { return delegate.normalize(table); }

		@Override
		public List<AnalyzedStatement> analyze(BoundSqlContext context) { return delegate.analyze(context); }

		@Override
		public SnapshotQuery buildBeforeQuery(ChangeTarget target) {
			return new SnapshotQuery("SELECT missing_column FROM missing_table", Collections.emptyList());
		}

		@Override
		public SnapshotQuery buildAfterQuery(ChangeTarget target, List<RowIdentity> identities) {
			return delegate.buildAfterQuery(target, identities);
		}

		@Override
		public List<RowIdentity> lookupGeneratedKeys(MappedStatement mappedStatement, Object parameterObject,
				Statement statement, FrameworkChangeMetadata frameworkMetadata, List<String> keyColumns) {
			return delegate.lookupGeneratedKeys(mappedStatement, parameterObject, statement, frameworkMetadata,
					keyColumns);
		}

		@Override
		public int getOrder() { return delegate.getOrder(); }
	}
}
