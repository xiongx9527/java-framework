package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitor;
import com.xiongx9527.fw.plus.mybatis.datachange.config.TableMonitorRegistry;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SqlTargetDetectionTest {
	private final JSqlParserTargetDetector detector = new JSqlParserTargetDetector();

	@Test
	void distinguishesDmlTargetsFromCallAndNonDml() {
		SqlTargetDetection detection = detector.detect(
				"SELECT * FROM sys_user; CALL refresh_cache(); UPDATE audit.sys_user SET name=? WHERE id=?");

		assertThat(detection.getStatements()).hasSize(3);
		assertThat(detection.getStatements().get(0).isAuditable()).isFalse();
		assertThat(detection.getStatements().get(1).isAuditable()).isFalse();
		assertThat(detection.getStatements().get(2).getStatementIndex()).isEqualTo(2);
		assertThat(detection.getStatements().get(2).isAuditable()).isTrue();
		assertThat(detection.getStatements().get(2).isTargetCertain()).isTrue();
		assertThat(detection.getStatements().get(2).getTargetTables()).singleElement()
				.satisfies(table -> {
					assertThat(table.getSchema()).isEqualTo("audit");
					assertThat(table.getTable()).isEqualTo("sys_user");
				});
	}

	@Test
	void rejectsMalformedDmlBeforeDialectSelection() {
		assertThatThrownBy(() -> detector.detect("UPDATE ???"))
				.isInstanceOf(DataChangeException.class)
				.extracting(exception -> ((DataChangeException) exception).getCode())
				.isEqualTo(DataChangeErrorCode.UNSUPPORTED_SQL);
	}

	@Test
	void rejectsH2MergeShortcutWithoutTextFallback() {
		assertThatThrownBy(() -> detector.detect("MERGE INTO sys_user KEY(id) VALUES (?,?)"))
				.isInstanceOf(DataChangeException.class)
				.extracting(exception -> ((DataChangeException) exception).getCode())
				.isEqualTo(DataChangeErrorCode.UNSUPPORTED_SQL);
	}

	@ParameterizedTest
	@ValueSource(strings = {
			"INSERT INTO sys_user(id,name) VALUES (?,?),(?,?)",
			"INSERT INTO sys_user(id,name) SELECT id,name FROM source_user"
	})
	void rejectsInsertShapesThatCannotBeReliablyModelled(String sql) {
		assertThatThrownBy(() -> detector.detect(sql))
				.isInstanceOf(DataChangeException.class)
				.extracting(exception -> ((DataChangeException) exception).getCode())
				.isEqualTo(DataChangeErrorCode.UNSUPPORTED_SQL);
	}

	@Test
	void bypassesUnknownDialectWhenAllCertainTargetsAreUnmonitored() throws Exception {
		DataChangeDialectRegistry registry = new DataChangeDialectRegistry(Collections.<DataChangeDialect>emptyList());

		List<AnalyzedStatement> plan = registry.analyze(connection("UnknownDB"),
				context("UPDATE other_table SET name=? WHERE id=?", "name", "id"), monitors("monitored_table"));

		assertThat(plan).isEmpty();
	}

	@Test
	void rejectsUnknownDialectWhenTargetMayBeMonitored() throws Exception {
		DataChangeDialectRegistry registry = new DataChangeDialectRegistry(Collections.<DataChangeDialect>emptyList());

		assertThatThrownBy(() -> registry.analyze(connection("UnknownDB"),
				context("UPDATE monitored_table SET name=? WHERE id=?", "name", "id"), monitors("monitored_table")))
				.isInstanceOf(DataChangeException.class)
				.extracting(exception -> ((DataChangeException) exception).getCode())
				.isEqualTo(DataChangeErrorCode.UNSUPPORTED_DIALECT);
	}

	@Test
	void rejectsMalformedDmlBeforeUnknownDialectFastPath() throws Exception {
		DataChangeDialectRegistry registry = new DataChangeDialectRegistry(Collections.<DataChangeDialect>emptyList());

		assertThatThrownBy(() -> registry.analyze(connection("UnknownDB"), context("UPDATE ???"), monitors("sys_user")))
				.isInstanceOf(DataChangeException.class)
				.extracting(exception -> ((DataChangeException) exception).getCode())
				.isEqualTo(DataChangeErrorCode.UNSUPPORTED_SQL);
	}

	private TableMonitorRegistry monitors(String tableName) {
		TableMonitor monitor = new TableMonitor();
		monitor.setName(tableName);
		return new TableMonitorRegistry(Arrays.asList(monitor));
	}

	private BoundSqlContext context(String sql, String... properties) {
		Configuration configuration = new Configuration();
		List<ParameterMapping> mappings = new java.util.ArrayList<ParameterMapping>();
		for (String property : properties) {
			mappings.add(new ParameterMapping.Builder(configuration, property, Object.class).build());
		}
		return new BoundSqlContext(new BoundSql(configuration, sql, mappings, new Object()));
	}

	private Connection connection(String productName) throws Exception {
		Connection connection = Mockito.mock(Connection.class);
		DatabaseMetaData metadata = Mockito.mock(DatabaseMetaData.class);
		Mockito.when(connection.getMetaData()).thenReturn(metadata);
		Mockito.when(metadata.getDatabaseProductName()).thenReturn(productName);
		Mockito.when(metadata.getDatabaseMajorVersion()).thenReturn(1);
		Mockito.when(metadata.getDatabaseMinorVersion()).thenReturn(0);
		return connection;
	}
}
