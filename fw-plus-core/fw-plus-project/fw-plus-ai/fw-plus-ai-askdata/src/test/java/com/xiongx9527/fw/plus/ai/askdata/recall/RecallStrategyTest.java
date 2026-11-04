package com.xiongx9527.fw.plus.ai.askdata.recall;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.askdata.schema.ColumnSchema;
import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import com.xiongx9527.fw.plus.ai.support.retrieval.IndexMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Verifies EngineSchemaRecallCapability selects the retrieval chain per request IndexMode:
 * ECONOMICAL uses the lexical retriever only (no rerank), HIGH_QUALITY uses vector / semantic when
 * only a vector retriever is provided, and a null indexMode defaults to HIGH_QUALITY.
 */
public class RecallStrategyTest {

    static class SpyRetriever implements Retriever<Document> {
        private final String id;
        boolean used = false;

        SpyRetriever(String id) {
            this.id = id;
        }

        @Override
        public List<Retrieved<Document>> retrieve(RetrievalQuery query) {
            used = true;
            return Collections.singletonList(Retrieved.of(Document.builder().id(id).content("x").build(), 1.0));
        }
    }

    private TableSchema table(String name) {
        List<ColumnSchema> cols = new ArrayList<ColumnSchema>();
        cols.add(ColumnSchema.builder().name("id").type("varchar").build());
        return TableSchema.builder().name(name).comment("c").columns(cols).build();
    }

    private List<TableSchema> candidates() {
        List<TableSchema> c = new ArrayList<TableSchema>();
        c.add(table("users"));
        return c;
    }

    @Test
    void economicalUsesLexicalOnly() {
        SpyRetriever vector = new SpyRetriever("users");
        SpyRetriever lexical = new SpyRetriever("users");
        EngineSchemaRecallCapability cap = new EngineSchemaRecallCapability(vector, lexical, null, 10, 8, 0.0);
        SchemaContext ctx = cap.recall(SchemaRecallInput.builder()
                .question("q").candidates(candidates()).indexMode(IndexMode.ECONOMICAL).build());
        assertTrue(lexical.used, "lexical retriever should be used for ECONOMICAL");
        assertFalse(vector.used, "vector retriever should NOT be used for ECONOMICAL");
        assertFalse(ctx.isEmpty());
        assertEquals("users", ctx.getTables().get(0).getName());
    }

    @Test
    void highQualityUsesVectorWhenOnlyVectorProvided() {
        SpyRetriever vector = new SpyRetriever("users");
        EngineSchemaRecallCapability cap = new EngineSchemaRecallCapability(vector, null, null, 10, 8, 0.0);
        SchemaContext ctx = cap.recall(SchemaRecallInput.builder()
                .question("q").candidates(candidates()).indexMode(IndexMode.HIGH_QUALITY).build());
        assertTrue(vector.used, "vector retriever should be used for HIGH_QUALITY");
        assertFalse(ctx.isEmpty());
    }

    @Test
    void nullIndexModeDefaultsToHighQuality() {
        SpyRetriever vector = new SpyRetriever("users");
        EngineSchemaRecallCapability cap = new EngineSchemaRecallCapability(vector, null, null, 10, 8, 0.0);
        SchemaContext ctx = cap.recall(SchemaRecallInput.builder()
                .question("q").candidates(candidates()).build());
        assertTrue(vector.used);
        assertFalse(ctx.isEmpty());
    }
}
