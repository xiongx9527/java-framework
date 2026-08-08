package com.xiongx9527.fw.plus.mybatis.datachange.dialect.it;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapterRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.MybatisDataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.api.ConsistencyLevel;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeEvent;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeFailure;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeOperation;
import com.xiongx9527.fw.plus.mybatis.datachange.api.FieldChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.StatementChange;
import com.xiongx9527.fw.plus.mybatis.datachange.config.DataChangeConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitorRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialectRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.diff.ChangeDiffEngine;
import com.xiongx9527.fw.plus.mybatis.datachange.dispatch.DataChangeDispatcher;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.DataChangeEngine;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.DataChangeInterceptor;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.JdbcTableMetadataResolver;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.SnapshotService;
import org.apache.ibatis.builder.SqlSourceBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.logging.nologging.NoLoggingImpl;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.mapping.SqlSource;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

abstract class DialectDatabaseContract {
	private static final String INSERT = "contract.insert";
	private static final String UPDATE = "contract.update";
	private static final String UPSERT = "contract.upsert";
	private static final String DELETE = "contract.delete";
	private static final String RETURNING = "contract.returning";

	private final String environmentPrefix;
	private final DataChangeDialect dialect;
	private String jdbcUrl;
	private String username;
	private String password;
	private RecordingHandler events;
	private RecordingErrorHandler errors;

	DialectDatabaseContract(String environmentPrefix, DataChangeDialect dialect) {
		this.environmentPrefix = environmentPrefix;
		this.dialect = dialect;
	}

	final void runContract() throws Exception {
		loadEnvironment();
		String suffix = Long.toHexString(System.nanoTime());
		String schema = supportsCreatedSchema() ? "dc_" + suffix : null;
		String table = "dc_event_" + suffix;
		try (Connection connection = open()) {
			assertThat(new DataChangeDialectRegistry(Collections.singletonList(dialect)).select(connection))
					.isSameAs(dialect);
			createObjects(connection, schema, table);
		}
		try {
			verifyProductionContract(schema, table);
		} finally {
			cleanup(schema, table);
		}
	}

	protected abstract boolean supportsCreatedSchema();
	protected abstract String driverClassName();
	protected abstract String createTableSql(String qualifiedTable);
	protected abstract List<String> createTriggerSql(String schema, String table, String qualifiedTable);
	protected abstract String upsertSql(String table);
	protected abstract String qualified(String schema, String table);

	protected String dmlTable(String schema, String table) { return qualified(schema, table); }
	protected String monitorSchema(String schema) { return schema; }
	protected String returningSql(String table) { return null; }
	protected int[] acceptedUpsertCounts() { return new int[]{1}; }
	protected String createSchemaSql(String schema) { return "CREATE SCHEMA " + schema; }
	protected List<String> dropExtraSql(String schema, String table, String qualifiedTable) {
		return Collections.emptyList();
	}
	protected void configureConnection(Connection connection, String schema) throws Exception { }

	private void verifyProductionContract(String schema, String table) throws Exception {
		events = new RecordingHandler();
		errors = new RecordingErrorHandler();
		String sqlTable = dmlTable(schema, table);
		SqlSessionFactory factory = factory(schema, table, sqlTable);

		Map<String, Object> initial = values(7L, "A", 10);
		try (SqlSession session = factory.openSession(true)) {
			int count = execute(session, INSERT, initial);
			StatementChange insert = event(count, DataChangeOperation.INSERT);
			assertThat(initial.get("id")).isInstanceOf(Number.class);
			assertThat(((Number) initial.get("id")).longValue()).isPositive();
			assertIdentityId(insert, initial.get("id"));
			assertThat(field(row(insert), "status").getAfterValue()).isEqualTo("NEW");
		}

		try (SqlSession session = factory.openSession(true)) {
			Map<String, Object> update = values(7L, "A", 20);
			StatementChange change = event(execute(session, UPDATE, update), DataChangeOperation.UPDATE);
			assertIdentityId(change, initial.get("id"));
			assertThat(((Number) field(row(change), "touched").getAfterValue()).longValue()).isGreaterThan(0L);
		}

		try (SqlSession session = factory.openSession(true)) {
			Map<String, Object> inserted = values(8L, "UPSERT_INSERT", 30);
			StatementChange insert = event(execute(session, UPSERT, inserted), DataChangeOperation.INSERT,
					acceptedUpsertCounts());
			assertThat(field(row(insert), "status").getAfterValue()).isEqualTo("NEW");

			Map<String, Object> matched = values(7L, "A", 40);
			StatementChange update = event(execute(session, UPSERT, matched), DataChangeOperation.UPDATE,
					acceptedUpsertCounts());
			assertThat(field(row(update), "amount").getBeforeValue()).isEqualTo(20);
			assertThat(field(row(update), "amount").getAfterValue()).isEqualTo(40);
		}

		verifyReturning(factory);
		verifyRollback(factory, schema, sqlTable);
		verifyConcurrentLock(factory);

		try (SqlSession session = factory.openSession(true)) {
			StatementChange delete = event(execute(session, DELETE, values(7L, "A", null)),
					DataChangeOperation.PHYSICAL_DELETE);
			assertIdentityId(delete, initial.get("id"));
			assertThat(field(row(delete), "amount").getBeforeValue()).isNotNull();
		}
		assertThat(errors.failures).isEmpty();
	}

	private void assertIdentityId(StatementChange change, Object expected) {
		Map<String, Object> identity = row(change).getIdentity().getKeyValues();
		assertThat(identity).containsKey("id");
		assertThat(identity.get("id")).isInstanceOf(Number.class);
		assertThat(((Number) identity.get("id")).longValue())
				.isEqualTo(((Number) expected).longValue());
	}

	private void verifyReturning(SqlSessionFactory factory) {
		if (returningSql("ignored") == null) { return; }
		try (SqlSession session = factory.openSession(true)) {
			Map<String, Object> values = values(9L, "RETURNING", 11);
			int actualCount = execute(session, RETURNING, values);
			assertThat(actualCount).isIn(-1, 1);
			DataChangeEvent event = events.singleEvent();
			assertThat(event.getStatements()).singleElement();
			StatementChange change = event.getStatements().get(0);
			assertThat(change.getAffectedRows()).isEqualTo(1);
			assertThat(row(change).getOperation()).isEqualTo(DataChangeOperation.INSERT);
			assertThat(row(change).getIdentity().getKeyValues()).containsOnlyKeys("id");
			assertThat(row(change).getIdentity().getKeyValues().get("id")).isInstanceOf(Number.class);
			assertThat(errors.failures).isEmpty();
		}
	}

	private void verifyRollback(SqlSessionFactory factory, String schema, String table) throws Exception {
		Map<String, Object> values = values(10L, "ROLLBACK", 90);
		try (SqlSession session = factory.openSession(false)) {
			StatementChange change = event(execute(session, INSERT, values), DataChangeOperation.INSERT);
			assertThat(events.singleConsistency()).isEqualTo(ConsistencyLevel.TRANSACTIONAL);
			assertThat(change.getAffectedRows()).isEqualTo(1);
			session.rollback();
		}
		assertThat(count(schema, table, 10L, "ROLLBACK")).isZero();
	}

	private void verifyConcurrentLock(SqlSessionFactory factory) throws Exception {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		try (SqlSession locked = factory.openSession(false)) {
			StatementChange first = event(execute(locked, UPDATE, values(7L, "A", 50)),
					DataChangeOperation.UPDATE);
			assertThat(events.singleConsistency()).isEqualTo(ConsistencyLevel.TRANSACTIONAL);
			assertThat(first.getAffectedRows()).isEqualTo(1);
			Future<Integer> concurrent = executor.submit(() -> {
				try (SqlSession session = factory.openSession(true)) {
					return Integer.valueOf(session.update(UPDATE, values(7L, "A", 60)));
				}
			});
			assertThatThrownBy(() -> concurrent.get(250, TimeUnit.MILLISECONDS))
					.isInstanceOf(TimeoutException.class);
			locked.rollback();
			assertThat(concurrent.get(10, TimeUnit.SECONDS).intValue()).isEqualTo(1);
		}
		finally {
			executor.shutdownNow();
			executor.awaitTermination(10, TimeUnit.SECONDS);
		}
	}

	private SqlSessionFactory factory(String schema, String table, String sqlTable) throws Exception {
		DataChangeConfiguration properties = new DataChangeConfiguration();
		properties.setEnabled(true);
		properties.setMaxSnapshotRows(100);
		properties.setTables(Collections.singletonList(monitor(schema, table)));
		properties.validate();

		DataChangeDispatcher dispatcher = new DataChangeDispatcher(
				Collections.<DataChangeHandler>singletonList(events), errors);
		DataChangeEngine engine = new DataChangeEngine(properties,
				new TableMonitorRegistry(properties.getTables()),
				new DataChangeFrameworkAdapterRegistry(Collections.<DataChangeFrameworkAdapter>singletonList(
						new MybatisDataChangeFrameworkAdapter())),
				new DataChangeDialectRegistry(Collections.singletonList(dialect)),
				new JdbcTableMetadataResolver(), new SnapshotService(), new ChangeDiffEngine(), dispatcher, errors);

		UnpooledDataSource delegate = new UnpooledDataSource();
		delegate.setDriver(driverClassName());
		delegate.setUrl(jdbcUrl);
		delegate.setUsername(username);
		delegate.setPassword(password);
		DataSource dataSource = new ConfiguredDataSource(delegate, schema);
		Configuration configuration = new Configuration(
				new Environment("datachange-dialect-it", new JdbcTransactionFactory(), dataSource));
		configuration.setLogImpl(NoLoggingImpl.class);
		configuration.addInterceptor(new DataChangeInterceptor(properties, engine));
		addStatement(configuration, INSERT, "INSERT INTO " + sqlTable
				+ " (tenant_id,order_no,amount) VALUES (#{tenant_id},#{order_no},#{amount})",
				SqlCommandType.INSERT, true);
		addStatement(configuration, UPDATE, "UPDATE " + sqlTable
				+ " SET amount=#{amount} WHERE tenant_id=#{tenant_id} AND order_no=#{order_no}",
				SqlCommandType.UPDATE, false);
		addStatement(configuration, UPSERT, upsertSql(sqlTable), SqlCommandType.INSERT, false);
		addStatement(configuration, DELETE, "DELETE FROM " + sqlTable
				+ " WHERE tenant_id=#{tenant_id} AND order_no=#{order_no}", SqlCommandType.DELETE, false);
		String returning = returningSql(sqlTable);
		if (returning != null) { addStatement(configuration, RETURNING, returning, SqlCommandType.INSERT, false); }
		return new SqlSessionFactoryBuilder().build(configuration);
	}

	private void addStatement(Configuration configuration, String id, String sql, SqlCommandType command,
			boolean generatedKey) {
		SqlSource source = new SqlSourceBuilder(configuration).parse(sql, Map.class,
				Collections.<String, Object>emptyMap());
		MappedStatement.Builder builder = new MappedStatement.Builder(configuration, id, source, command);
		if (generatedKey) {
			builder.keyGenerator(Jdbc3KeyGenerator.INSTANCE).keyProperty("id").keyColumn("ID");
		}
		configuration.addMappedStatement(builder.build());
	}

	private int execute(SqlSession session, String statement, Map<String, Object> parameters) {
		events.clear();
		return session.update(statement, parameters);
	}

	private StatementChange event(int actualCount, DataChangeOperation operation, int... acceptedCounts) {
		Integer[] accepted = boxed(acceptedCounts.length == 0 ? new int[]{1} : acceptedCounts);
		assertThat(actualCount).isIn(accepted);
		DataChangeEvent event = events.singleEvent();
		assertThat(event.getStatements()).singleElement();
		StatementChange statement = event.getStatements().get(0);
		assertThat(statement.getAffectedRows()).isEqualTo(actualCount);
		assertThat(row(statement).getOperation()).isEqualTo(operation);
		assertThat(errors.failures).isEmpty();
		return statement;
	}

	private RowChange row(StatementChange statement) {
		assertThat(statement.getTables()).singleElement();
		assertThat(statement.getTables().get(0).getRows()).singleElement();
		return statement.getTables().get(0).getRows().get(0);
	}

	private FieldChange field(RowChange row, String column) {
		return row.getFields().stream().filter(change -> column.equalsIgnoreCase(change.getColumnName()))
				.findFirst().orElseThrow(() -> new AssertionError("Missing field " + column));
	}

	private Map<String, Object> values(Long tenantId, String orderNo, Integer amount) {
		Map<String, Object> values = new LinkedHashMap<String, Object>();
		values.put("tenant_id", tenantId);
		values.put("order_no", orderNo);
		values.put("amount", amount);
		return values;
	}

	private TableMonitor monitor(String schema, String table) {
		TableMonitor monitor = new TableMonitor();
		monitor.setSchema(monitorSchema(schema));
		monitor.setName(table);
		monitor.setKeyColumns(Collections.singletonList("id"));
		monitor.setIncludeColumns(Arrays.asList("amount", "status", "touched"));
		return monitor;
	}

	private void createObjects(Connection connection, String schema, String table) throws Exception {
		try (Statement statement = connection.createStatement()) {
			if (schema != null) { statement.execute(createSchemaSql(schema)); }
			String qualifiedTable = qualified(schema, table);
			statement.execute(createTableSql(qualifiedTable));
			for (String sql : createTriggerSql(schema, table, qualifiedTable)) { statement.execute(sql); }
		}
	}

	private long count(String schema, String table, long tenantId, String orderNo) throws Exception {
		try (Connection connection = open()) {
			configureConnection(connection, schema);
			try (java.sql.PreparedStatement statement = connection.prepareStatement(
					"SELECT COUNT(*) FROM " + table + " WHERE tenant_id=? AND order_no=?")) {
			statement.setLong(1, tenantId);
			statement.setString(2, orderNo);
			try (ResultSet rows = statement.executeQuery()) {
				assertThat(rows.next()).isTrue();
				return rows.getLong(1);
			}
			}
		}
	}

	private Integer[] boxed(int[] values) {
		Integer[] boxed = new Integer[values.length];
		for (int index = 0; index < values.length; index++) { boxed[index] = Integer.valueOf(values[index]); }
		return boxed;
	}

	private void loadEnvironment() {
		jdbcUrl = required(environmentPrefix + "_JDBC_URL");
		username = required(environmentPrefix + "_USERNAME");
		password = required(environmentPrefix + "_PASSWORD");
	}

	private String required(String name) {
		String value = System.getenv(name);
		if (value == null) { throw new IllegalStateException("Missing required environment variable: " + name); }
		return value;
	}

	private Connection open() throws Exception {
		Class.forName(driverClassName());
		return DriverManager.getConnection(jdbcUrl, username, password);
	}

	private void cleanup(String schema, String table) {
		if (jdbcUrl == null) { return; }
		try (Connection connection = open(); Statement statement = connection.createStatement()) {
			String qualifiedTable = qualified(schema, table);
			for (String sql : dropExtraSql(schema, table, qualifiedTable)) {
				try { statement.execute(sql); } catch (Exception ignored) { }
			}
			try { statement.execute("DROP TABLE " + qualifiedTable); } catch (Exception ignored) { }
			if (schema != null) {
				try { statement.execute("DROP SCHEMA " + schema); } catch (Exception ignored) { }
			}
		} catch (Exception ignored) {
			// Cleanup cannot replace the contract failure and only targets generated object names.
		}
	}

	private static final class RecordingHandler implements DataChangeHandler {
		private final List<DataChangeEvent> recorded = new CopyOnWriteArrayList<DataChangeEvent>();

		@Override public boolean supports(DataChangeEvent event) { return true; }
		@Override public void handle(DataChangeEvent event) { recorded.add(event); }
		@Override public int getOrder() { return 0; }

		private DataChangeEvent singleEvent() {
			assertThat(recorded).singleElement();
			return recorded.get(0);
		}

		private ConsistencyLevel singleConsistency() { return singleEvent().getConsistencyLevel(); }
		private void clear() { recorded.clear(); }
	}

	private static final class RecordingErrorHandler implements DataChangeErrorHandler {
		private final List<DataChangeFailure> failures = new CopyOnWriteArrayList<DataChangeFailure>();
		@Override public void handle(DataChangeFailure failure) { failures.add(failure); }
	}

	private final class ConfiguredDataSource implements DataSource {
		private final DataSource delegate;
		private final String schema;

		private ConfiguredDataSource(DataSource delegate, String schema) {
			this.delegate = delegate;
			this.schema = schema;
		}

		@Override public Connection getConnection() throws SQLException { return configured(delegate.getConnection()); }
		@Override public Connection getConnection(String user, String pass) throws SQLException {
			return configured(delegate.getConnection(user, pass));
		}
		private Connection configured(Connection connection) throws SQLException {
			try {
				configureConnection(connection, schema);
				return connection;
			} catch (Exception failure) {
				try { connection.close(); } catch (Exception ignored) { }
				if (failure instanceof SQLException) { throw (SQLException) failure; }
				throw new SQLException("Unable to configure integration-test connection");
			}
		}
		@Override public PrintWriter getLogWriter() throws SQLException { return delegate.getLogWriter(); }
		@Override public void setLogWriter(PrintWriter out) throws SQLException { delegate.setLogWriter(out); }
		@Override public void setLoginTimeout(int seconds) throws SQLException { delegate.setLoginTimeout(seconds); }
		@Override public int getLoginTimeout() throws SQLException { return delegate.getLoginTimeout(); }
		@Override public Logger getParentLogger() { return Logger.getLogger("global"); }
		@Override public <T> T unwrap(Class<T> iface) throws SQLException { return delegate.unwrap(iface); }
		@Override public boolean isWrapperFor(Class<?> iface) throws SQLException { return delegate.isWrapperFor(iface); }
	}
}
