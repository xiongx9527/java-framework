package com.xiongx9527.fw.plus.ai.askdata.recall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.schema.ColumnSchema;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import com.xiongx9527.fw.plus.ai.base.retrieval.FullTextRetriever;
import com.xiongx9527.fw.plus.ai.support.retrieval.RetrievalPipeline;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class EngineSchemaRecallCapabilityTest {

    private TableSchema table(String name, String comment, String... cols) {
        List<ColumnSchema> columns = new ArrayList<>();
        for (String c : cols) {
            columns.add(ColumnSchema.builder().name(c).type("varchar").build());
        }
        return TableSchema.builder().name(name).comment(comment).columns(columns).build();
    }

    @Test
    void recallsRelevantTable() {
        List<TableSchema> candidates = new ArrayList<>();
        candidates.add(table("users", "user account", "id", "name", "email"));
        candidates.add(table("orders", "order record", "id", "user", "amount"));
        candidates.add(table("products", "product", "id", "name", "price"));

        EngineSchemaRecallCapability cap = new EngineSchemaRecallCapability(
                RetrievalPipeline.of(new FullTextRetriever()), 10, 5, 0.0);
        SchemaContext ctx = cap.recall(SchemaRecallInput.builder()
                .question("user name").candidates(candidates).build());

        assertFalse(ctx.isEmpty());
        assertEquals("users", ctx.getTables().get(0).getName());
    }

    @Test
    void noMatchReturnsEmpty() {
        List<TableSchema> candidates = new ArrayList<>();
        candidates.add(table("users", "user account", "id", "name"));
        candidates.add(table("orders", "order record", "id", "amount"));

        EngineSchemaRecallCapability cap = new EngineSchemaRecallCapability(
                RetrievalPipeline.of(new FullTextRetriever()), 10, 5, 0.0);
        SchemaContext ctx = cap.recall(SchemaRecallInput.builder()
                .question("zzz").candidates(candidates).build());

        assertTrue(ctx.isEmpty());
    }
}
