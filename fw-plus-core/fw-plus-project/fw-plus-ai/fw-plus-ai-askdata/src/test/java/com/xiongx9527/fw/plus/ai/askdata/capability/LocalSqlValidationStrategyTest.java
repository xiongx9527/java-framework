package com.xiongx9527.fw.plus.ai.askdata.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.postprocess.ValidationResult;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import java.util.Collections;
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
                .anyMatch(i -> i.contains("missing_table") && i.contains("visible schema"));
        assertTrue(found, "should flag missing_table: " + vr.getIssues());
    }

    @Test
    void rejectsWriteDml() {
        LocalSqlValidationStrategy s = new LocalSqlValidationStrategy();
        ValidationResult vr = s.validate("DELETE FROM orders", input());
        assertFalse(vr.isOk());
        assertTrue(vr.getIssues().stream().anyMatch(i -> i.contains("write DML")));
    }

    @Test
    void rejectsUnbalancedParentheses() {
        LocalSqlValidationStrategy s = new LocalSqlValidationStrategy();
        ValidationResult vr = s.validate("SELECT id FROM orders WHERE (id > 1", input());
        assertFalse(vr.isOk());
        assertTrue(vr.getIssues().stream().anyMatch(i -> i.contains("unbalanced parentheses")));
    }

    @Test
    void rejectsEmptySql() {
        LocalSqlValidationStrategy s = new LocalSqlValidationStrategy();
        ValidationResult vr = s.validate("   ", input());
        assertFalse(vr.isOk());
    }
}
