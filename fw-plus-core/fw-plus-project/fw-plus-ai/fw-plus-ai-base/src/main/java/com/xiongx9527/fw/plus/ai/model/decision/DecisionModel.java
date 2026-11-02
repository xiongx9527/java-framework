package com.xiongx9527.fw.plus.ai.model.decision;

import com.xiongx9527.fw.plus.ai.model.output.Response;
import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import java.util.List;

/**
 * 决策模型端口（简化中性形态），对应 langchain4j 1.23.0 的 {@code dev.langchain4j.model.decision.DecisionModel}。
 *
 * <p>官方 DecisionModel 有更丰富的 YesNo / Choice / Scale 请求响应；本抽象只保留「对候选片段与 query 逐条打分」这一打分所需的最小契约，
 * 供 {@link com.xiongx9527.fw.plus.ai.model.scoring.DecisionScoringModel} 使用。
 */
public interface DecisionModel {
    Response<List<Double>> decide(List<TextSegment> segments, String query);
}
