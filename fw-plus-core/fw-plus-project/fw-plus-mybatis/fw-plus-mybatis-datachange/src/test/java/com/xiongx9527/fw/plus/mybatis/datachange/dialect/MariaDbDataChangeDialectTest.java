package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MariaDbDataChangeDialectTest extends AbstractDataChangeDialectContract {
	private final MariaDbDataChangeDialect dialect = new MariaDbDataChangeDialect();

	@Test
	void implementsSharedSnapshotContract() {
		assertCompositeKeyAfterQuery(dialect);
		assertBeforeQuery(dialect, "`id`", "LIMIT 1001", "FOR UPDATE");
	}

	@Test
	void recognizesMariaDbAndAnalyzesOnDuplicateKey() {
		assertThat(dialect.supports(new DatabaseMetadata("MariaDB", 10, 11))).isTrue();
		assertThat(dialect.analyze(context("INSERT INTO audit_order(id,amount) VALUES(?,?) "
				+ "ON DUPLICATE KEY UPDATE amount=VALUES(amount)", "id", "amount")).get(0).getTargets())
					.singleElement().satisfies(target -> assertThat(target.isUpsert()).isTrue());
	}

	@Test
	void inheritsSchemaCapabilityMappingForQualifiedDatabaseName() throws Exception {
		Connection connection = mock(Connection.class);
		DatabaseMetaData metadata = mock(DatabaseMetaData.class);
		ResultSet rows = mock(ResultSet.class);
		when(connection.getMetaData()).thenReturn(metadata);
		when(metadata.supportsCatalogsInDataManipulation()).thenReturn(false);
		when(metadata.supportsSchemasInDataManipulation()).thenReturn(true);
		when(metadata.getIndexInfo(any(), any(), any(), eq(true), eq(false))).thenReturn(rows);
		when(rows.next()).thenReturn(true, false);
		when(rows.getString("INDEX_NAME")).thenReturn("uk_email");
		when(rows.getString("COLUMN_NAME")).thenReturn("email");
		when(rows.getShort("ORDINAL_POSITION")).thenReturn((short) 1);
		ChangeTarget target = dialect.analyze(context("INSERT INTO tenant_db.audit_order(id,email) VALUES(?,?) "
				+ "ON DUPLICATE KEY UPDATE email=VALUES(email)", "id", "email"))
				.get(0).getTargets().get(0);

		assertThat(dialect.resolveOnDuplicateKeyColumns(connection, target)).containsExactly("email");

		verify(metadata).getIndexInfo(null, "tenant_db", "audit_order", true, false);
	}

	@Test
	void inheritsConnectorSchemaTermFallbackWhenCapabilitiesAreAmbiguous() throws Exception {
		Connection connection = mock(Connection.class);
		DatabaseMetaData metadata = mock(DatabaseMetaData.class);
		ResultSet rows = mock(ResultSet.class);
		when(connection.getMetaData()).thenReturn(metadata);
		when(metadata.supportsCatalogsInDataManipulation()).thenReturn(false);
		when(metadata.supportsSchemasInDataManipulation()).thenReturn(false);
		when(metadata.getSchemaTerm()).thenReturn("SCHEMA");
		when(metadata.getCatalogTerm()).thenReturn("CATALOG");
		when(metadata.getIndexInfo(any(), any(), any(), eq(true), eq(false))).thenReturn(rows);
		when(rows.next()).thenReturn(true, false);
		when(rows.getString("INDEX_NAME")).thenReturn("uk_email");
		when(rows.getString("COLUMN_NAME")).thenReturn("email");
		when(rows.getShort("ORDINAL_POSITION")).thenReturn((short) 1);
		ChangeTarget target = dialect.analyze(context("INSERT INTO tenant_db.audit_order(id,email) VALUES(?,?) "
				+ "ON DUPLICATE KEY UPDATE email=VALUES(email)", "id", "email"))
				.get(0).getTargets().get(0);

		assertThat(dialect.resolveOnDuplicateKeyColumns(connection, target)).containsExactly("email");

		verify(metadata).getIndexInfo(null, "tenant_db", "audit_order", true, false);
	}
}
