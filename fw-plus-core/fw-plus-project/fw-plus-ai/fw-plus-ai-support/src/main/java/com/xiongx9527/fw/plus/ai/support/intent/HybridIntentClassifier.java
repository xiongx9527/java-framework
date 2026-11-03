package com.xiongx9527.fw.plus.ai.support.intent;

import com.xiongx9527.fw.plus.ai.base.intent.Intent;
import com.xiongx9527.fw.plus.ai.base.intent.IntentMatch;
import com.xiongx9527.fw.plus.ai.base.intent.IntentPort;
import com.xiongx9527.fw.plus.ai.base.intent.IntentRequest;
import com.xiongx9527.fw.plus.ai.base.intent.IntentResult;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 混合意图分类：组合多个子分类器，按权重合并同 ref 的分数。
 *
 * <p>典型组合：正则（精确命中）+ 向量（语义召回）+ LLM（消歧），权重可调；
 * 合并后取最高分作为最终意图。</p>
 */
public final class HybridIntentClassifier implements IntentPort {

    private final List<IntentPort> classifiers;

    private final List<Double> weights;

    public HybridIntentClassifier(List<IntentPort> classifiers, List<Double> weights) {
        if (classifiers == null || classifiers.isEmpty()) {
            throw new IllegalArgumentException("at least one classifier is required");
        }
        this.classifiers = new ArrayList<IntentPort>(classifiers);
        this.weights = new ArrayList<Double>(weights == null ? defaultWeights(classifiers.size()) : weights);
        if (this.weights.size() < this.classifiers.size()) {
            while (this.weights.size() < this.classifiers.size()) {
                this.weights.add(1.0);
            }
        }
    }

    private static List<Double> defaultWeights(int n) {
        List<Double> w = new ArrayList<Double>();
        for (int i = 0; i < n; i++) {
            w.add(1.0);
        }
        return w;
    }

    @Override
    public IntentResult classify(IntentRequest request) {
        Map<String, IntentMatch> merged = new LinkedHashMap<String, IntentMatch>();
        for (int i = 0; i < classifiers.size(); i++) {
            IntentResult partial = classifiers.get(i).classify(request);
            double weight = weights.get(i);
            if (partial == null) {
                continue;
            }
            for (IntentMatch match : partial.getMatches()) {
                IntentMatch existing = merged.get(match.getRef());
                double combined = (existing == null ? 0.0 : existing.getScore()) + match.getScore() * weight;
                Intent candidate = find(request, match.getRef());
                merged.put(match.getRef(), new IntentMatch(match.getRef(),
                        candidate == null ? match.getLabel() : candidate.getLabel(), combined));
            }
        }
        return IntentResult.of(new ArrayList<IntentMatch>(merged.values()));
    }

    private static Intent find(IntentRequest request, String ref) {
        if (request == null) {
            return null;
        }
        for (Intent intent : request.getCandidates()) {
            if (intent.getRef().equals(ref)) {
                return intent;
            }
        }
        return null;
    }
}
