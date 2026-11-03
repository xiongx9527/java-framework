package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.Reranker;
import com.xiongx9527.fw.plus.ai.base.model.RerankPort;
import com.xiongx9527.fw.plus.ai.base.store.Document;
import java.util.ArrayList;
import java.util.List;

/** 基于 RerankPort 的重排器（product 注入重排模型）。 */
public class ModelReranker implements Reranker<Document> {

    private final RerankPort port;

    public ModelReranker(RerankPort port) {
        if (port == null) {
            throw new IllegalArgumentException("rerank port is required");
        }
        this.port = port;
    }

    @Override
    public List<Retrieved<Document>> rerank(String query, List<Retrieved<Document>> candidates) {
        List<Retrieved<Document>> out = new ArrayList<Retrieved<Document>>();
        if (candidates == null || candidates.isEmpty()) {
            return out;
        }
        List<String> docs = new ArrayList<String>();
        for (Retrieved<Document> hit : candidates) {
            Document doc = hit.getItem();
            docs.add(doc == null || doc.getContent() == null ? "" : doc.getContent());
        }
        List<Double> scores = port.score(query, docs);
        for (int i = 0; i < candidates.size(); i++) {
            double s = scores != null && i < scores.size() && scores.get(i) != null
                    ? scores.get(i) : candidates.get(i).getScore();
            out.add(candidates.get(i).withScore(s));
        }
        out.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
        return out;
    }
}
