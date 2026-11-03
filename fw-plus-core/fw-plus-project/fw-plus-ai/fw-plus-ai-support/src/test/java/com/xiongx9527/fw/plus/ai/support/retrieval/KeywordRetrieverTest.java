package com.xiongx9527.fw.plus.ai.support.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class KeywordRetrieverTest {

    @Test
    void ranksRelevantDocumentFirst() {
        List<Document> corpus = new ArrayList<>();
        corpus.add(Document.builder().id("users").content("users table user id name email").build());
        corpus.add(Document.builder().id("orders").content("orders table order id user amount").build());
        corpus.add(Document.builder().id("products").content("products table product id name price").build());

        RetrievalQuery query = RetrievalQuery.builder().text("user name").topK(10).corpus(corpus).build();
        List<ScoredDocument> hits = new KeywordRetriever().retrieve(query);

        assertFalse(hits.isEmpty());
        assertEquals("users", hits.get(0).getDocument().getId());
        assertTrue(hits.get(0).getScore() > 0.0);
        assertTrue(hits.get(0).getScore() <= 1.0);
    }

    @Test
    void chineseCharTokenization() {
        List<Document> corpus = new ArrayList<>();
        corpus.add(Document.builder().id("u").content("用户表").build());
        corpus.add(Document.builder().id("o").content("订单表").build());

        RetrievalQuery query = RetrievalQuery.builder().text("用户").corpus(corpus).build();
        List<ScoredDocument> hits = new KeywordRetriever().retrieve(query);

        assertEquals(1, hits.size());
        assertEquals("u", hits.get(0).getDocument().getId());
    }

    @Test
    void emptyCorpusReturnsEmpty() {
        RetrievalQuery query = RetrievalQuery.builder().text("x").corpus(new ArrayList<>()).build();
        assertTrue(new KeywordRetriever().retrieve(query).isEmpty());
    }

    @Test
    void topKTruncates() {
        List<Document> corpus = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            corpus.add(Document.builder().id("t" + i).content("user name email").build());
        }
        RetrievalQuery query = RetrievalQuery.builder().text("user").topK(2).corpus(corpus).build();
        assertEquals(2, new KeywordRetriever().retrieve(query).size());
    }
}
