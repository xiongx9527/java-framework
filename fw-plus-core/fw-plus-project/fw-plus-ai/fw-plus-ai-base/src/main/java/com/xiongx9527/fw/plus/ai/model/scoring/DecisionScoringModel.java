package com.xiongx9527.fw.plus.ai.model.scoring;

import com.xiongx9527.fw.plus.ai.model.output.Response;
import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.model.decision.DecisionModel;
import java.util.List;

/** 基于决策模型的打分模型，镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.model.scoring.DecisionScoringModel}（用 DecisionModel 逐条打分）。 */
public class DecisionScoringModel implements ScoringModel {

    private final DecisionModel decisionModel;

    public DecisionScoringModel(DecisionModel decisionModel) { this.decisionModel = decisionModel; }

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        return decisionModel.decide(segments, query);
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private DecisionModel decisionModel;
        public Builder decisionModel(DecisionModel decisionModel) { this.decisionModel = decisionModel; return this; }
        public DecisionScoringModel build() { return new DecisionScoringModel(decisionModel); }
    }
}
