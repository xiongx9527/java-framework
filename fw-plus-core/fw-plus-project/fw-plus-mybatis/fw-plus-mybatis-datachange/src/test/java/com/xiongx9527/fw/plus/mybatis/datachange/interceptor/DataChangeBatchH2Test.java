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
import org.apache.ibatis.mapping.Environment;
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataChangeBatchH2Test {
	private static final AtomicInteger DATABASE_SEQUENCE = new AtomicInteger();

	private JdbcDataSource dataSource;
	private SqlSessionFactory factory;
	private RecordingHandler recordingHandler;
	private RecordingErrorHandler errorHandler;
	private FailingHandler failingHandler;

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
		DataChangeEngine engine = new DataChangeEngine(properties,
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
		configuration.addInterceptor(new DataChangeInterceptor(properties, engine));
		return new SqlSessionFactoryBuilder().build(configuration);
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
