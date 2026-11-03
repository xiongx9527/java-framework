package com.xiongx9527.fw.plus.ai.askdata.capability;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.llm.LlmChatPort;
import com.xiongx9527.fw.plus.ai.askdata.llm.StaticLlmChatPort;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.ValidationResult;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import java.util.Collections;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.junit.jupiter.api.Test;

public class LlmSqlValidationStrategyTest {

    private SqlGenInput input() {
        SchemaContext schema = SchemaContext.builder()
                .tables(Collections.singletonList(TableSchema.builder().name("orders").build()))
                .build();
        return SqlGenInput.builder().question("统计订单数").schemaContext(schema).build();
    }

    @Test
    void acceptsWhenLlmSaysYes() {
        LlmChatPort port = new StaticLlmChatPort("YES");
        LlmSqlValidationStrategy s = new LlmSqlValidationStrategy(port);
        ValidationResult vr = s.validate("SELECT id FROM orders", input());
        assertTrue(vr.isOk());
    }

    @Test
    void rejectsWhenLlmSaysNo() {
        LlmChatPort port = new StaticLlmChatPort("NO\n字段在可见 schema 中不存在");
        LlmSqlValidationStrategy s = new LlmSqlValidationStrategy(port);
        ValidationResult vr = s.validate("SELECT x FROM orders", input());
        assertFalse(vr.isOk());
        assertTrue(vr.getIssues().stream().anyMatch(i -> i.startsWith(I18nMessageResolver.resolveKey("askdata.llm.invalid", ""))));
    }

    @Test
    void fallsBackToLocalWhenLlmThrows() {
        LlmChatPort port = new StaticLlmChatPort(p -> {
            throw new RuntimeException("model unavailable");
        });
        LlmSqlValidationStrategy s = new LlmSqlValidationStrategy(port);
        ValidationResult vr = s.validate("SELECT id FROM missing_table", input());
        assertFalse(vr.isOk());
        assertTrue(vr.getIssues().stream().anyMatch(i -> i.equals(I18nMessageResolver.resolveKey("askdata.validate.table_not_in_schema", "missing_table"))));
    }
}
