package com.xiongx9527.fw.plus.ai.askdata.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.llm.LlmChatPort;
import com.xiongx9527.fw.plus.ai.askdata.llm.StaticLlmChatPort;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlRepairPolicy;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class DefaultSqlGenerationCapabilityTest {

    private static final String F = String.valueOf('\u0060') + String.valueOf('\u0060') + String.valueOf('\u0060');

    private SchemaContext schema() {
        return SchemaContext.builder()
                .tables(Collections.singletonList(TableSchema.builder().name("orders").build()))
                .build();
    }

    private SqlGenInput input(String q) {
        return SqlGenInput.builder().question(q).schemaContext(schema()).build();
    }

    @Test
    void happyPathReturnsSqlAndConfidence() {
        LlmChatPort port = new StaticLlmChatPort(F + "sql\nSELECT id FROM orders\n" + F);
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().enabled(false).build())
                .build();
        SqlGenResult r = cap.generate(input("统计订单数"));
        assertEquals("SELECT id FROM orders", r.getSql());
        assertTrue(r.getConfidence() > 0);
        assertTrue(r.isValid());
    }

    @Test
    void missingSchemaThrows() {
        LlmChatPort port = new StaticLlmChatPort(F + "sql\nSELECT 1\n" + F);
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().enabled(false).build())
                .build();
        assertThrows(MissingInputException.class,
                () -> cap.generate(SqlGenInput.builder().question("q").build()));
    }

    @Test
    void repairsInvalidSql() {
        AtomicInteger n = new AtomicInteger();
        LlmChatPort port = new StaticLlmChatPort(p -> {
            if (n.getAndIncrement() == 0) {
                return F + "sql\nSELECT x FROM missing_table\n" + F;
            }
            return F + "sql\nSELECT id FROM orders\n" + F;
        });
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().enabled(true).maxAttempts(1).build())
                .build();
        SqlGenResult r = cap.generate(input("统计订单数"));
        assertEquals("SELECT id FROM orders", r.getSql());
    }

    @Test
    void failClosedThrowsWhenValidationFails() {
        LlmChatPort port = new StaticLlmChatPort("SELECT id FROM missing_table");
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().failClosed(true).enabled(false).build())
                .build();
        assertThrows(SqlGenerationException.class, () -> cap.generate(input("统计订单数")));
    }
}
