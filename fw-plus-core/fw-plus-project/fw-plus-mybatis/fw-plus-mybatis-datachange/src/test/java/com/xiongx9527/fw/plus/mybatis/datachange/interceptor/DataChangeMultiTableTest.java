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
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DataChangeMultiTableTest {

	@Test
	void multiTableStatementKeepsOneStatementAndTwoTables() throws Exception {
		JdbcDataSource dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:datachange_multi_table;DB_CLOSE_DELAY=-1");
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			statement.execute("DROP ALL OBJECTS");
			statement.execute("CREATE TABLE account (id BIGINT PRIMARY KEY, name VARCHAR(64))");
			statement.execute("CREATE TABLE account_summary (id BIGINT PRIMARY KEY, name VARCHAR(64))");
			statement.execute("INSERT INTO account VALUES(1, 'before')");
			statement.execute("INSERT INTO account_summary VALUES(1, 'before')");

			DataChangeConfiguration properties = configuration();
			DataChangeFrameworkAdapter adapter = new MybatisDataChangeFrameworkAdapter();
			DataChangeEngine engine = new DataChangeEngine(properties,
					new TableMonitorRegistry(properties.getTables()),
					new DataChangeFrameworkAdapterRegistry(Collections.singletonList(adapter)),
					new DataChangeDialectRegistry(Collections.<DataChangeDialect>singletonList(new H2DataChangeDialect())),
					new JdbcTableMetadataResolver(), new SnapshotService(), new ChangeDiffEngine(),
					new DataChangeDispatcher(Collections.emptyList(), null), null);
			String sql = "DELETE account, account_summary FROM account "
					+ "JOIN account_summary ON account_summary.id=account.id WHERE account.id=1";
			Configuration mybatis = new Configuration();
			MappedStatement mappedStatement = new MappedStatement.Builder(mybatis, "deleteAccount",
					new StaticSqlSource(mybatis, sql), SqlCommandType.DELETE).build();
			DataChangeExecutionContext context = new DataChangeExecutionContext(mappedStatement, null, adapter);
			engine.initializeConnection(context, connection);
			List<StatementChangePlan> plans = engine.beforeStatement(context,
					new BoundSqlContext(mappedStatement.getBoundSql(null)));
			statement.executeUpdate("DELETE FROM account_summary WHERE id=1");
			statement.executeUpdate("DELETE FROM account WHERE id=1");
			List<JdbcStatementResult> results = Collections.singletonList(JdbcStatementResult.updateCount(2));
			engine.captureStatementResults(context, plans, mock(Statement.class), results);

			List<StatementChange> changes = engine.afterStatement(context, plans, results);

			assertThat(changes).hasSize(1);
			assertThat(changes.get(0).getAffectedRows()).isEqualTo(2);
			assertThat(changes.get(0).getTables())
					.extracting(change -> change.getTable().getTable().toLowerCase())
					.containsExactlyInAnyOrder("account", "account_summary");
		}
	}

	private DataChangeConfiguration configuration() {
		TableMonitor account = monitor("ACCOUNT");
		TableMonitor summary = monitor("ACCOUNT_SUMMARY");
		DataChangeConfiguration properties = new DataChangeConfiguration();
		properties.setEnabled(true);
		properties.setTables(Arrays.asList(account, summary));
		properties.validate();
		return properties;
	}

	private TableMonitor monitor(String table) {
		TableMonitor monitor = new TableMonitor();
		monitor.setName(table);
		monitor.setKeyColumns(Collections.singletonList("ID"));
		monitor.setIncludeColumns(Collections.singletonList("NAME"));
		return monitor;
	}
}
