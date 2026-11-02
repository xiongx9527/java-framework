package com.xiongx9527.fw.plus.ai.askdata.chart;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 图表生成的显式输入（§6.20 CHART 节点）。 */
@Getter
@Builder
public class ChartGenInput {
    /** 原始问题 / 意图，用于标题与类型判定上下文。 */
    private final String question;

    /** 已执行得到的数据集（如 QueryResult 转换而来）。 */
    private final ChartDataset data;

    /** 偏好图表类型（可选，优先级高于启发式与模型）。 */
    private final ChartType preferredType;

    /** 额外约束（可选）。 */
    @Builder.Default
    private final List<String> constraints = Collections.emptyList();

    public void validate() {
        if (question == null || question.trim().isEmpty()) {
            throw new ChartGenerationException("question 不可为空");
        }
        if (data == null || data.rowCount() == 0) {
            throw new ChartGenerationException("data 不可为空（图表生成依赖已执行的数据集）");
        }
    }
}
