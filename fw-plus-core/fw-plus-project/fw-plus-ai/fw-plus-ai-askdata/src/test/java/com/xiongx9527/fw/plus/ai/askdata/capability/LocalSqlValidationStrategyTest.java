package com.xiongx9527.fw.plus.ai.askdata.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.dialect.SqlDialect;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ValidationResult;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import java.util.Collections;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.junit.jupiter.api.Test;

public class LocalSqlValidationStrategyTest {

    private SchemaContext schema() {
        return SchemaContext.builder()
                .tables(Collections.singletonList(TableSchema.builder().name("orders").build()))
                .build();
    }

    private SqlGenInput input() {
        return SqlGenInput.builder().question("统计订单数").schemaContext(schema()).build();
    }

    @Test
    void acceptsValidSelectOnVisibleTable() {
        LocalSqlValidationStrategy s = new LocalSqlValidationStrategy();
        ValidationResult vr = s.validate("SELECT id FROM orders", input());
        assertTrue(vr.isOk());
        assertEquals(Collections.singletonList("orders"), vr.getReferencedTables());
    }

    @Test
    void rejectsTableNotInVisibleSchema() {
        LocalSqlValidationStrategy s = new LocalSqlValidationStrategy();
        ValidationResult vr = s.validate("SELECT id FROM missing_table", input());
        assertFalse(vr.isOk());
        boolean found = vr.getIssues().stream()
                .anyMatch(i -> i.equals(I18nMessageResolver.resolveKey("askdata.validate.table_not_in_schema", "missing_table")));
        assertTrue(found, "should flag missing_table: " + vr.getIssues());
    }

    @Test
    void rejectsWriteDml() {
        LocalSqlValidationStrategy s = new LocalSqlValidationStrategy();
        ValidationResult vr = s.validate("DELETE FROM orders", input());
        assertFalse(vr.isOk());
        assertTrue(vr.getIssues().stream().anyMatch(i -> i.equals(I18nMessageResolver.resolveKey("askdata.validate.write_dml", "DELETE"))));
    }

    @Test
    void rejectsUnbalancedParentheses() {
        LocalSqlValidationStrategy s = new LocalSqlValidationStrategy();
        ValidationResult vr = s.validate("SELECT id FROM orders WHERE (id > 1", input());
        assertFalse(vr.isOk());
        assertTrue(vr.getIssues().stream().anyMatch(i -> i.equals(I18nMessageResolver.resolveKey("askdata.validate.unbalanced_paren", 1, 0))));
    }

    @Test
    void rejectsEmptySql() {
        LocalSqlValidationStrategy s = new LocalSqlValidationStrategy();
        ValidationResult vr = s.validate("   ", input());
        assertFalse(vr.isOk());
    }

    @Test
    void flagsBacktickInPostgresDialect() {
        LocalSqlValidationStrategy s = new LocalSqlValidationStrategy();
        SqlGenInput in = SqlGenInput.builder().question("q").schemaContext(schema())
                .dialectHint("pg").build();
        ValidationResult vr = s.validate("SELECT `id` FROM orders", in);
        assertFalse(vr.isOk());
        assertTrue(vr.getIssues().stream().anyMatch(i -> i.equals(I18nMessageResolver.resolveKey("askdata.dialect.backtick", SqlDialect.POSTGRESQL))),
                "should flag backtick in pg: " + vr.getIssues());
    }

    @Test
    void ignoresBacktickInMysqlDialect() {
        LocalSqlValidationStrategy s = new LocalSqlValidationStrategy();
        SqlGenInput in = SqlGenInput.builder().question("q").schemaContext(schema())
                .dialectHint("mysql").build();
        ValidationResult vr = s.validate("SELECT `id` FROM orders", in);
        assertTrue(vr.isOk());
    }

    @Test
    void flagsLimitInOracleDialect() {
        LocalSqlValidationStrategy s = new LocalSqlValidationStrategy();
        SqlGenInput in = SqlGenInput.builder().question("q").schemaContext(schema())
                .dialectHint("oracle").build();
        ValidationResult vr = s.validate("SELECT id FROM orders LIMIT 10", in);
        assertFalse(vr.isOk());
        assertTrue(vr.getIssues().stream().anyMatch(i -> i.equals(I18nMessageResolver.resolveKey("askdata.dialect.limit", SqlDialect.ORACLE))),
                "should flag LIMIT in oracle: " + vr.getIssues());
    }

    @Test
    void skipsDialectCheckWhenNoHint() {
        LocalSqlValidationStrategy s = new LocalSqlValidationStrategy();
        ValidationResult vr = s.validate("SELECT id FROM orders LIMIT 10", input());
        assertTrue(vr.isOk());
    }
}
