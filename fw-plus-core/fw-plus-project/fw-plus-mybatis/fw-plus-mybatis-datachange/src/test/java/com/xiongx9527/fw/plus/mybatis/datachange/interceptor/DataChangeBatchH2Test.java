package com.xiongx9527.fw.plus.mybatis.datachange.interceptor;

import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.DataChangeFrameworkAdapterRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.adapter.MybatisDataChangeFrameworkAdapter;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeEvent;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeFailure;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeHandler;
import com.xiongx9527.fw.plus.mybatis.datachange.api.RowChange;
import com.xiongx9527.fw.plus.mybatis.datachange.config.DataChangeConfiguration;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitorRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.DataChangeDialectRegistry;
import com.xiongx9527.fw.plus.mybatis.datachange.dialect.H2DataChangeDialect;
import com.xiongx9527.fw.plus.mybatis.datachange.diff.ChangeDiffEngine;
import com.xiongx9527.fw.plus.mybatis.datachange.dispatch.DataChangeDispatcher;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.fixture.TestUserMapper;
import com.xiongx9527.fw.plus.mybatis.datachange.interceptor.fixture.TestUser;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.JdbcTableMetadataResolver;
import com.xiongx9527.fw.plus.mybatis.datachange.snapshot.SnapshotService;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.executor.BatchResult;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Plugin;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DataChangeBatchH2Test {
	private static final AtomicInteger DATABASE_SEQUENCE = new AtomicInteger();

	private JdbcDataSource dataSource;
	private SqlSessionFactory factory;
	private RecordingHandler recordingHandler;
	private RecordingErrorHandler errorHandler;
	private FailingHandler failingHandler;
	private DataChangeEngine engine;
	private DataChangeInterceptor interceptor;

	@BeforeEach
	void setUp() throws Exception {
		dataSource = new JdbcDataSource();
		dataSource.setURL("jdbc:h2:mem:datachange_batch_" + DATABASE_SEQUENCE.incrementAndGet()
				+ ";DB_CLOSE_DELAY=-1");
		initializeSchema();
		recordingHandler = new RecordingHandler();
		errorHandler = new RecordingErrorHandler();
		failingHandler = new FailingHandler();
		factory = buildSessionFactory();
	}

	@Test
	void batchCollapsesIntermediateChangesForSameRow() throws Exception {
		insertUser(1L, "before");

		try (SqlSession session = factory.openSession(ExecutorType.BATCH, false)) {
			TestUserMapper mapper = session.getMapper(TestUserMapper.class);
			mapper.rename(1L, "middle");
			mapper.rename(1L, "final");
			session.flushStatements();
			session.commit();
		}

		RowChange row = onlyRow(recordingHandler.singleEvent());
		assertThat(recordingHandler.events.get(0).getStatements().get(0).getAffectedRows()).isEqualTo(2);
		assertThat(row.getBefore()).containsEntry("NAME", "before");
		assertThat(row.getAfter()).containsEntry("NAME", "final");
	}

	@Test
	void flushesPendingBatchBeforeCommit() throws Exception {
		insertUser(1L, "before");

		try (SqlSession session = factory.openSession(ExecutorType.BATCH, false)) {
			session.getMapper(TestUserMapper.class).rename(1L, "after");
			session.commit();
		}

		assertThat(recordingHandler.events).hasSize(1);
		assertThat(selectName(1L)).isEqualTo("after");
	}

	@Test
	void flushesPendingBatchBeforeQuery() throws Exception {
		insertUser(1L, "before");

		try (SqlSession session = factory.openSession(ExecutorType.BATCH, false)) {
			TestUserMapper mapper = session.getMapper(TestUserMapper.class);
			mapper.rename(1L, "after");
			assertThat(mapper.findName(1L)).isEqualTo("after");
			session.commit();
		}

		assertThat(recordingHandler.events).hasSize(1);
	}

	@Test
	void discardsPendingBatchOnRollback() throws Exception {
		insertUser(1L, "before");

		try (SqlSession session = factory.openSession(ExecutorType.BATCH, false)) {
			session.getMapper(TestUserMapper.class).rename(1L, "after");
			session.rollback();
		}

		assertThat(recordingHandler.events).isEmpty();
		assertThat(selectName(1L)).isEqualTo("before");
	}

	@Test
	void groupsCompositeKeyBatchByIdentity() throws Exception {
		insertTenantUser(7L, 11L, "before");

		try (SqlSession session = factory.openSession(ExecutorType.BATCH, false)) {
			TestUserMapper mapper = session.getMapper(TestUserMapper.class);
			mapper.renameComposite(7L, 11L, "middle");
			mapper.renameComposite(7L, 11L, "final");
			session.flushStatements();
			session.commit();
		}

		RowChange row = onlyRow(recordingHandler.singleEvent());
		assertThat(row.getIdentity().getKeyValues())
				.containsEntry("TENANT_ID", 7L).containsEntry("USER_ID", 11L);
		assertThat(row.getAfter()).containsEntry("NAME", "final");
	}

	@Test
	void oneActualFlushAcrossStatementGroupsEmitsOneEvent() throws Exception {
		insertUser(1L, "user-before");
		insertTenantUser(7L, 11L, "tenant-before");

		try (SqlSession session = factory.openSession(ExecutorType.BATCH, false)) {
			TestUserMapper mapper = session.getMapper(TestUserMapper.class);
			mapper.rename(1L, "user-after");
			mapper.renameComposite(7L, 11L, "tenant-after");
			session.flushStatements();
			session.commit();
		}

		assertThat(recordingHandler.events).hasSize(1);
		assertThat(recordingHandler.events.get(0).getStatements()).hasSize(2);
	}

	@Test
	void batchInsertUsesGeneratedKeysAfterActualFlush() {
		TestUser first = new TestUser(null, "first", null);
		TestUser second = new TestUser(null, "second", null);

		try (SqlSession session = factory.openSession(ExecutorType.BATCH, false)) {
			TestUserMapper mapper = session.getMapper(TestUserMapper.class);
			mapper.insertDefault(first);
			mapper.insertDefault(second);
			session.flushStatements();
			session.commit();
		}

		assertThat(first.getId()).isPositive();
		assertThat(second.getId()).isPositive().isNotEqualTo(first.getId());
		assertThat(recordingHandler.singleEvent().getStatements().get(0).getTables().get(0).getRows()).hasSize(2);
	}

	@Test
	void transactionalHandlerFailurePreventsCommit() throws Exception {
		insertUser(1L, "before");
		failingHandler.fail = true;

		try (SqlSession session = factory.openSession(ExecutorType.BATCH, false)) {
			session.getMapper(TestUserMapper.class).rename(1L, "after");
			assertThatThrownBy(session::commit);
			session.rollback();
		}

		assertThat(selectName(1L)).isEqualTo("before");
		assertThat(errorHandler.failures).isEmpty();
	}

	@Test
	void bestEffortHandlerFailureKeepsFlushedBusinessRows() throws Exception {
		insertUser(1L, "before");
		failingHandler.fail = true;

		try (SqlSession session = factory.openSession(ExecutorType.BATCH, true)) {
			session.getMapper(TestUserMapper.class).rename(1L, "after");
			session.flushStatements();
		}

		assertThat(selectName(1L)).isEqualTo("after");
		assertThat(errorHandler.failures).hasSize(1);
	}

	@Test
	void batchRebuildsPlanFromBoundSqlAfterPrepare() throws Throwable {
		insertUser(1L, "first-before");
		insertUser(2L, "second-before");
		Configuration mybatis = new Configuration();
		String preparedSql = "UPDATE sys_user SET name='ignored' WHERE id=1";
		String batchedSql = "UPDATE sys_user SET name='second-after' WHERE id=2";
		MappedStatement mappedStatement = new MappedStatement.Builder(mybatis, "mutableBatch",
				new StaticSqlSource(mybatis, preparedSql), SqlCommandType.UPDATE).build();
		BoundSql prepared = new BoundSql(mybatis, preparedSql, Collections.emptyList(), null);
		BoundSql batched = new BoundSql(mybatis, batchedSql, Collections.emptyList(), null);
		AtomicReference<BoundSql> currentSql = new AtomicReference<BoundSql>(prepared);
		StatementHandler handler = mock(StatementHandler.class);
		Statement batchStatement = mock(Statement.class);
		Executor owner = mock(Executor.class);
		when(handler.getBoundSql()).thenAnswer(invocation -> currentSql.get());
		when(handler.prepare(org.mockito.ArgumentMatchers.any(Connection.class),
				org.mockito.ArgumentMatchers.isNull())).thenReturn(batchStatement);
		DataChangeExecutionContext context = new DataChangeExecutionContext(mappedStatement, null,
				new MybatisDataChangeFrameworkAdapter(), owner);

		try (Connection connection = dataSource.getConnection();
				 DataChangeExecutionContextHolder.Scope ignored = DataChangeExecutionContextHolder.push(context)) {
			interceptor.intercept(new Invocation(handler,
					StatementHandler.class.getMethod("prepare", Connection.class, Integer.class),
					new Object[] { connection, null }));
			currentSql.set(batched);
			when(batchStatement.getConnection()).thenReturn(connection);
			interceptor.intercept(new Invocation(handler,
					StatementHandler.class.getMethod("batch", Statement.class),
					new Object[] { batchStatement }));
			try (Statement update = connection.createStatement()) {
				update.executeUpdate(batchedSql);
			}
			BatchResult batchResult = new BatchResult(mappedStatement, batchedSql);
			batchResult.setUpdateCounts(new int[] { 1 });
			when(owner.flushStatements()).thenReturn(Collections.singletonList(batchResult));
			interceptor.intercept(new Invocation(owner, Executor.class.getMethod("flushStatements"), new Object[0]));
		}

		RowChange row = onlyRow(recordingHandler.singleEvent());
		assertThat(row.getIdentity().getKeyValues()).containsEntry("ID", 2L);
		assertThat(row.getBefore()).containsEntry("NAME", "second-before");
		assertThat(row.getAfter()).containsEntry("NAME", "second-after");
	}

	@Test
	void closeReleasesPendingBatchWithoutExplicitRollback() throws Exception {
		insertUser(1L, "before");
		SqlSession session = factory.openSession(ExecutorType.BATCH, false);
		Object owner = executionOwner(session);
		session.getMapper(TestUserMapper.class).rename(1L, "after");
		assertThat(hasPending(owner)).isTrue();

		session.close();

		assertThat(hasPending(owner)).isFalse();
		assertThat(recordingHandler.events).isEmpty();
		assertThat(selectName(1L)).isEqualTo("before");
	}

	@Test
	void secondEntryPostProcessingFailureSuppressesTheWholeFlushEvent() throws Exception {
		insertUser(1L, "first-after");
		Configuration mybatis = new Configuration();
		String sql = "INSERT INTO sys_user(id, name) VALUES(?, ?)";
		MappedStatement mappedStatement = new MappedStatement.Builder(mybatis, "batchInsertPostFailure",
				new StaticSqlSource(mybatis, sql), SqlCommandType.INSERT).build();
		DataChangeFrameworkAdapter adapter = new MybatisDataChangeFrameworkAdapter();
		Object owner = new Object();
		DataChangeExecutionContext first = new DataChangeExecutionContext(mappedStatement,
				Collections.<String, Object>singletonMap("ID", 1L), adapter, owner);
		DataChangeExecutionContext second = new DataChangeExecutionContext(mappedStatement,
				Collections.<String, Object>singletonMap("NAME", "missing-key"), adapter, owner);
		Statement firstStatement = mock(Statement.class);
		Statement secondStatement = mock(Statement.class);
		DataChangeBatchCoordinator coordinator = new DataChangeBatchCoordinator(engine);

		try (Connection connection = dataSource.getConnection()) {
			engine.initializeConnection(first, connection);
			engine.initializeConnection(second, connection);
			List<StatementChangePlan> firstPlans = engine.beforeStatement(first,
					new com.xiongx9527.fw.plus.mybatis.datachange.dialect.BoundSqlContext(
							mappedStatement.getBoundSql(first.getParameterObject())));
			List<StatementChangePlan> secondPlans = engine.beforeStatement(second,
					new com.xiongx9527.fw.plus.mybatis.datachange.dialect.BoundSqlContext(
							mappedStatement.getBoundSql(second.getParameterObject())));
			first.markBatch(firstStatement, sql);
			second.markBatch(secondStatement, sql);
			coordinator.collect(first, firstPlans);
			coordinator.collect(second, secondPlans);
			BatchResult result = new BatchResult(mappedStatement, sql);
			result.setUpdateCounts(new int[] { 1, 1 });

			coordinator.flush(new DataChangeExecutionContext(null, null, null, owner),
					Collections.singletonList(result));
		}

		assertThat(recordingHandler.events).isEmpty();
		assertThat(errorHandler.failures).isNotEmpty();
	}

	private SqlSessionFactory buildSessionFactory() {
		DataChangeConfiguration properties = new DataChangeConfiguration();
		properties.setEnabled(true);
		properties.setTables(Arrays.asList(monitor("SYS_USER", Collections.singletonList("ID")),
				monitor("TENANT_USER", Arrays.asList("TENANT_ID", "USER_ID"))));
		properties.validate();
		List<DataChangeFrameworkAdapter> adapters = Collections.<DataChangeFrameworkAdapter>singletonList(
				new MybatisDataChangeFrameworkAdapter());
		DataChangeDispatcher dispatcher = new DataChangeDispatcher(
				Arrays.<DataChangeHandler>asList(failingHandler, recordingHandler), errorHandler);
		engine = new DataChangeEngine(properties,
				new TableMonitorRegistry(properties.getTables()), new DataChangeFrameworkAdapterRegistry(adapters),
				new DataChangeDialectRegistry(Collections.<DataChangeDialect>singletonList(new H2DataChangeDialect())),
				new JdbcTableMetadataResolver(), new SnapshotService(), new ChangeDiffEngine(), dispatcher, errorHandler);
		Configuration configuration = new Configuration(
				new Environment("h2", new JdbcTransactionFactory(), dataSource));
		configuration.setMapUnderscoreToCamelCase(true);
		String resource = "com/xiongx9527/fw/plus/mybatis/datachange/interceptor/fixture/TestUserMapper.xml";
		InputStream input = DataChangeBatchH2Test.class.getClassLoader().getResourceAsStream(resource);
		if (input == null) { throw new IllegalStateException("Missing mapper fixture"); }
		new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments()).parse();
		interceptor = new DataChangeInterceptor(properties, engine);
		configuration.addInterceptor(interceptor);
		return new SqlSessionFactoryBuilder().build(configuration);
	}

	private Object executionOwner(SqlSession session) throws Exception {
		Field executor = session.getClass().getDeclaredField("executor");
		executor.setAccessible(true);
		Object owner = executor.get(session);
		if (owner != null && Proxy.isProxyClass(owner.getClass())) {
			Object handler = Proxy.getInvocationHandler(owner);
			if (handler instanceof Plugin) {
				Field target = Plugin.class.getDeclaredField("target");
				target.setAccessible(true);
				owner = target.get(handler);
			}
		}
		return owner;
	}

	private boolean hasPending(Object owner) throws Exception {
		Field coordinator = DataChangeInterceptor.class.getDeclaredField("batchCoordinator");
		coordinator.setAccessible(true);
		return ((DataChangeBatchCoordinator) coordinator.get(interceptor))
				.hasPending(new DataChangeExecutionContext(null, null, null, owner));
	}

	private TableMonitor monitor(String table, List<String> keys) {
		TableMonitor monitor = new TableMonitor();
		monitor.setName(table);
		monitor.setKeyColumns(keys);
		monitor.setIncludeColumns(Collections.singletonList("NAME"));
		return monitor;
	}

	private void initializeSchema() throws Exception {
		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE sys_user (id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY, "
					+ "name VARCHAR(64))");
			statement.execute("CREATE TABLE tenant_user (tenant_id BIGINT NOT NULL, user_id BIGINT NOT NULL, "
					+ "name VARCHAR(64), PRIMARY KEY (tenant_id, user_id))");
		}
	}

	private void insertUser(long id, String name) throws Exception {
		try (Connection connection = dataSource.getConnection();
				 PreparedStatement statement = connection.prepareStatement(
						 "INSERT INTO sys_user(id, name) VALUES(?, ?)")) {
			statement.setLong(1, id);
			statement.setString(2, name);
			statement.executeUpdate();
		}
		recordingHandler.events.clear();
	}

	private void insertTenantUser(long tenantId, long userId, String name) throws Exception {
		try (Connection connection = dataSource.getConnection();
				 PreparedStatement statement = connection.prepareStatement(
						 "INSERT INTO tenant_user(tenant_id, user_id, name) VALUES(?, ?, ?)")) {
			statement.setLong(1, tenantId);
			statement.setLong(2, userId);
			statement.setString(3, name);
			statement.executeUpdate();
		}
		recordingHandler.events.clear();
	}

	private String selectName(long id) throws Exception {
		try (Connection connection = dataSource.getConnection();
				 PreparedStatement statement = connection.prepareStatement("SELECT name FROM sys_user WHERE id = ?")) {
			statement.setLong(1, id);
			try (ResultSet resultSet = statement.executeQuery()) {
				assertThat(resultSet.next()).isTrue();
				return resultSet.getString(1);
			}
		}
	}

	private RowChange onlyRow(DataChangeEvent event) {
		assertThat(event.getStatements()).hasSize(1);
		assertThat(event.getStatements().get(0).getTables()).hasSize(1);
		assertThat(event.getStatements().get(0).getTables().get(0).getRows()).hasSize(1);
		return event.getStatements().get(0).getTables().get(0).getRows().get(0);
	}

	private static final class RecordingHandler implements DataChangeHandler {
		private final List<DataChangeEvent> events = new ArrayList<DataChangeEvent>();
		@Override public boolean supports(DataChangeEvent event) { return true; }
		@Override public void handle(DataChangeEvent event) { events.add(event); }
		@Override public int getOrder() { return 10; }
		private DataChangeEvent singleEvent() {
			assertThat(events).hasSize(1);
			return events.get(0);
		}
	}

	private static final class RecordingErrorHandler implements DataChangeErrorHandler {
		private final List<DataChangeFailure> failures = new ArrayList<DataChangeFailure>();
		@Override public void handle(DataChangeFailure failure) { failures.add(failure); }
	}

	private static final class FailingHandler implements DataChangeHandler {
		private boolean fail;
		@Override public boolean supports(DataChangeEvent event) { return fail; }
		@Override public void handle(DataChangeEvent event) { throw new IllegalStateException("handler fixture failure"); }
		@Override public int getOrder() { return 0; }
	}
}
