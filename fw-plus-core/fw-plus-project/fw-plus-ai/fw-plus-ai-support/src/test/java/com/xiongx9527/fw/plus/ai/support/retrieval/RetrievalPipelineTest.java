package com.xiongx9527.fw.plus.ai.support.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.retrieval.Reranker;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalListener;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class RetrievalPipelineTest {

    private final Document a = Document.of("a", "A");
    private final Document b = Document.of("b", "B");
    private final Document c = Document.of("c", "C");

    @Test
    void appliesThresholdThenTopN() {
        Retriever<Document> retriever = q -> Arrays.asList(
                Retrieved.of(a, 0.9), Retrieved.of(b, 0.5), Retrieved.of(c, 0.2));

        List<Retrieved<Document>> hits = RetrievalPipeline.of(retriever)
                .retrieve(RetrievalQuery.builder().text("x").scoreThreshold(0.4).topN(5).build());

        assertEquals(2, hits.size());
        assertEquals("a", hits.get(0).getItem().getId());
    }

    @Test
    void truncatesToTopN() {
        Retriever<Document> retriever = q -> Arrays.asList(
                Retrieved.of(a, 0.9), Retrieved.of(b, 0.5), Retrieved.of(c, 0.2));

        assertEquals(1, RetrievalPipeline.of(retriever)
                .retrieve(RetrievalQuery.builder().text("x").topN(1).build()).size());
    }

    @Test
    void rerankerReordersCandidates() {
        Retriever<Document> retriever = q -> Arrays.asList(
                Retrieved.of(a, 0.9), Retrieved.of(b, 0.5));
        Reranker<Document> reranker = (query, candidates) -> {
            List<Retrieved<Document>> out = new ArrayList<Retrieved<Document>>();
            out.add(Retrieved.of(candidates.get(1).getItem(), 0.95));
            out.add(Retrieved.of(candidates.get(0).getItem(), 0.3));
            return out;
        };

        List<Retrieved<Document>> hits = RetrievalPipeline.of(retriever, reranker)
                .retrieve(RetrievalQuery.builder().text("x").topN(5).build());

        assertEquals("b", hits.get(0).getItem().getId());
    }

    @Test
    void listenerSeesQueryAndHits() {
        final int[] queries = {0};
        final int[] hitBatches = {0};
        Retriever<Document> retriever = RetrievalPipeline.of(
                q -> Arrays.asList(Retrieved.of(a, 0.8)),
                null,
                Collections.singletonList(new RetrievalListener() {
                    @Override
                    public void onQuery(RetrievalQuery query) {
                        queries[0]++;
                    }

                    @Override
                    public void onHits(List<? extends Retrieved<?>> hits) {
                        hitBatches[0]++;
                    }
                }));

        List<Retrieved<Document>> hits = retriever.retrieve(RetrievalQuery.builder().text("x").build());

        assertEquals(1, hits.size());
        assertEquals(1, queries[0]);
        assertEquals(1, hitBatches[0]);
    }

    @Test
    void emptyResultIsAllowed() {
        Retriever<Document> retriever = RetrievalPipeline.of(q -> new ArrayList<Retrieved<Document>>());
        assertTrue(retriever.retrieve(RetrievalQuery.builder().text("x").build()).isEmpty());
    }
}
