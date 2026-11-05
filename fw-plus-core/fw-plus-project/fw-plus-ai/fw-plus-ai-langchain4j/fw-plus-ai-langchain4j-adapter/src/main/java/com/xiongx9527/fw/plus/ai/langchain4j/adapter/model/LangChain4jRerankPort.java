package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

import com.xiongx9527.fw.plus.ai.base.model.RerankPort;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;
import java.util.ArrayList;
import java.util.List;

/**
 * 把 LangChain4j 0.35 的 {@link ScoringModel} 适配成 base 的 {@link RerankPort}。
 *
 * <p>0.35 的评分模型是实例级配置，无法按模型名切换，因此
 * {@link #score(String, String, List)} 忽略 model id，委托给当前实例。</p>
 */
public class LangChain4jRerankPort implements RerankPort {

    private final ScoringModel model;

    public LangChain4jRerankPort(ScoringModel model) {
        if (model == null) {
            throw new IllegalArgumentException("scoring model is required");
        }
        this.model = model;
    }

    @Override
    public List<Double> score(String query, List<String> documents) {
        List<Double> out = new ArrayList<Double>();
        if (documents == null || documents.isEmpty()) {
            return out;
        }
        List<TextSegment> segments = new ArrayList<TextSegment>();
        for (String document : documents) {
            segments.add(TextSegment.from(document == null ? "" : document));
        }
        Response<List<Double>> response = model.scoreAll(segments, query == null ? "" : query);
        if (response == null || response.content() == null) {
            for (int i = 0; i < documents.size(); i++) {
                out.add(0.0);
            }
            return out;
        }
        return new ArrayList<Double>(response.content());
    }

    @Override
    public List<Double> score(String modelId, String query, List<String> documents) {
        return score(query, documents);
    }
}
