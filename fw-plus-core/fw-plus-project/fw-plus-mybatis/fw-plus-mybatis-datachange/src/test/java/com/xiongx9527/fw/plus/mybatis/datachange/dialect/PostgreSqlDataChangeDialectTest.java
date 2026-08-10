package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeErrorCode;
import com.xiongx9527.fw.plus.mybatis.datachange.api.DataChangeException;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Table;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PostgreSqlDataChangeDialectTest extends AbstractDataChangeDialectContract {
	private final PostgreSqlDataChangeDialect dialect = new PostgreSqlDataChangeDialect();

	@Test
	void implementsSharedSnapshotContract() {
		assertCompositeKeyAfterQuery(dialect);
		assertBeforeQuery(dialect, "\"id\"", "LIMIT 1001", "FOR UPDATE");
	}

	@Test
	void recognizesPostgresAndAnalyzesOnConflictReturning() {
		assertThat(dialect.supports(new DatabaseMetadata("PostgreSQL", 16, 2))).isTrue();
		ChangeTarget target = dialect.analyze(context("INSERT INTO audit_order(id,amount) VALUES(?,?) "
				+ "ON CONFLICT (id) DO UPDATE SET amount=EXCLUDED.amount RETURNING id", "id", "amount"))
				.get(0).getTargets().get(0);
		assertThat(target.isUpsert()).isTrue();
		assertThat(target.getConflictColumnsHint()).containsExactly("id");
		assertThat(target.getConflictTarget().getType()).isEqualTo(ConflictTarget.Type.COLUMNS);
		assertThat(dialect.resolveIdentityColumns(null, target, java.util.Arrays.asList("fallback_id")))
				.containsExactly("id");
	}

	@Test
	void resolvesOnConstraintColumnsFromOrderedUniqueIndexMetadata() throws Exception {
		ChangeTarget target = dialect.analyze(context("INSERT INTO tenant.audit_order(id,email,amount) VALUES(?,?,?) "
				+ "ON CONFLICT ON CONSTRAINT uk_tenant_email DO UPDATE SET amount=EXCLUDED.amount",
				"id", "email", "amount")).get(0).getTargets().get(0);
		Connection connection = mock(Connection.class);
		DatabaseMetaData metadata = mock(DatabaseMetaData.class);
		ResultSet rows = mock(ResultSet.class);
		when(connection.getMetaData()).thenReturn(metadata);
		when(metadata.getIndexInfo(null, "tenant", "audit_order", true, false)).thenReturn(rows);
		when(rows.next()).thenReturn(true, true, false);
		when(rows.getString("INDEX_NAME")).thenReturn("uk_tenant_email");
		when(rows.getString("COLUMN_NAME")).thenReturn("tenant_id", "email");
		when(rows.getShort("ORDINAL_POSITION")).thenReturn((short) 1, (short) 2);

		assertThat(target.getConflictTarget().getType()).isEqualTo(ConflictTarget.Type.CONSTRAINT);
		assertThat(target.getConflictTarget().getConstraintName()).isEqualTo("uk_tenant_email");
		assertThat(dialect.resolveIdentityColumns(connection, target, Collections.singletonList("id")))
				.containsExactly("tenant_id", "email");
	}

	@Test
	void rejectsExpressionConflictTargetWithoutFallingBackToPrimaryKey() {
		ConflictTarget conflict = ConflictTarget.expression(new Function());
		ChangeTarget target = new ChangeTarget(StatementOperation.INSERT,
				new com.xiongx9527.fw.plus.mybatis.datachange.api.TableIdentifier(null, null, "audit_order"),
				new Table("audit_order"), null, Collections.emptyList(), Collections.emptyList(),
				Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), null,
				new Table("audit_order"), true, conflict);

		assertThatThrownBy(() -> dialect.resolveIdentityColumns(null, target, Arrays.asList("id")))
				.isInstanceOf(DataChangeException.class)
				.extracting(failure -> ((DataChangeException) failure).getCode())
				.isEqualTo(DataChangeErrorCode.ROW_IDENTITY_UNAVAILABLE);
	}
}
