package com.xiongx9527.fw.plus.ai.askdata.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenResult;
import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenerationException;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.askdata.llm.StaticLlmPort;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlRepairPolicy;
import com.xiongx9527.fw.plus.ai.askdata.prompt.SqlPrompt;
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

    // ---- refine(String, SqlPrompt, SqlGenInput) ----

    @Test
    void refineExtractsValidSqlFromRawOutput() {
        LlmPort port = new StaticLlmPort(F + "sql\nSELECT id FROM t\n" + F);
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().enabled(false).build())
                .build();
        SqlPrompt prompt = new SqlPrompt("system", "user");
        SqlGenInput meta = SqlGenInput.builder().question("统计").build();
        SqlGenResult r = cap.refine(F + "sql\nSELECT id FROM t\n" + F, prompt, meta);
        assertEquals("SELECT id FROM t", r.getSql());
        assertTrue(r.isValid());
    }

    @Test
    void refineExtractsProductJsonProtocol() {
        // 产品 SQL JSON 协议（success/sql/brief/chart-type/tables）应被框架透明抽取
        String json = "{\"success\":true,\"sql\":\"SELECT id FROM t\",\"brief\":\"统计订单\","
                + "\"chart-type\":\"bar\",\"tables\":[\"t\"]}";
        LlmPort port = new StaticLlmPort(json);
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().enabled(false).build())
                .build();
        SqlPrompt prompt = new SqlPrompt("system", "user");
        SqlGenInput meta = SqlGenInput.builder().question("统计").build();
        SqlGenResult r = cap.refine(json, prompt, meta);
        assertEquals("SELECT id FROM t", r.getSql());
        assertTrue(r.isValid());
        assertTrue(r.isSuccess());
        assertEquals("统计订单", r.getBrief());
        assertEquals("t", r.getTables().get(0));
        assertEquals(com.xiongx9527.fw.plus.ai.askdata.chart.ChartType.BAR, r.getChartType());
    }

    @Test
    void refineRepairsInvalidSqlViaRepairLoop() {
        // 初始原始输出是写型 DML（非法，取自入参 rawOutput，不调 LLM）；
        // 修复轮回喂后 LLM 返回合法 SELECT，修复成功。
        LlmPort port = new StaticLlmPort(F + "sql\nSELECT id FROM t\n" + F);
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().enabled(true).maxAttempts(1).build())
                .build();
        SqlPrompt prompt = new SqlPrompt("system", "user");
        SqlGenInput meta = SqlGenInput.builder().question("统计").build();
        SqlGenResult r = cap.refine(F + "sql\nINSERT INTO t VALUES (1)\n" + F, prompt, meta);
        assertEquals("SELECT id FROM t", r.getSql());
        assertTrue(r.isValid());
    }

    @Test
    void refineFailClosedThrowsWhenRepairCannotFix() {
        LlmPort port = new StaticLlmPort(F + "sql\nINSERT INTO t VALUES (1)\n" + F);
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().failClosed(true).enabled(true).maxAttempts(1).build())
                .build();
        SqlPrompt prompt = new SqlPrompt("system", "user");
        SqlGenInput meta = SqlGenInput.builder().question("统计").build();
        assertThrows(SqlGenerationException.class,
                () -> cap.refine(F + "sql\nINSERT INTO t VALUES (1)\n" + F, prompt, meta));
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
