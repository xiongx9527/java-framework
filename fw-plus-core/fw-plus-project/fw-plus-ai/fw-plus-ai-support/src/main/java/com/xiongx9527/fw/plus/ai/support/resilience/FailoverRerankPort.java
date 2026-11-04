package com.xiongx9527.fw.plus.ai.support.resilience;

import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.base.model.RerankPort;
import com.xiongx9527.fw.plus.ai.base.resilience.ModelFailoverPort;
import com.xiongx9527.fw.plus.ai.base.resilience.ModelHealthPort;
import java.util.List;

/**
 * Rerank failover: same shape as FailoverLlmPort, tries a prioritized candidate rerank-model chain.
 * Given a model id, ModelFailoverPort.candidates returns the ordered candidates (including self);
 * when the previous one throws or ModelHealthPort marks it unavailable, the next is tried.
 * With no candidates, the primary model is used directly.
 */
public final class FailoverRerankPort implements RerankPort {

    private final RerankPort primary;
    private final ModelFailoverPort failover;
    private final ModelHealthPort health;

    public FailoverRerankPort(RerankPort primary, ModelFailoverPort failover, ModelHealthPort health) {
        if (primary == null) {
            throw new IllegalArgumentException("primary rerank port is required");
        }
        this.primary = primary;
        this.failover = failover;
        this.health = health;
    }

    @Override
    public List<Double> score(String query, List<String> documents) {
        return score(null, query, documents);
    }

    @Override
    public List<Double> score(String model, String query, List<String> documents) {
        List<String> candidates = failover == null ? null : failover.candidates(model);
        if (candidates == null || candidates.isEmpty()) {
            return primary.score(model, query, documents);
        }
        RuntimeException last = null;
        for (String candidate : candidates) {
            if (health != null && !health.available(candidate)) {
                continue;
            }
            try {
                List<Double> scores = primary.score(candidate, query, documents);
                if (health != null) {
                    health.record(candidate, ModelHealthPort.Outcome.SUCCESS);
                }
                return scores;
            } catch (RuntimeException e) {
                last = e;
                if (health != null) {
                    health.record(candidate, ModelHealthPort.Outcome.FAILURE);
                }
            }
        }
        throw last == null ? new IllegalStateException("no available rerank model for: " + model) : last;
    }

    @Override
    public List<ModelInfo> listModels() {
        return primary.listModels();
    }

    @Override
    public ModelValidation validateModel(String model) {
        return primary.validateModel(model);
    }
}
