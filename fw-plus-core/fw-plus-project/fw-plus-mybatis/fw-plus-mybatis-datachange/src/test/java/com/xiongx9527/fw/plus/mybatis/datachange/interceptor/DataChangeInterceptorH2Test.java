package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapterRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.FrameworkChangeMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.MybatisDataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeEvent;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeFailure;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeOperation;
import com.xiongx9527.fw.plus.mybatis.datachange.api.FieldChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowIdentity;
import com.xiongx9527.fw.plus.mybatis.datachange.api.StatementChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableChange;
import com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier;
import com.xiongx9527.fw.plus.mybatis.datachange.config.DataChangeConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.config.LogicalDeleteConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitorRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.AnalyzedStatement;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.BoundSqlContext;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.ChangeTarget;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialectRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DatabaseMetadata;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.H2DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.MySqlDataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.SnapshotQuery;
import com.xiongx9527.fw.plus.mybatis.datachange.diff.ChangeDiffEngine;
import com.xiongx9527.fw.plus.mybatis.datachange.dispatch.DataChangeDispatcher;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.fixture.TestUser;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.fixture.TestUserMapper;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.JdbcTableMetadataResolver;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.SnapshotService;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataChangeInterceptorH2Test {
	private static final AtomicInteger DATABASE_SEQUENCE = new AtomicInteger();

	private JdbcDataSource dataSource;
	private SqlSessionFactory sessionFactory;
	private SqlSession session;
	private TestUserMapper mapper;
	private RecordingHandler recordingHandler;
	private RecordingErrorHandler errorHandler;
	private ToggleFailingHandler failingHandler;
	private ToggleH2Dialect dialect;

	@BeforeEach
	void setUp() throws Exception {
		dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:datachange_" + DATABASE_SEQUENCE.incrementAndGet()
				+ ";MODE=MySQL;DB_CLOSE_DELAY=-1");
		initializeSchema();
		recordingHandler = new RecordingHandler();
		errorHandler = new RecordingErrorHandler();
		failingHandler = new ToggleFailingHandler();
		dialect = new ToggleH2Dialect();
		openSession(10);
	}

	@AfterEach
	void tearDown() {
		if (session != null) {
			session.close();
		}
		assertThat(DataChangeExecutionContextHolder.current()).isEmpty();
	}

	@Test
	void recordsUpdateBeforeAndAfterValues() throws Exception {
		insertUser(1L, "before", 0);

		assertThat(mapper.rename(1L, "after")).isEqualTo(1);

		RowChange row = onlyRow(recordingHandler.singleEvent());
		assertThat(row.getOperation()).isEqualTo(DataChangeOperation.UPDATE);
		assertThat(row.getIdentity().getKeyValues()).containsEntry("ID", 1L);
		assertThat(field(row, "NAME").getBeforeValue()).isEqualTo("before");
		assertThat(field(row, "NAME").getAfterValue()).isEqualTo("after");
	}

	@Test
	void recordsInsertIncludingDatabaseDefaults() {
		TestUser user = new TestUser(null, "created", null);

		assertThat(mapper.insertDefault(user)).isEqualTo(1);

		RowChange row = onlyRow(recordingHandler.singleEvent());
		assertThat(user.getId()).isNotNull();
		assertThat(row.getOperation()).isEqualTo(DataChangeOperation.INSERT);
		assertThat(row.getIdentity().getKeyValues()).containsEntry("ID", user.getId());
		assertThat(field(row, "NAME").getAfterValue()).isEqualTo("created");
		assertThat(field(row, "DELETED").getAfterValue()).isEqualTo(0);
	}

	@Test
	void classifiesUpsertInsertAndMatchedUpdateFromProductionChain() throws Exception {
		useMySqlProductionDialect();
		assertThat(mapper.upsert(21L, "inserted")).isEqualTo(1);
		StatementChange inserted = onlyStatement(recordingHandler.singleEvent());
		assertThat(inserted.getAffectedRows()).isEqualTo(1);
		assertThat(inserted.getTables().get(0).getRows().get(0).getOperation())
				.isEqualTo(DataChangeOperation.INSERT);

		recordingHandler.clear();
		assertThat(mapper.upsert(21L, "updated")).isGreaterThan(0);
		StatementChange updated = onlyStatement(recordingHandler.singleEvent());
		assertThat(updated.getAffectedRows()).isGreaterThan(0);
		RowChange row = updated.getTables().get(0).getRows().get(0);
		assertThat(row.getOperation()).isEqualTo(DataChangeOperation.UPDATE);
		assertThat(field(row, "NAME").getBeforeValue()).isEqualTo("inserted");
		assertThat(field(row, "NAME").getAfterValue()).isEqualTo("updated");
	}

	@Test
	void classifiesMergeInsertAndMatchedUpdateFromProductionChain() throws Exception {
		useMySqlProductionDialect();
		assertThat(mapper.mergeComposite(7L, 31L, "inserted")).isEqualTo(1);
		StatementChange inserted = onlyStatement(recordingHandler.singleEvent());
		assertThat(inserted.getAffectedRows()).isEqualTo(1);
		assertThat(inserted.getTables().get(0).getRows().get(0).getOperation())
				.isEqualTo(DataChangeOperation.INSERT);

		recordingHandler.clear();
		assertThat(mapper.mergeComposite(7L, 31L, "updated")).isEqualTo(1);
		StatementChange updated = onlyStatement(recordingHandler.singleEvent());
		assertThat(updated.getAffectedRows()).isEqualTo(1);
		RowChange row = updated.getTables().get(0).getRows().get(0);
		assertThat(row.getOperation()).isEqualTo(DataChangeOperation.UPDATE);
		assertThat(field(row, "NAME").getBeforeValue()).isEqualTo("inserted");
		assertThat(field(row, "NAME").getAfterValue()).isEqualTo("updated");
	}

	@Test
	void recordsPhysicalDeleteFromBeforeSnapshot() throws Exception {
		insertUser(1L, "before", 0);

		assertThat(mapper.delete(1L)).isEqualTo(1);

		RowChange row = onlyRow(recordingHandler.singleEvent());
		assertThat(row.getOperation()).isEqualTo(DataChangeOperation.PHYSICAL_DELETE);
		assertThat(row.getIdentity().getKeyValues()).containsEntry("ID", 1L);
		assertThat(field(row, "NAME").getBeforeValue()).isEqualTo("before");
		assertThat(field(row, "NAME").getAfterValue()).isNull();
	}

	@Test
	void classifiesConfiguredLogicalDelete() throws Exception {
		insertUser(1L, "before", 0);

		assertThat(mapper.logicalDelete(1L)).isEqualTo(1);

		RowChange row = onlyRow(recordingHandler.singleEvent());
		assertThat(row.getOperation()).isEqualTo(DataChangeOperation.LOGICAL_DELETE);
		assertThat(field(row, "DELETED").getBeforeValue()).isEqualTo(0);
		assertThat(field(row, "DELETED").getAfterValue()).isEqualTo(1);
	}

	@Test
	void omitsZeroRowUpdate() {
		assertThat(mapper.rename(99L, "after")).isZero();

		assertThat(recordingHandler.events).isEmpty();
		assertThat(errorHandler.failures).isEmpty();
		assertThat(DataChangeExecutionContextHolder.current()).isEmpty();
	}

	@Test
	void bypassesUnmonitoredTable() throws Exception {
		insertIgnoredUser(1L, "before");

		assertThat(mapper.renameUnmonitored(1L, "after")).isEqualTo(1);

		assertThat(selectString("SELECT name FROM ignored_user WHERE id = 1")).isEqualTo("after");
		assertThat(recordingHandler.events).isEmpty();
	}

	@Test
	void readsAfterByCapturedIdentityWhenRowLeavesOriginalWhere() throws Exception {
		insertUser(1L, "before", 0);

		assertThat(mapper.renameWhereName(1L, "before", "after")).isEqualTo(1);

		RowChange row = onlyRow(recordingHandler.singleEvent());
		assertThat(row.getOperation()).isEqualTo(DataChangeOperation.UPDATE);
		assertThat(field(row, "NAME").getAfterValue()).isEqualTo("after");
	}

	@Test
	void readsGeneratedKey() {
		TestUser user = new TestUser(null, "generated", null);

		assertThat(mapper.insertDefault(user)).isEqualTo(1);

		RowChange row = onlyRow(recordingHandler.singleEvent());
		assertThat(user.getId()).isPositive();
		assertThat(row.getIdentity().getKeyValues()).containsEntry("ID", user.getId());
	}

	@Test
	void recordsCompositeIdentity() throws Exception {
		insertTenantUser(7L, 11L, "before");

		assertThat(mapper.renameComposite(7L, 11L, "after")).isEqualTo(1);

		RowChange row = onlyRow(recordingHandler.singleEvent());
		assertThat(row.getIdentity().getKeyValues())
				.containsEntry("TENANT_ID", 7L).containsEntry("USER_ID", 11L);
		assertThat(field(row, "NAME").getAfterValue()).isEqualTo("after");
	}

	@Test
	void rejectsLimitPlusOneBeforeDml() throws Exception {
		insertUser(1L, "one", 0);
		insertUser(2L, "two", 0);
		reopenSession(1, true);

		assertThatThrownBy(() -> mapper.renameAll("changed"))
				.satisfies(failure -> assertCode(failure, DataChangeErrorCode.SNAPSHOT_LIMIT_EXCEEDED));

		assertThat(selectString("SELECT name FROM sys_user WHERE id = 1")).isEqualTo("one");
		assertThat(selectString("SELECT name FROM sys_user WHERE id = 2")).isEqualTo("two");
		assertThat(recordingHandler.events).isEmpty();
	}

	@Test
	void transactionalHandlerFailureRollsBackBusinessUpdate() throws Exception {
		insertUser(1L, "before", 0);
		reopenSession(10, false);
		failingHandler.fail = true;

		assertThatThrownBy(() -> mapper.rename(1L, "after"))
				.satisfies(failure -> assertCode(failure, DataChangeErrorCode.HANDLER_FAILED));
		session.rollback();

		assertThat(selectString("SELECT name FROM sys_user WHERE id = 1")).isEqualTo("before");
		assertThat(errorHandler.failures).isEmpty();
	}

	@Test
	void autoCommitHandlerFailureReportsWithoutPretendingBusinessFailure() throws Exception {
		insertUser(1L, "before", 0);
		failingHandler.fail = true;

		assertThat(mapper.rename(1L, "after")).isEqualTo(1);

		assertThat(selectString("SELECT name FROM sys_user WHERE id = 1")).isEqualTo("after");
		assertThat(errorHandler.failures).singleElement()
				.extracting(DataChangeFailure::getCode).isEqualTo(DataChangeErrorCode.HANDLER_FAILED);
		assertThat(recordingHandler.events).hasSize(1);
	}

	@Test
	void transactionalAfterSnapshotFailureRollsBackBusinessUpdate() throws Exception {
		insertUser(1L, "before", 0);
		reopenSession(10, false);
		dialect.failAfterSnapshot = true;

		assertThatThrownBy(() -> mapper.rename(1L, "after"))
				.satisfies(failure -> assertCode(failure, DataChangeErrorCode.SNAPSHOT_QUERY_FAILED));
		session.rollback();

		assertThat(selectString("SELECT name FROM sys_user WHERE id = 1")).isEqualTo("before");
	}

	@Test
	void autoCommitAfterSnapshotFailureReportsAndKeepsBusinessResult() throws Exception {
		insertUser(1L, "before", 0);
		dialect.failAfterSnapshot = true;

		assertThat(mapper.rename(1L, "after")).isEqualTo(1);

		assertThat(selectString("SELECT name FROM sys_user WHERE id = 1")).isEqualTo("after");
		assertThat(errorHandler.failures).singleElement()
				.extracting(DataChangeFailure::getCode).isEqualTo(DataChangeErrorCode.SNAPSHOT_QUERY_FAILED);
		assertThat(recordingHandler.events).isEmpty();
	}

	@Test
	void dmlFailureDoesNotEmitEventAndClearsContext() throws Exception {
		insertUser(1L, "before", 0);

		assertThatThrownBy(() -> mapper.insertExplicit(1L, "duplicate"));

		assertThat(recordingHandler.events).isEmpty();
		assertThat(errorHandler.failures).isEmpty();
		assertThat(DataChangeExecutionContextHolder.current()).isEmpty();
	}

	@Test
	void nestedMapperFailureFromHandlerClearsContext() throws Exception {
		insertUser(1L, "before", 0);
		failingHandler.nestedMapper = mapper;

		assertThat(mapper.rename(1L, "after")).isEqualTo(1);

		assertThat(errorHandler.failures).singleElement()
				.extracting(DataChangeFailure::getCode).isEqualTo(DataChangeErrorCode.HANDLER_FAILED);
		assertThat(DataChangeExecutionContextHolder.current()).isEmpty();
	}

	@Test
	void suppressesNestedMapperCallFromHandlerSupports() throws Exception {
		insertUser(1L, "outer-before", 0);
		insertUser(2L, "nested-before", 0);
		failingHandler.supportsMapper = mapper;

		assertThat(mapper.rename(1L, "outer-after")).isEqualTo(1);

		assertThat(selectString("SELECT name FROM sys_user WHERE id = 2")).isEqualTo("nested-after");
		assertThat(recordingHandler.events).hasSize(1);
		assertThat(errorHandler.failures).isEmpty();
	}

	private void openSession(int maxRows) {
		sessionFactory = buildSessionFactory(maxRows);
		session = sessionFactory.openSession(true);
		mapper = session.getMapper(TestUserMapper.class);
	}

	private void reopenSession(int maxRows, boolean autoCommit) {
		if (session != null) {
			session.close();
		}
		sessionFactory = buildSessionFactory(maxRows);
		session = sessionFactory.openSession(autoCommit);
		mapper = session.getMapper(TestUserMapper.class);
		recordingHandler.clear();
		errorHandler.failures.clear();
	}

	private void useMySqlProductionDialect() {
		dialect.delegate = new MySqlH2Dialect();
		reopenSession(10, true);
	}

	private SqlSessionFactory buildSessionFactory(int maxRows) {
		DataChangeConfiguration properties = new DataChangeConfiguration();
		properties.setEnabled(true);
		properties.setMaxSnapshotRows(maxRows);
		properties.setTables(Arrays.asList(userMonitor(), tenantMonitor()));
		properties.validate();

		List<DataChangeFrameworkAdapter> adapters = Collections.<DataChangeFrameworkAdapter>singletonList(
				new MybatisDataChangeFrameworkAdapter());
		DataChangeDispatcher dispatcher = new DataChangeDispatcher(
				Arrays.<DataChangeHandler>asList(failingHandler, recordingHandler), errorHandler);
		DataChangeEngine engine = new DataChangeEngine(properties,
				new TableMonitorRegistry(properties.getTables()), new DataChangeFrameworkAdapterRegistry(adapters),
				new DataChangeDialectRegistry(Collections.<DataChangeDialect>singletonList(dialect)),
				new JdbcTableMetadataResolver(), new SnapshotService(), new ChangeDiffEngine(), dispatcher, errorHandler);

		Environment environment = new Environment("h2", new JdbcTransactionFactory(), dataSource);
		Configuration configuration = new Configuration(environment);
		configuration.setMapUnderscoreToCamelCase(true);
		parseMapper(configuration);
		configuration.addInterceptor(new DataChangeInterceptor(properties, engine));
		return new SqlSessionFactoryBuilder().build(configuration);
	}

	private void parseMapper(Configuration configuration) {
		String resource = "com/xiongx9527/fw/plus/mybatis/datachange/interceptor/fixture/TestUserMapper.xml";
		InputStream input = DataChangeInterceptorH2Test.class.getClassLoader().getResourceAsStream(resource);
		if (input == null) {
			throw new IllegalStateException("Missing mapper fixture");
		}
		new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
	}

	private TableMonitor userMonitor() {
		TableMonitor monitor = new TableMonitor();
		monitor.setName("SYS_USER");
		monitor.setKeyColumns(Collections.singletonList("ID"));
		monitor.setIncludeColumns(Arrays.asList("NAME", "DELETED"));
		LogicalDeleteConfiguration logicalDelete = new LogicalDeleteConfiguration();
		logicalDelete.setColumn("DELETED");
		logicalDelete.setDeletedValue(1);
		logicalDelete.setNotDeletedValue(0);
		monitor.setLogicalDelete(logicalDelete);
		return monitor;
	}

	private TableMonitor tenantMonitor() {
		TableMonitor monitor = new TableMonitor();
		monitor.setName("TENANT_USER");
		monitor.setKeyColumns(Arrays.asList("TENANT_ID", "USER_ID"));
		monitor.setIncludeColumns(Collections.singletonList("NAME"));
		return monitor;
	}

	private void initializeSchema() throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE sys_user (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, "
					+ "name VARCHAR(64) DEFAULT 'database-default', deleted INT DEFAULT 0 NOT NULL)");
			statement.execute("CREATE TABLE ignored_user (id BIGINT PRIMARY KEY, name VARCHAR(64))");
			statement.execute("CREATE TABLE tenant_user (tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, "
					+ "name VARCHAR(64), PRIMARY KEY (tenant_id, user_id))");
		}
	}

	private void insertUser(long id, String name, int deleted) throws Exception {
		execute("INSERT INTO sys_user(id, name, deleted) VALUES(" + id + ", '" + name + "', " + deleted + ")");
		recordingHandler.clear();
	}

	private void insertIgnoredUser(long id, String name) throws Exception {
		execute("INSERT INTO ignored_user(id, name) VALUES(" + id + ", '" + name + "')");
	}

	private void insertTenantUser(long tenantId, long userId, String name) throws Exception {
		execute("INSERT INTO tenant_user(tenant_id, user_id, name) VALUES(" + tenantId + ", " + userId
				+ ", '" + name + "')");
		recordingHandler.clear();
	}

	private void execute(String sql) throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			statement.executeUpdate(sql);
		}
	}

	private String selectString(String sql) throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
			 ResultSet resultSet = statement.executeQuery(sql)) {
			assertThat(resultSet.next()).isTrue();
			return resultSet.getString(1);
		}
	}

	private RowChange onlyRow(DataChangeEvent event) {
		StatementChange statement = onlyStatement(event);
		assertThat(statement.getTables()).hasSize(1);
		TableChange table = statement.getTables().get(0);
		assertThat(table.getRows()).hasSize(1);
		return table.getRows().get(0);
	}

	private StatementChange onlyStatement(DataChangeEvent event) {
		assertThat(event.getStatements()).hasSize(1);
		return event.getStatements().get(0);
	}

	private FieldChange field(RowChange row, String column) {
		return row.getFields().stream().filter(change -> column.equalsIgnoreCase(change.getColumnName()))
				.findFirst().orElseThrow(() -> new AssertionError("Missing field " + column));
	}

	private void assertCode(Throwable failure, DataChangeErrorCode code) {
		Throwable current = failure;
		while (current != null && !(current instanceof DataChangeException)) {
			current = current.getCause();
		}
		assertThat(current).isInstanceOf(DataChangeException.class);
		assertThat(((DataChangeException) current).getCode()).isEqualTo(code);
	}

	private static final class RecordingHandler implements DataChangeHandler {
		private final List<DataChangeEvent> events = new ArrayList<DataChangeEvent>();

		@Override
		public boolean supports(DataChangeEvent event) { return true; }

		@Override
		public void handle(DataChangeEvent event) { events.add(event); }

		@Override
		public int getOrder() { return 10; }

		private DataChangeEvent singleEvent() {
			assertThat(events).hasSize(1);
			return events.get(0);
		}

		private void clear() { events.clear(); }
	}

	private static final class RecordingErrorHandler implements DataChangeErrorHandler {
		private final List<DataChangeFailure> failures = new ArrayList<DataChangeFailure>();

		@Override
		public void handle(DataChangeFailure failure) { failures.add(failure); }
	}

	private static final class ToggleFailingHandler implements DataChangeHandler {
		private boolean fail;
		private TestUserMapper nestedMapper;
		private TestUserMapper supportsMapper;
		private boolean insideSupportsMapper;

		@Override
		public boolean supports(DataChangeEvent event) {
			if (supportsMapper != null && !insideSupportsMapper) {
				insideSupportsMapper = true;
				try {
					supportsMapper.rename(2L, "nested-after");
				} finally {
					insideSupportsMapper = false;
				}
			}
			return fail || nestedMapper != null;
		}

		@Override
		public void handle(DataChangeEvent event) {
			if (nestedMapper != null) {
				nestedMapper.insertExplicit(1L, "duplicate");
			}
			if (fail) {
				throw new IllegalStateException("handler fixture failure");
			}
		}

		@Override
		public int getOrder() { return 0; }
	}

	private static final class ToggleH2Dialect implements DataChangeDialect {
		private DataChangeDialect delegate = new H2DataChangeDialect();
		private boolean failAfterSnapshot;

		@Override
		public boolean supports(DatabaseMetadata metadata) { return delegate.supports(metadata); }

		@Override
		public TableIdentifier normalize(TableIdentifier table) { return delegate.normalize(table); }

		@Override
		public List<AnalyzedStatement> analyze(BoundSqlContext context) { return delegate.analyze(context); }

		@Override
		public List<String> resolveIdentityColumns(Connection connection, ChangeTarget target,
				List<String> defaultColumns) {
			return delegate.resolveIdentityColumns(connection, target, defaultColumns);
		}

		@Override
		public SnapshotQuery buildBeforeQuery(ChangeTarget target) { return delegate.buildBeforeQuery(target); }

		@Override
		public SnapshotQuery buildAfterQuery(ChangeTarget target, List<RowIdentity> identities) {
			return failAfterSnapshot
					? new SnapshotQuery("SELECT missing_column FROM missing_table", Collections.emptyList())
					: delegate.buildAfterQuery(target, identities);
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

	private static final class MySqlH2Dialect extends MySqlDataChangeDialect {
		@Override
		public boolean supports(DatabaseMetadata metadata) {
			return metadata != null && "h2".equals(metadata.getProductName());
		}
	}
}
