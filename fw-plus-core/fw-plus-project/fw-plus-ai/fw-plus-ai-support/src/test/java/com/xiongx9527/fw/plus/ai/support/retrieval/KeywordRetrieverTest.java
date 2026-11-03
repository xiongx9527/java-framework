package com.xiongx9527.fw.plus.ai.support.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.store.Document;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class KeywordRetrieverTest {

    /** 表名 1.0 / 注释 0.9。 */
    private final KeywordRetriever retriever = new KeywordRetriever(
            new WeightedKeywordScorer()
                    .field("name", 1.0, Document::getId)
                    .field("comment", 0.9, doc -> String.valueOf(doc.getMetadata().get("comment"))));

    private Document table(String name, String comment) {
        return Document.builder().id(name).content(comment).metadata("comment", comment).build();
    }

    @Test
    void exactNameMatchRanksFirst() {
        List<Document> corpus = Arrays.asList(
                table("orders", "order record"),
                table("users", "user account"));

        List<Retrieved<Document>> hits = retriever.retrieve(RetrievalQuery.builder()
                .text("users").corpus(corpus).topK(10).build());

        assertEquals("users", hits.get(0).getItem().getId());
        assertEquals(1.0, hits.get(0).getScore(), 0.0001);
    }

    @Test
    void commentSubstringScoresLowerThanExactName() {
        List<Document> corpus = Arrays.asList(
                table("orders", "user order record"),
                table("users", "account"));

        List<Retrieved<Document>> hits = retriever.retrieve(RetrievalQuery.builder()
                .text("user").corpus(corpus).topK(10).build());

        assertEquals("users", hits.get(0).getItem().getId());
        assertTrue(hits.get(0).getScore() > hits.get(1).getScore());
    }

    @Test
    void noHitReturnsEmpty() {
        List<Document> corpus = Arrays.asList(table("orders", "order record"));

        assertTrue(retriever.retrieve(RetrievalQuery.builder()
                .text("zzz").corpus(corpus).topK(10).build()).isEmpty());
    }
}
