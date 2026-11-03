package com.xiongx9527.fw.plus.ai.askdata.capability;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.askdata.llm.StaticLlmPort;
import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlRepairPolicy;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class SqlValidationComboTest {

    private static final String F = String.valueOf('`') + String.valueOf('`') + String.valueOf('`');

    private SchemaContext schema() {
        return SchemaContext.builder()
                .tables(Collections.singletonList(TableSchema.builder().name("orders").build()))
                .build();
    }

    private SqlGenInput input() {
        return SqlGenInput.builder().question("统计订单数").schemaContext(schema()).build();
    }

    @Test
    void llmValidationFailureLowersConfidenceWhenOpen() {
        LlmPort port = new StaticLlmPort(p -> {
            if (p.contains("待校验")) {
                return "NO\n该 SQL 关联了错误表";
            }
            return F + "sql\nSELECT id FROM orders\n" + F;
        });
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().enabled(false).build())
                .failClosed(false)
                .build();
        SqlGenResult r = cap.generate(input());
        assertTrue(r.getConfidence() < 0.5, "validation failed should lower confidence");
    }

    @Test
    void llmValidationFailureClosesWhenRepairCannotFix() {
        LlmPort port = new StaticLlmPort(p -> {
            if (p.contains("待校验")) {
                return "NO\n该 SQL 关联了错误表";
            }
            return F + "sql\nSELECT id FROM orders\n" + F;
        });
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().enabled(false).build())
                .failClosed(true)
                .build();
        assertThrows(SqlGenerationException.class, () -> cap.generate(input()));
    }

    @Test
    void llmValidationPassKeepsHighConfidence() {
        LlmPort port = new StaticLlmPort(p -> {
            if (p.contains("待校验")) {
                return "YES";
            }
            return F + "sql\nSELECT id FROM orders\n" + F;
        });
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().enabled(false).build())
                .build();
        SqlGenResult r = cap.generate(input());
        assertTrue(r.getConfidence() >= 0.5);
    }
}
