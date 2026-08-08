package com.xiongx9527.fw.plus.mybatis.datachange.dialect;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.MySQLIndexHint;
import net.sf.jsqlparser.expression.SQLServerHints;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.merge.Merge;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Pivot;
import net.sf.jsqlparser.statement.select.UnPivot;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

	@Test
	void analyzesSingleRowMergeSourceFromOracleDualInBindingOrder() {
		String sql = "MERGE INTO audit_order target USING ("
				+ "SELECT ? tenant_id, ? order_no, ? amount FROM dual) source "
				+ "ON (target.tenant_id=source.tenant_id AND target.order_no=source.order_no) "
				+ "WHEN MATCHED THEN UPDATE SET target.amount=source.amount "
				+ "WHEN NOT MATCHED THEN INSERT (tenant_id,order_no,amount) "
				+ "VALUES (source.tenant_id,source.order_no,source.amount)";
		Merge merge = (Merge) parse(sql);
		Table source = (Table) ((PlainSelect) merge.getUsingSelect().getSelectBody()).getFromItem();
		assertThat(source.getName()).isEqualToIgnoringCase("dual");
		assertThat(source.getSchemaName()).isNull();
		assertThat(source.getDatabase().getDatabaseName()).isNullOrEmpty();

		ChangeTarget target = dialect.analyze(context(sql,
				"tenantId", "orderNo", "amount")).get(0).getTargets().get(0);

		assertThat(target.getMatchParameterMappings())
				.extracting(mapping -> mapping.getProperty())
				.containsExactly("tenantId", "orderNo", "amount");
	}

	@Test
	void rejectsPivotAndUnpivotDecorationsOnDual() {
		assertThat(dialect.isSingleRowSource(new Table("dual").withPivot(new Pivot()))).isFalse();
		assertThat(dialect.isSingleRowSource(new Table("dual").withUnPivot(new UnPivot()))).isFalse();
	}

	@Test
	void rejectsDatabaseLinkAliasAndPlatformHintsOnDual() {
		assertThat(dialect.isSingleRowSource(new Table("dual@remote"))).isFalse();
		assertThat(dialect.isSingleRowSource(new Table("dual@dual"))).isFalse();
		assertThat(dialect.isSingleRowSource(new Table("dual").withAlias(new Alias("d")))).isFalse();
		assertThat(dialect.isSingleRowSource(new Table("dual")
				.withHint(new MySQLIndexHint("USE", "INDEX", java.util.Collections.singletonList("idx")))))
				.isFalse();
		assertThat(dialect.isSingleRowSource(new Table("dual")
				.withSqlServerHints(new SQLServerHints().withNoLock()))).isFalse();
	}

	private net.sf.jsqlparser.statement.Statement parse(String sql) {
		try {
			return CCJSqlParserUtil.parse(sql);
		} catch (net.sf.jsqlparser.JSQLParserException exception) {
			throw new AssertionError(exception);
		}
	}

	@Test
	void rejectsComplexMergeSourceEvenWhenItReadsFromDual() {
		assertThatThrownBy(() -> dialect.analyze(context("MERGE INTO audit_order target USING ("
				+ "SELECT ? tenant_id FROM dual WHERE ? = 1) source "
				+ "ON (target.tenant_id=source.tenant_id) "
				+ "WHEN MATCHED THEN UPDATE SET target.amount=1", "tenantId", "guard")))
				.isInstanceOf(SqlAnalysisException.class);
	}

	@Test
	void rejectsHierarchicalMergeSourceThatCanExpandDualToMultipleRows() {
		assertThatThrownBy(() -> dialect.analyze(context("MERGE INTO audit_order target USING ("
				+ "SELECT ? tenant_id FROM dual CONNECT BY LEVEL <= ?) source "
				+ "ON (target.tenant_id=source.tenant_id) "
				+ "WHEN MATCHED THEN UPDATE SET target.amount=1", "tenantId", "levels")))
				.isInstanceOf(SqlAnalysisException.class);
	}
}
