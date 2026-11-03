package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.model.RerankPort;
import com.xiongx9527.fw.plus.ai.base.retrieval.Reranker;
import com.xiongx9527.fw.plus.ai.base.store.ScoredDocument;
import java.util.ArrayList;
import java.util.List;

/** 基于 RerankPort 的重排器（product 注入重排模型）。 */
public class ModelReranker implements Reranker {

    private final RerankPort port;

    public ModelReranker(RerankPort port) {
        if (port == null) {
            throw new IllegalArgumentException("rerank port is required");
        }
        this.port = port;
    }

    @Override
    public List<ScoredDocument> rerank(String query, List<ScoredDocument> candidates) {
        List<ScoredDocument> out = new ArrayList<ScoredDocument>();
        if (candidates == null || candidates.isEmpty()) {
            return out;
        }
        List<String> docs = new ArrayList<String>();
        for (ScoredDocument sd : candidates) {
            docs.add(sd.getDocument() == null ? "" : sd.getDocument().getContent());
        }
        List<Double> scores = port.score(query, docs);
        for (int i = 0; i < candidates.size(); i++) {
            double s = scores != null && i < scores.size() && scores.get(i) != null
                    ? scores.get(i) : candidates.get(i).getScore();
            out.add(new ScoredDocument(candidates.get(i).getDocument(), s));
        }
        out.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return out;
    }
}
