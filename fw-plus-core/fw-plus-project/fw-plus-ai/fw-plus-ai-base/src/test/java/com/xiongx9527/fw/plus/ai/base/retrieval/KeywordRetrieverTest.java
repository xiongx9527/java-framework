package com.xiongx9527.fw.plus.ai.base.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.store.Document;
import com.xiongx9527.fw.plus.ai.base.store.ScoredDocument;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class KeywordRetrieverTest {

    private final KeywordRetriever retriever = new KeywordRetriever();

    @Test
    void ranksRelevantDocumentFirst() {
        List<Document> corpus = Arrays.asList(
                Document.of("1", "user name and email"),
                Document.of("2", "order amount and date"));

        List<ScoredDocument> hits = retriever.retrieve(
                RetrievalQuery.builder().text("user name").corpus(corpus).topK(10).build());

        assertEquals("1", hits.get(0).getDocument().getId());
        assertTrue(hits.get(0).getScore() > 0.0);
    }

    @Test
    void tokenizesChineseByCharacter() {
        List<Document> corpus = Arrays.asList(
                Document.of("1", "用户表结构"),
                Document.of("2", "订单金额"));

        List<ScoredDocument> hits = retriever.retrieve(
                RetrievalQuery.builder().text("用户").corpus(corpus).topK(10).build());

        assertEquals("1", hits.get(0).getDocument().getId());
    }

    @Test
    void emptyCorpusReturnsEmpty() {
        assertTrue(retriever.retrieve(RetrievalQuery.builder().text("x").build()).isEmpty());
        assertTrue(retriever.retrieve(RetrievalQuery.builder()
                .text("x").corpus(Collections.<Document>emptyList()).build()).isEmpty());
    }

    @Test
    void truncatesToTopK() {
        List<Document> corpus = Arrays.asList(
                Document.of("1", "alpha beta"),
                Document.of("2", "alpha gamma"),
                Document.of("3", "alpha delta"));

        List<ScoredDocument> hits = retriever.retrieve(
                RetrievalQuery.builder().text("alpha").corpus(corpus).topK(2).build());

        assertEquals(2, hits.size());
    }

    @Test
    void customScorerReplacesTfidf() {
        KeywordRetriever custom = new KeywordRetriever((query, doc) ->
                "1".equals(doc.getId()) ? 0.9 : 0.1);

        List<ScoredDocument> hits = custom.retrieve(RetrievalQuery.builder()
                .text("anything")
                .corpus(Arrays.asList(Document.of("1", "a"), Document.of("2", "b")))
                .topK(10)
                .build());

        assertEquals("1", hits.get(0).getDocument().getId());
        assertEquals(0.9, hits.get(0).getScore(), 0.0001);
    }
}
