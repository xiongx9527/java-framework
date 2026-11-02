package com.xiongx9527.fw.plus.ai.askdata.capability;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.llm.LlmChatPort;
import com.xiongx9527.fw.plus.ai.askdata.llm.StaticLlmChatPort;
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
    void llmValidationFailureLowersConfidence() {
        LlmChatPort port = new StaticLlmChatPort(p -> {
            if (p.contains("待校验")) {
                return "NO
该 SQL 关联了错误表";
            }
            return F + "sql
SELECT id FROM orders
" + F;
        });
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().enabled(false).build())
                .build();
        SqlGenResult r = cap.generate(input());
        assertTrue(r.getConfidence() < 0.5, "validation failed should lower confidence");
    }

    @Test
    void llmValidationPassKeepsHighConfidence() {
        LlmChatPort port = new StaticLlmChatPort(p -> {
            if (p.contains("待校验")) {
                return "YES";
            }
            return F + "sql
SELECT id FROM orders
" + F;
        });
        SqlGenerationCapability cap = SqlGenerationCapabilityBuilder.builder()
                .llm(port)
                .repairPolicy(SqlRepairPolicy.builder().enabled(false).build())
                .build();
        SqlGenResult r = cap.generate(input());
        assertTrue(r.getConfidence() >= 0.5);
    }
}
