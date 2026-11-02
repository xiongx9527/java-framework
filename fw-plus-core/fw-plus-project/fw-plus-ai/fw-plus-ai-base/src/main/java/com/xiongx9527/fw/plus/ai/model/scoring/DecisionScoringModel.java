package com.xiongx9527.fw.plus.ai.model.scoring;

import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.model.decision.DecisionModel;
import com.xiongx9527.fw.plus.ai.model.decision.request.DecisionRequest;
import com.xiongx9527.fw.plus.ai.model.decision.request.Question;
import com.xiongx9527.fw.plus.ai.model.decision.request.ScaleQuestion;
import com.xiongx9527.fw.plus.ai.model.decision.request.TextInput;
import com.xiongx9527.fw.plus.ai.model.decision.response.DecisionResponse;
import com.xiongx9527.fw.plus.ai.model.decision.response.ScaleAnswer;
import com.xiongx9527.fw.plus.ai.model.output.Response;

import java.util.ArrayList;
import java.util.List;

/**
 * 基于决策模型的打分模型。
 *
 * <p><b>作用</b>：把「对候选片段逐条打分」这一需求，复用决策模型的能力来表达——
 * 为每个片段构造一个 0~1 的量表题，片段作为题面、查询作为决策输入，
 * 模型给出分值即相关度。从而无需额外接入独立的重排服务。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.scoring.DecisionScoringModel}（用 DecisionModel 逐条打分）。</p>
 *
 * <p><b>实现约定</b>：每个片段对应一个量表题，取值区间 [0, 1]，得分取 {@link ScaleAnswer#mean()}；
 * 模型拒绝回答或异常时该片段得分记为 0。</p>
 *
 * @author xiongxiao
 */
public class DecisionScoringModel implements ScoringModel {

    private final DecisionModel decisionModel;

    /**
     * 构造。
     *
     * @param decisionModel 决策模型
     */
    public DecisionScoringModel(DecisionModel decisionModel) {
        this.decisionModel = decisionModel;
    }

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        if (segments == null || segments.isEmpty()) {
            return Response.from(new ArrayList<Double>());
        }
        DecisionRequest.Builder requestBuilder = DecisionRequest.builder().input(TextInput.from(query));
        int index = 0;
        for (TextSegment segment : segments) {
            String key = "seg" + (index++);
            Question question = ScaleQuestion.builder()
                    .text(segment.text())
                    .level("0")
                    .level("1")
                    .build();
            requestBuilder.question(key, question);
        }
        DecisionResponse response = decisionModel.decide(requestBuilder.build());

        List<Double> scores = new ArrayList<Double>(segments.size());
        for (int i = 0; i < segments.size(); i++) {
            String key = "seg" + i;
            ScaleAnswer answer = response.scale(key);
            scores.add(answer == null ? 0.0d : answer.mean());
        }
        return Response.from(scores);
    }

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    public static Builder builder() {
        return new Builder();
    }

    /** {@link DecisionScoringModel} 构建器。 */
    public static class Builder {
        private DecisionModel decisionModel;

        /**
         * 设置决策模型。
         *
         * @param decisionModel 决策模型
         * @return 本构建器
         */
        public Builder decisionModel(DecisionModel decisionModel) {
            this.decisionModel = decisionModel;
            return this;
        }

        /**
         * 构建打分模型。
         *
         * @return 打分模型
         */
        public DecisionScoringModel build() {
            return new DecisionScoringModel(decisionModel);
        }
    }
}
