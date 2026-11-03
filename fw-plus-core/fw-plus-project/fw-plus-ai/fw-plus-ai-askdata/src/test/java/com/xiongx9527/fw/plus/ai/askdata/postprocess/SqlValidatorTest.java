package com.xiongx9527.fw.plus.ai.askdata.postprocess;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import java.util.Collections;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SqlValidatorTest {

    private SchemaContext ctx;

    @BeforeEach
    void setUp() {
        ctx = SchemaContext.builder()
                .tables(Collections.singletonList(TableSchema.builder().name("orders").build()))
                .build();
    }

    @Test
    void acceptsKnownTable() {
        ValidationResult vr = new SqlValidator().validate("SELECT id FROM orders", ctx);
        assertTrue(vr.isOk());
    }

    @Test
    void rejectsWriteDml() {
        ValidationResult vr = new SqlValidator().validate("INSERT INTO orders VALUES (1)", ctx);
        assertFalse(vr.isOk());
        assertTrue(vr.getIssues().stream().anyMatch(i -> i.equals(I18nMessageResolver.resolveKey("askdata.validate.write_dml", "INSERT"))));
    }

    @Test
    void flagsUnknownTable() {
        ValidationResult vr = new SqlValidator().validate("SELECT id FROM missing_table", ctx);
        assertFalse(vr.isOk());
        assertTrue(vr.getIssues().stream().anyMatch(i -> i.equals(I18nMessageResolver.resolveKey("askdata.validate.table_not_in_schema", "missing_table"))));
    }

    @Test
    void detectsUnbalancedParentheses() {
        ValidationResult vr = new SqlValidator().validate("SELECT (id FROM orders", ctx);
        assertFalse(vr.isOk());
        assertTrue(vr.getIssues().stream().anyMatch(i -> i.equals(I18nMessageResolver.resolveKey("askdata.validate.unbalanced_paren", 1, 0))));
    }
}
