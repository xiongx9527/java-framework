package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MySqlDataChangeDialectTest extends AbstractDataChangeDialectContract {
	private final MySqlDataChangeDialect dialect = new MySqlDataChangeDialect();

	@Test
	void implementsSharedSnapshotContract() {
		assertCompositeKeyAfterQuery(dialect);
		assertBeforeQuery(dialect, "`id`", "LIMIT 1001", "FOR UPDATE");
	}

	@Test
	void recognizesMySqlAndAnalyzesOnDuplicateKey() {
		assertThat(dialect.supports(new DatabaseMetadata("MySQL", 8, 0))).isTrue();
		AnalyzedStatement statement = dialect.analyze(context("INSERT INTO audit_order(id,email,amount) VALUES(?,?,?) "
				+ "ON DUPLICATE KEY UPDATE amount=VALUES(amount)", "id", "email", "amount" )).get(0);
		assertThat(statement.getTargets()).singleElement()
				.satisfies(target -> assertThat(target.isUpsert()).isTrue());
	}

	@Test
	void resolvesSingleMatchingUniqueIndexFromJdbcMetadata() throws Exception {
		ChangeTarget target = dialect.analyze(context("INSERT INTO audit_order(id,email,amount) VALUES(?,?,?) "
				+ "ON DUPLICATE KEY UPDATE amount=VALUES(amount)", "id", "email", "amount")).get(0).getTargets().get(0);
		try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:mysql_one;DB_CLOSE_DELAY=-1");
			 Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE audit_order(id BIGINT, email VARCHAR(100), amount DECIMAL, "
					+ "CONSTRAINT uk_email UNIQUE(email))");
			assertThat(dialect.resolveIdentityColumns(connection, target, java.util.Arrays.asList("id")))
					.extracting(value -> value.toLowerCase(java.util.Locale.ENGLISH)).containsExactly("email");
		}
	}

	@Test
	void rejectsAmbiguousMatchingUniqueIndexes() throws Exception {
		ChangeTarget target = dialect.analyze(context("INSERT INTO audit_order(id,email,amount) VALUES(?,?,?) "
				+ "ON DUPLICATE KEY UPDATE amount=VALUES(amount)", "id", "email", "amount")).get(0).getTargets().get(0);
		try (Connection connection = DriverManager.getConnection("jdbc:h2:mem:mysql_many;DB_CLOSE_DELAY=-1");
			 Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE audit_order(id BIGINT PRIMARY KEY, email VARCHAR(100) UNIQUE, amount DECIMAL)");
			assertThatThrownBy(() -> dialect.resolveOnDuplicateKeyColumns(connection, target))
					.isInstanceOf(DataChangeException.class)
					.extracting(exception -> ((DataChangeException) exception).getCode())
					.isEqualTo(DataChangeErrorCode.ROW_IDENTITY_UNAVAILABLE);
		}
	}

	@Test
	void standardSetContainsAllBuiltInDialects() {
		assertThat(StandardDialectSet.create()).extracting(Object::getClass).containsExactly(
				MySqlDataChangeDialect.class, MariaDbDataChangeDialect.class, PostgreSqlDataChangeDialect.class,
				OracleDataChangeDialect.class, SqlServerDataChangeDialect.class, H2DataChangeDialect.class);
	}

}
