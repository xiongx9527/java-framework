package com.xiongx9527.fw.plus.ai.askdata.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.askdata.llm.StaticLlmPort;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlRepairPolicy;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
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
        LlmPort port = new StaticLlmPort(F + "sql\nSELECT id FROM orders\n" + F);
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
        LlmPort port = new StaticLlmPort(F + "sql\nSELECT 1\n" + F);
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
        LlmPort port = new StaticLlmPort(p -> {
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
        LlmPort port = new StaticLlmPort("SELECT id FROM missing_table");
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().failClosed(true).enabled(false).build())
                .build();
        assertThrows(SqlGenerationException.class, () -> cap.generate(input("统计订单数")));
    }

    @Test
    void normalizeInputFoldsWhitespaceBeforePrompt() {
        AtomicReference<String> captured = new AtomicReference<>();
        LlmPort port = new StaticLlmPort(p -> {
            captured.set(p);
            return F + "sql\nSELECT id FROM orders\n" + F;
        });
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .normalizeInput(true)
                .repairPolicy(SqlRepairPolicy.builder().enabled(false).build())
                .build();
        cap.generate(input("统计\n  订单   数"));
        assertTrue(captured.get().contains("统计 订单 数"),
                "prompt should contain folded question: " + captured.get());
    }

    @Test
    void normalizeInputLengthCapThrows() {
        LlmPort port = new StaticLlmPort("SELECT 1");
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .normalizeInput(true)
                .repairPolicy(SqlRepairPolicy.builder().enabled(false).build())
                .build();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 2500; i++) {
            sb.append("a");
        }
        assertThrows(IllegalArgumentException.class, () -> cap.generate(input(sb.toString())));
    }
}
