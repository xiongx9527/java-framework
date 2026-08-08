package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
	void mapsTwoPartMysqlNameToCatalogWhenDriverUsesDatabaseCatalogs() throws Exception {
		Connection connection = mock(Connection.class);
		DatabaseMetaData metadata = uniqueIndexMetadata("email");
		when(connection.getMetaData()).thenReturn(metadata);
		when(metadata.supportsCatalogsInDataManipulation()).thenReturn(true);
		when(metadata.supportsSchemasInDataManipulation()).thenReturn(false);
		when(metadata.getCatalogTerm()).thenReturn("CATALOG");
		when(metadata.getSchemaTerm()).thenReturn("SCHEMA");

		assertThat(dialect.resolveOnDuplicateKeyColumns(connection, qualifiedTarget()))
				.containsExactly("email");

		verify(metadata).getIndexInfo("tenant_db", null, "audit_order", true, false);
	}

	@Test
	void mapsTwoPartMysqlNameToSchemaWhenDriverUsesDatabaseSchemas() throws Exception {
		Connection connection = mock(Connection.class);
		DatabaseMetaData metadata = uniqueIndexMetadata("email");
		when(connection.getMetaData()).thenReturn(metadata);
		when(metadata.supportsCatalogsInDataManipulation()).thenReturn(false);
		when(metadata.supportsSchemasInDataManipulation()).thenReturn(true);
		when(metadata.getCatalogTerm()).thenReturn("CATALOG");
		when(metadata.getSchemaTerm()).thenReturn("SCHEMA");

		assertThat(dialect.resolveOnDuplicateKeyColumns(connection, qualifiedTarget()))
				.containsExactly("email");

		verify(metadata).getIndexInfo(null, "tenant_db", "audit_order", true, false);
	}

	@Test
	void mapsTwoPartMysqlNameToSchemaFromConnectorSchemaTermsWhenCapabilitiesAreAmbiguous() throws Exception {
		Connection connection = mock(Connection.class);
		DatabaseMetaData metadata = uniqueIndexMetadata("email");
		when(connection.getMetaData()).thenReturn(metadata);
		when(metadata.supportsCatalogsInDataManipulation()).thenReturn(false);
		when(metadata.supportsSchemasInDataManipulation()).thenReturn(false);
		when(metadata.getSchemaTerm()).thenReturn("SCHEMA");
		when(metadata.getCatalogTerm()).thenReturn("CATALOG");

		assertThat(dialect.resolveOnDuplicateKeyColumns(connection, qualifiedTarget()))
				.containsExactly("email");

		verify(metadata).getIndexInfo(null, "tenant_db", "audit_order", true, false);
	}

	@Test
	void mapsTwoPartMysqlNameToCatalogFromConnectorCatalogTermsWhenCapabilitiesAreAmbiguous() throws Exception {
		Connection connection = mock(Connection.class);
		DatabaseMetaData metadata = uniqueIndexMetadata("email");
		when(connection.getMetaData()).thenReturn(metadata);
		when(metadata.supportsCatalogsInDataManipulation()).thenReturn(false);
		when(metadata.supportsSchemasInDataManipulation()).thenReturn(false);
		when(metadata.getSchemaTerm()).thenReturn("");
		when(metadata.getCatalogTerm()).thenReturn("database");

		assertThat(dialect.resolveOnDuplicateKeyColumns(connection, qualifiedTarget()))
				.containsExactly("email");

		verify(metadata).getIndexInfo("tenant_db", null, "audit_order", true, false);
	}

	@Test
	void rejectsAmbiguousNamespaceTermsWithoutLeakingMetadataFailure() throws Exception {
		Connection connection = mock(Connection.class);
		DatabaseMetaData metadata = uniqueIndexMetadata("email");
		when(connection.getMetaData()).thenReturn(metadata);
		when(metadata.supportsCatalogsInDataManipulation()).thenReturn(false);
		when(metadata.supportsSchemasInDataManipulation()).thenReturn(false);
		when(metadata.getSchemaTerm()).thenReturn("database");
		when(metadata.getCatalogTerm()).thenReturn("catalog");

		assertSanitized(connection);
		org.mockito.Mockito.verify(metadata, org.mockito.Mockito.never())
				.getIndexInfo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
						org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(true),
						org.mockito.ArgumentMatchers.eq(false));
	}

	@Test
	void usesCurrentCatalogForUnqualifiedNameWhenCatalogsAreSupported() throws Exception {
		Connection connection = mock(Connection.class);
		DatabaseMetaData metadata = uniqueIndexMetadata("email");
		when(connection.getMetaData()).thenReturn(metadata);
		when(metadata.supportsCatalogsInDataManipulation()).thenReturn(true);
		when(metadata.supportsSchemasInDataManipulation()).thenReturn(false);
		when(connection.getCatalog()).thenReturn("tenant_db");

		assertThat(dialect.resolveOnDuplicateKeyColumns(connection, unqualifiedTarget()))
				.containsExactly("email");

		verify(metadata).getIndexInfo("tenant_db", null, "audit_order", true, false);
	}

	@Test
	void usesCurrentSchemaForUnqualifiedNameWhenSchemasAreSupported() throws Exception {
		Connection connection = mock(Connection.class);
		DatabaseMetaData metadata = uniqueIndexMetadata("email");
		when(connection.getMetaData()).thenReturn(metadata);
		when(metadata.supportsCatalogsInDataManipulation()).thenReturn(false);
		when(metadata.supportsSchemasInDataManipulation()).thenReturn(true);
		when(connection.getSchema()).thenReturn("tenant_db");

		assertThat(dialect.resolveOnDuplicateKeyColumns(connection, unqualifiedTarget()))
				.containsExactly("email");

		verify(metadata).getIndexInfo(null, "tenant_db", "audit_order", true, false);
	}

	@Test
	void sanitizesMetadataQueryAndCloseFailuresAtIdentityBoundary() throws Exception {
		assertSanitized(metadataFailure(new SQLException("jdbc:secret-query")));
		assertSanitized(resultSetFailure(new SQLException("jdbc:secret-next"), null));
		assertSanitized(resultSetFailure(null, new IllegalStateException("jdbc:secret-close")));
		assertSanitized(metadataFailure(new IllegalStateException("jdbc:secret-runtime")));
	}

	@Test
	void standardSetContainsAllBuiltInDialects() {
		assertThat(StandardDialectSet.create()).extracting(Object::getClass).containsExactly(
				MySqlDataChangeDialect.class, MariaDbDataChangeDialect.class, PostgreSqlDataChangeDialect.class,
				OracleDataChangeDialect.class, SqlServerDataChangeDialect.class, H2DataChangeDialect.class);
	}

	private ChangeTarget qualifiedTarget() {
		return dialect.analyze(context("INSERT INTO tenant_db.audit_order(id,email,amount) VALUES(?,?,?) "
				+ "ON DUPLICATE KEY UPDATE amount=VALUES(amount)", "id", "email", "amount"))
				.get(0).getTargets().get(0);
	}

	private ChangeTarget unqualifiedTarget() {
		return dialect.analyze(context("INSERT INTO audit_order(id,email,amount) VALUES(?,?,?) "
				+ "ON DUPLICATE KEY UPDATE amount=VALUES(amount)", "id", "email", "amount"))
				.get(0).getTargets().get(0);
	}

	private DatabaseMetaData uniqueIndexMetadata(String column) throws Exception {
		DatabaseMetaData metadata = mock(DatabaseMetaData.class);
		ResultSet rows = mock(ResultSet.class);
		when(metadata.getIndexInfo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(true),
				org.mockito.ArgumentMatchers.eq(false))).thenReturn(rows);
		when(rows.next()).thenReturn(true, false);
		when(rows.getString("INDEX_NAME")).thenReturn("uk_email");
		when(rows.getString("COLUMN_NAME")).thenReturn(column);
		when(rows.getShort("ORDINAL_POSITION")).thenReturn((short) 1);
		return metadata;
	}

	private Connection metadataFailure(Exception failure) throws Exception {
		Connection connection = mock(Connection.class);
		when(connection.getMetaData()).thenThrow(failure);
		return connection;
	}

	private Connection resultSetFailure(Exception queryFailure, RuntimeException closeFailure) throws Exception {
		Connection connection = mock(Connection.class);
		DatabaseMetaData metadata = mock(DatabaseMetaData.class);
		ResultSet rows = mock(ResultSet.class);
		when(connection.getMetaData()).thenReturn(metadata);
		when(metadata.getIndexInfo(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
				org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(true),
				org.mockito.ArgumentMatchers.eq(false))).thenReturn(rows);
		if (queryFailure != null) { when(rows.next()).thenThrow(queryFailure); }
		if (closeFailure != null) { org.mockito.Mockito.doThrow(closeFailure).when(rows).close(); }
		return connection;
	}

	private void assertSanitized(Connection connection) {
		assertThatThrownBy(() -> dialect.resolveOnDuplicateKeyColumns(connection, qualifiedTarget()))
				.isInstanceOf(DataChangeException.class)
				.satisfies(failure -> {
					DataChangeException exception = (DataChangeException) failure;
					assertThat(exception.getCode()).isEqualTo(DataChangeErrorCode.ROW_IDENTITY_UNAVAILABLE);
					assertThat(exception.getMessage()).isEqualTo("ROW_IDENTITY_UNAVAILABLE");
					assertThat(exception.getCause()).isNull();
					assertThat(exception.getSuppressed()).isEmpty();
				});
	}

}
