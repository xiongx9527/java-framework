package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OracleDataChangeDialectTest extends AbstractDataChangeDialectContract {
	private final OracleDataChangeDialect dialect = new OracleDataChangeDialect();

	@Test
	void implementsSharedSnapshotContract() {
		assertCompositeKeyAfterQuery(dialect);
		assertBeforeQuery(dialect, "\"ID\"", "ROWNUM <= 1001", "FOR UPDATE");
	}

	@Test
	void recognizesOracleAndAnalyzesMerge() {
		assertThat(dialect.supports(new DatabaseMetadata("Oracle", 19, 0))).isTrue();
		assertThat(dialect.analyze(context("MERGE INTO audit_order target USING source_order source "
				+ "ON (target.id=source.id) WHEN MATCHED THEN UPDATE SET target.amount=source.amount"))
				.get(0).getTargets()).singleElement()
				.satisfies(target -> assertThat(target.getOperation()).isEqualTo(StatementOperation.MERGE));
	}

	@Test
	void appliesRowLimitToTheWholeOrPredicate() {
		ChangeTarget target = dialect.analyze(context("UPDATE audit_order SET amount=? WHERE id=? OR id=?",
				"amount", "firstId", "secondId")).get(0).getTargets().get(0)
				.withSnapshotConfiguration(java.util.Arrays.asList("id"), java.util.Arrays.asList("amount"),
						1000, false);

		assertThat(dialect.buildBeforeQuery(target).getSql())
				.contains("WHERE (id = ? OR id = ?) AND ROWNUM <= 1001");
	}
}
