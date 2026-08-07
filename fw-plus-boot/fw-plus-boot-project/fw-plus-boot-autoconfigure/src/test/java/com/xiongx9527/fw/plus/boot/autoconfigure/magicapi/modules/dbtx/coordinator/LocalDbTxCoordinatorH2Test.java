package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.coordinator;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxAwareDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.datasource.DbTxDataSourceAdapter;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.dbtx.model.DbTxDefinition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class LocalDbTxCoordinatorH2Test {

	private DbTxAwareDataSource dataSourceA;

	private DbTxAwareDataSource dataSourceB;

	private JdbcTemplate jdbcA;

	private JdbcTemplate jdbcB;

	private LocalDbTxCoordinator coordinator;

	@BeforeEach
	void setUp() {
		dataSourceA = managedH2DataSource("a");
		dataSourceB = managedH2DataSource("b");
		jdbcA = new JdbcTemplate(dataSourceA);
		jdbcB = new JdbcTemplate(dataSourceB);
		jdbcA.execute("create table tx_test(id integer primary key)");
		jdbcB.execute("create table tx_test(id integer primary key)");
		MagicDynamicDataSource dynamicDataSource = new MagicDynamicDataSource();
		dynamicDataSource.put("a", dataSourceA);
		dynamicDataSource.put("b", dataSourceB);
		coordinator = new LocalDbTxCoordinator(dynamicDataSource);
	}

	@AfterEach
	void verifyThreadIsClean() {
		assertThat(TransactionSynchronizationManager.getResourceMap()).isEmpty();
		assertThat(TransactionSynchronizationManager.isSynchronizationActive()).isFalse();
		assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
	}

	@Test
	void commitsBothIndependentH2DataSources() throws Throwable {
		Object result = coordinator.execute(definition(), () -> {
			assertThat(TransactionSynchronizationManager.hasResource(dataSourceA)).isTrue();
			assertThat(TransactionSynchronizationManager.hasResource(dataSourceB)).isTrue();
			jdbcA.update("insert into tx_test(id) values (1)");
			jdbcB.update("insert into tx_test(id) values (1)");
			return "ok";
		});

		assertThat(result).isEqualTo("ok");
		assertThat(count(jdbcA)).isOne();
		assertThat(count(jdbcB)).isOne();
	}

	@Test
	void rollsBackBothIndependentH2DataSourcesWhenTheCallbackFails() {
		IllegalStateException callbackFailure = new IllegalStateException("callback failed");

		Throwable thrown = catchThrowable(() -> coordinator.execute(definition(), () -> {
			jdbcA.update("insert into tx_test(id) values (1)");
			jdbcB.update("insert into tx_test(id) values (1)");
			throw callbackFailure;
		}));

		assertThat(thrown).isSameAs(callbackFailure);
		assertThat(count(jdbcA)).isZero();
		assertThat(count(jdbcB)).isZero();
	}

	private static DbTxDefinition definition() {
		return new DbTxDefinition("local", Arrays.asList("b", "a"), 60000, "h2-test");
	}

	private static int count(JdbcTemplate jdbcTemplate) {
		return jdbcTemplate.queryForObject("select count(*) from tx_test", Integer.class);
	}

	private static DriverManagerDataSource h2DataSource(String name) {
		return new DriverManagerDataSource("jdbc:h2:mem:dbtx_h2_" + name + "_" + UUID.randomUUID()
				+ ";DB_CLOSE_DELAY=-1", "sa", "");
	}

	private static DbTxAwareDataSource managedH2DataSource(String name) {
		return new DbTxAwareDataSource(name, h2DataSource(name),
				Collections.<DbTxDataSourceAdapter>emptyList());
	}
}
