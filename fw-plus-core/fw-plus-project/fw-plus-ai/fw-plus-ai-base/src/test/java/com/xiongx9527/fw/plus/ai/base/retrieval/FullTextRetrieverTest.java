package com.xiongx9527.fw.plus.ai.base.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class FullTextRetrieverTest {

    private final FullTextRetriever retriever = new FullTextRetriever();

    @Test
    void ranksRelevantDocumentFirst() {
        List<Document> corpus = Arrays.asList(
                Document.of("1", "user name and email"),
                Document.of("2", "order amount and date"));

        List<Retrieved<Document>> hits = retriever.retrieve(
                RetrievalQuery.builder().text("user name").corpus(corpus).topK(10).build());

        assertEquals("1", hits.get(0).getItem().getId());
        assertTrue(hits.get(0).getScore() > 0.0);
    }

    @Test
    void tokenizesChineseByCharacter() {
        List<Document> corpus = Arrays.asList(
                Document.of("1", "user table schema"),
                Document.of("2", "order amount"));

        List<Retrieved<Document>> hits = retriever.retrieve(
                RetrievalQuery.builder().text("user").corpus(corpus).topK(10).build());

        assertEquals("1", hits.get(0).getItem().getId());
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

        List<Retrieved<Document>> hits = retriever.retrieve(
                RetrievalQuery.builder().text("alpha").corpus(corpus).topK(2).build());

        assertEquals(2, hits.size());
    }
}
