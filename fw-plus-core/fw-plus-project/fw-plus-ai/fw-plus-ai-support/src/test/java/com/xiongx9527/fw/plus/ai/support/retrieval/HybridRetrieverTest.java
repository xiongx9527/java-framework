package com.xiongx9527.fw.plus.ai.support.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.store.Document;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class HybridRetrieverTest {

    private final Document a = Document.of("a", "A");
    private final Document b = Document.of("b", "B");
    private final Document c = Document.of("c", "C");

    @Test
    void weightedFusionCombinesScores() {
        Retriever<Document> r1 = q -> Arrays.asList(Retrieved.of(a, 1.0), Retrieved.of(b, 0.5));
        Retriever<Document> r2 = q -> Arrays.asList(Retrieved.of(b, 1.0), Retrieved.of(a, 0.2));
        HybridRetriever hybrid = new HybridRetriever(
                Arrays.asList(r1, r2), Arrays.asList(0.7, 0.3), FusionMode.WEIGHTED);

        List<Retrieved<Document>> out = hybrid.retrieve(RetrievalQuery.builder().text("x").topK(10).build());

        // a: 0.7*1.0 + 0.3*0.2 = 0.76 ; b: 0.7*0.5 + 0.3*1.0 = 0.65
        assertEquals("a", out.get(0).getItem().getId());
        assertEquals("b", out.get(1).getItem().getId());
    }

    @Test
    void rrfRanksConsistentTopFirst() {
        Retriever<Document> r1 = q -> Arrays.asList(Retrieved.of(a, 0.9), Retrieved.of(b, 0.8));
        Retriever<Document> r2 = q -> Arrays.asList(Retrieved.of(a, 0.4), Retrieved.of(c, 0.3));
        HybridRetriever hybrid = new HybridRetriever(
                Arrays.asList(r1, r2), Arrays.asList(1.0, 1.0), FusionMode.RRF);

        List<Retrieved<Document>> out = hybrid.retrieve(RetrievalQuery.builder().text("x").topK(10).build());

        assertEquals("a", out.get(0).getItem().getId());
    }
}
