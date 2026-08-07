package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
