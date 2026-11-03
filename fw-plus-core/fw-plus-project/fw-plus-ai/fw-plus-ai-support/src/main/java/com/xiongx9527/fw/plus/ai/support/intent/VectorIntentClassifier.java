package com.xiongx9527.fw.plus.ai.support.intent;

import com.xiongx9527.fw.plus.ai.base.intent.Intent;
import com.xiongx9527.fw.plus.ai.base.intent.IntentMatch;
import com.xiongx9527.fw.plus.ai.base.intent.IntentPort;
import com.xiongx9527.fw.plus.ai.base.intent.IntentRequest;
import com.xiongx9527.fw.plus.ai.base.intent.IntentResult;
import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import java.util.ArrayList;
import java.util.List;

/**
 * 向量意图分类：把文本与候选描述各自向量化，按余弦相似度打分。
 *
 * <p>适合语义相近、关键词不完全重叠的意图（如「查一下销售额」与「经营数据」）。</p>
 */
public final class VectorIntentClassifier implements IntentPort {

    private final EmbeddingPort embedding;

    private final double threshold;

    public VectorIntentClassifier(EmbeddingPort embedding) {
        this(embedding, 0.0);
    }

    public VectorIntentClassifier(EmbeddingPort embedding, double threshold) {
        if (embedding == null) {
            throw new IllegalArgumentException("embedding port is required");
        }
        this.embedding = embedding;
        this.threshold = threshold;
    }

    @Override
    public IntentResult classify(IntentRequest request) {
        List<IntentMatch> matches = new ArrayList<IntentMatch>();
        if (request == null || request.getText() == null || request.getCandidates().isEmpty()) {
            return IntentResult.of(matches);
        }
        double[] textVec = embedding.embed(request.getText());
        for (Intent candidate : request.getCandidates()) {
            double[] candVec = embedding.embed(candidate.getDescription());
            double score = VectorMath.cosine(textVec, candVec);
            if (score >= threshold) {
                matches.add(new IntentMatch(candidate.getRef(), candidate.getLabel(), score));
            }
        }
        return IntentResult.of(matches);
    }
}
