package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(dialect.resolveIdentityColumns(null, target, java.util.Arrays.asList("fallback_id")))
				.containsExactly("id");
	}
}
