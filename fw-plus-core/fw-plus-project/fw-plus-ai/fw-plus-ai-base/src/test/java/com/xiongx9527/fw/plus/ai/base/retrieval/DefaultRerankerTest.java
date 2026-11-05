package com.xiongx9527.fw.plus.ai.base.retrieval;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.model.RerankPort;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultRerankerTest {

    private static RerankPort port(final double... scores) {
        return (query, documents) -> {
            List<Double> out = new ArrayList<Double>();
            for (double score : scores) {
                out.add(Double.valueOf(score));
            }
            return out;
        };
    }

    private static List<Retrieved<Document>> candidates() {
        return Arrays.asList(
                Retrieved.of(Document.of("a", "A"), 0.9),
                Retrieved.of(Document.of("b", "B"), 0.1));
    }

    @Test
    void fromProvidesAWorkingReranker() {
        Reranker<Document> reranker = Reranker.from(port(0.2, 0.8));

        List<Retrieved<Document>> out = reranker.rerank("q", candidates());

        assertEquals("b", out.get(0).getItem().getId());
        assertEquals(0.8, out.get(0).getScore(), 0.0001);
        assertEquals(0.2, out.get(1).getScore(), 0.0001);
    }

    @Test
    void missingScoresKeepOriginalScores() {
        RerankPort returnsNull = (query, documents) -> null;

        List<Retrieved<Document>> out = Reranker.from(returnsNull).rerank("q", candidates());

        assertEquals(2, out.size());
        assertEquals("a", out.get(0).getItem().getId());
        assertEquals(0.9, out.get(0).getScore(), 0.0001);
        assertEquals("b", out.get(1).getItem().getId());
        assertEquals(0.1, out.get(1).getScore(), 0.0001);
    }

    @Test
    void shortScoreListOnlyOverridesMatchedCandidates() {
        List<Retrieved<Document>> out = Reranker.from(port(0.3)).rerank("q", candidates());

        assertEquals("a", out.get(0).getItem().getId());
        assertEquals(0.3, out.get(0).getScore(), 0.0001);
        assertEquals("b", out.get(1).getItem().getId());
        assertEquals(0.1, out.get(1).getScore(), 0.0001);
    }

    @Test
    void upstreamFailurePropagates() {
        RerankPort failing = (query, documents) -> {
            throw new IllegalStateException("scoring down");
        };
        assertThrows(IllegalStateException.class,
                () -> Reranker.from(failing).rerank("q", candidates()));
    }

    @Test
    void emptyCandidatesYieldEmptyResult() {
        assertTrue(Reranker.from(port(0.5))
                .rerank("q", Collections.<Retrieved<Document>>emptyList()).isEmpty());
        assertTrue(Reranker.from(port(0.5)).rerank("q", null).isEmpty());
    }

    @Test
    void nullPortIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> Reranker.from(null));
    }
}
