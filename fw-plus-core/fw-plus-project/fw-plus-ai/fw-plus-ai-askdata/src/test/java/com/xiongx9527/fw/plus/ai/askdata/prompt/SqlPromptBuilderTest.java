package com.xiongx9527.fw.plus.ai.askdata.prompt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenInput;
import com.xiongx9527.fw.plus.ai.askdata.schema.ColumnSchema;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;

public class SqlPromptBuilderTest {

    private SchemaContext schema() {
        TableSchema t = TableSchema.builder()
                .name("orders")
                .columns(Arrays.asList(
                        ColumnSchema.builder().name("id").type("BIGINT").primaryKey(true).build(),
                        ColumnSchema.builder().name("amount").type("DOUBLE").build()))
                .build();
        return SchemaContext.builder().tables(Collections.singletonList(t)).build();
    }

    @Test
    void includesRagBlocksWhenProvided() {
        SqlPromptBuilder builder = new SqlPromptBuilder();
        String term = "GMV 表示成交总额";
        String example = "SELECT region, sum(amount) FROM orders GROUP BY region";
        String custom = "优先使用 GROUP BY 聚合";
        SqlGenInput input = SqlGenInput.builder()
                .question("各区域金额")
                .schemaContext(schema())
                .terms(Arrays.asList(term))
                .sqlExamples(Arrays.asList(example))
                .customPrompt(custom)
                .build();
        String user = builder.build(input).getUserPrompt();
        assertTrue(user.contains(term), "should contain business terminology block");
        assertTrue(user.contains(example), "should contain sql examples block");
        assertTrue(user.contains(custom), "should contain custom prompt block");
    }

    @Test
    void omitsRagBlocksWhenAbsent() {
        SqlPromptBuilder builder = new SqlPromptBuilder();
        SqlGenInput input = SqlGenInput.builder().question("q").schemaContext(schema()).build();
        String user = builder.build(input).getUserPrompt();
        assertTrue(!user.contains("GMV 表示成交总额"));
        assertTrue(!user.contains("SELECT region, sum(amount) FROM orders GROUP BY region"));
    }
}
