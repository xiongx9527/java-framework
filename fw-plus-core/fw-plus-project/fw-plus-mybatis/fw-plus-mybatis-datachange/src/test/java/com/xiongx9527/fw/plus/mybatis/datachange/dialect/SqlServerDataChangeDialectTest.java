package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SqlServerDataChangeDialectTest extends AbstractDataChangeDialectContract {
	private final SqlServerDataChangeDialect dialect = new SqlServerDataChangeDialect();

	@Test
	void implementsSharedSnapshotContract() {
		assertCompositeKeyAfterQuery(dialect);
		assertBeforeQuery(dialect, "[id]", "TOP (1001)", "WITH (UPDLOCK, HOLDLOCK)");
	}

	@Test
	void recognizesSqlServerAndAnalyzesMerge() {
		assertThat(dialect.supports(new DatabaseMetadata("Microsoft SQL Server", 16, 0))).isTrue();
		assertThat(dialect.analyze(context("MERGE INTO audit_order target USING source_order source "
				+ "ON (target.id=source.id) WHEN MATCHED THEN UPDATE SET target.amount=source.amount"))
				.get(0).getTargets()).singleElement()
				.satisfies(target -> assertThat(target.getOperation()).isEqualTo(StatementOperation.MERGE));
	}
}
