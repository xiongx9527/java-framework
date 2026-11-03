package com.xiongx9527.fw.plus.ai.support.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class RetrievalEngineTest {

    private final Document a = Document.builder().id("a").content("A").build();
    private final Document b = Document.builder().id("b").content("B").build();
    private final Document c = Document.builder().id("c").content("C").build();

    @Test
    void appliesThresholdThenTopN() {
        Retriever r = q -> Arrays.asList(
                new ScoredDocument(a, 0.9), new ScoredDocument(b, 0.6), new ScoredDocument(c, 0.3));
        RetrievalEngine engine = new RetrievalEngine(r);

        RetrievalQuery query = RetrievalQuery.builder()
                .text("x").scoreThreshold(0.5).topN(2).build();
        List<ScoredDocument> out = engine.retrieve(query);

        assertEquals(2, out.size());
        assertEquals("a", out.get(0).getDocument().getId());
        assertEquals("b", out.get(1).getDocument().getId());
    }

    @Test
    void topNLimitsResult() {
        Retriever r = q -> Arrays.asList(
                new ScoredDocument(a, 0.9), new ScoredDocument(b, 0.6), new ScoredDocument(c, 0.3));
        RetrievalEngine engine = new RetrievalEngine(r);

        assertEquals(1, engine.retrieve(RetrievalQuery.builder().text("x").topN(1).build()).size());
    }

    @Test
    void rerankerReorders() {
        Retriever r = q -> Arrays.asList(
                new ScoredDocument(a, 0.9), new ScoredDocument(b, 0.6));
        RerankPort port = (query, docs) -> Arrays.asList(0.1, 0.95);
        RetrievalEngine engine = new RetrievalEngine(r, new ModelReranker(port));

        List<ScoredDocument> out = engine.retrieve(RetrievalQuery.builder().text("x").topN(2).build());

        assertEquals("b", out.get(0).getDocument().getId());
        assertEquals("a", out.get(1).getDocument().getId());
    }
}
