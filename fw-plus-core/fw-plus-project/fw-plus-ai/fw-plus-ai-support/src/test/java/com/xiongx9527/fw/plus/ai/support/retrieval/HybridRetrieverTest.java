package com.xiongx9527.fw.plus.ai.support.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.store.Document;
import com.xiongx9527.fw.plus.ai.base.store.ScoredDocument;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class HybridRetrieverTest {

    private final Document a = Document.builder().id("a").content("A").build();
    private final Document b = Document.builder().id("b").content("B").build();
    private final Document c = Document.builder().id("c").content("C").build();

    @Test
    void weightedFusionCombinesScores() {
        Retriever r1 = q -> Arrays.asList(new ScoredDocument(a, 1.0), new ScoredDocument(b, 0.5));
        Retriever r2 = q -> Arrays.asList(new ScoredDocument(b, 1.0), new ScoredDocument(a, 0.2));
        HybridRetriever hybrid = new HybridRetriever(
                Arrays.asList(r1, r2), Arrays.asList(0.7, 0.3), FusionMode.WEIGHTED);

        List<ScoredDocument> out = hybrid.retrieve(RetrievalQuery.builder().text("x").topK(10).build());

        // a: 0.7*1.0 + 0.3*0.2 = 0.76 ; b: 0.7*0.5 + 0.3*1.0 = 0.65
        assertEquals("a", out.get(0).getDocument().getId());
        assertEquals("b", out.get(1).getDocument().getId());
    }

    @Test
    void rrfRanksConsistentTopFirst() {
        Retriever r1 = q -> Arrays.asList(new ScoredDocument(a, 0.9), new ScoredDocument(b, 0.8));
        Retriever r2 = q -> Arrays.asList(new ScoredDocument(a, 0.4), new ScoredDocument(c, 0.3));
        HybridRetriever hybrid = new HybridRetriever(
                Arrays.asList(r1, r2), Arrays.asList(1.0, 1.0), FusionMode.RRF);

        List<ScoredDocument> out = hybrid.retrieve(RetrievalQuery.builder().text("x").topK(10).build());

        assertEquals("a", out.get(0).getDocument().getId());
    }
}
