package com.xiongx9527.fw.plus.ai.base.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.store.Document;
import com.xiongx9527.fw.plus.ai.base.store.ScoredDocument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class RetrievalEngineTest {

    private final Document a = Document.of("a", "A");
    private final Document b = Document.of("b", "B");
    private final Document c = Document.of("c", "C");

    @Test
    void appliesThresholdThenTopN() {
        Retriever retriever = q -> Arrays.asList(
                new ScoredDocument(a, 0.9), new ScoredDocument(b, 0.5), new ScoredDocument(c, 0.2));
        RetrievalEngine engine = new RetrievalEngine(retriever);

        List<ScoredDocument> hits = engine.retrieve(RetrievalQuery.builder()
                .text("x").scoreThreshold(0.4).topN(5).build());

        assertEquals(2, hits.size());
        assertEquals("a", hits.get(0).getDocument().getId());
    }

    @Test
    void truncatesToTopN() {
        Retriever retriever = q -> Arrays.asList(
                new ScoredDocument(a, 0.9), new ScoredDocument(b, 0.5), new ScoredDocument(c, 0.2));
        RetrievalEngine engine = new RetrievalEngine(retriever);

        assertEquals(1, engine.retrieve(RetrievalQuery.builder().text("x").topN(1).build()).size());
    }

    @Test
    void rerankerReordersCandidates() {
        Retriever retriever = q -> Arrays.asList(
                new ScoredDocument(a, 0.9), new ScoredDocument(b, 0.5));
        Reranker reranker = (query, candidates) -> {
            List<ScoredDocument> out = new ArrayList<ScoredDocument>();
            out.add(new ScoredDocument(candidates.get(1).getDocument(), 0.95));
            out.add(new ScoredDocument(candidates.get(0).getDocument(), 0.3));
            return out;
        };

        List<ScoredDocument> hits = new RetrievalEngine(retriever, reranker)
                .retrieve(RetrievalQuery.builder().text("x").topN(5).build());

        assertEquals("b", hits.get(0).getDocument().getId());
    }

    @Test
    void listenerSeesQueryAndHits() {
        final int[] queries = {0};
        final int[] hitBatches = {0};
        RetrievalEngine engine = new RetrievalEngine(
                q -> Arrays.asList(new ScoredDocument(a, 0.8)),
                null,
                java.util.Collections.singletonList(new RetrievalListener() {
                    @Override
                    public void onQuery(RetrievalQuery query) {
                        queries[0]++;
                    }

                    @Override
                    public void onHits(List<ScoredDocument> hits) {
                        hitBatches[0]++;
                    }
                }));

        List<ScoredDocument> hits = engine.retrieve(RetrievalQuery.builder().text("x").build());

        assertEquals(1, hits.size());
        assertEquals(1, queries[0]);
        assertEquals(1, hitBatches[0]);
    }

    @Test
    void emptyResultIsAllowed() {
        RetrievalEngine engine = new RetrievalEngine(q -> new ArrayList<ScoredDocument>());
        assertTrue(engine.retrieve(RetrievalQuery.builder().text("x").build()).isEmpty());
    }
}
