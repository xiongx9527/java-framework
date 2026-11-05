package com.xiongx9527.fw.plus.ai.base.retrieval;

import com.xiongx9527.fw.plus.ai.base.model.RerankPort;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import java.util.ArrayList;
import java.util.List;

/**
 * 把 {@link RerankPort} 的打分结果套回检索候选的默认 {@code Reranker<Document>}。
 *
 * <p>放在 base：任何后端产出的 {@code RerankPort} 都能经 {@link Reranker#from(RerankPort)}
 * 一步接到检索链上，不必依赖增强层，也不必在每个 adapter 里重复这套组合。</p>
 *
 * <p>失败语义：打分异常<b>不吞</b>，向上传播由调用方处理；返回分数缺失 / 数量不足时
 * 该条沿用原分，而不是静默丢弃整批候选。</p>
 */
public class DefaultReranker implements Reranker<Document> {

    private final RerankPort port;

    public DefaultReranker(RerankPort port) {
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
        List<String> texts = new ArrayList<String>();
        for (Retrieved<Document> candidate : candidates) {
            Document doc = candidate == null ? null : candidate.getItem();
            texts.add(doc == null || doc.getContent() == null ? "" : doc.getContent());
        }
        List<Double> scores = port.score(query, texts);
        for (int i = 0; i < candidates.size(); i++) {
            Retrieved<Document> candidate = candidates.get(i);
            if (candidate == null) {
                continue;
            }
            Double score = scores == null || i >= scores.size() ? null : scores.get(i);
            out.add(score == null ? candidate : candidate.withScore(score.doubleValue()));
        }
        out.sort((left, right) -> Double.compare(right.getScore(), left.getScore()));
        return out;
    }
}
