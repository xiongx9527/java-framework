package com.xiongx9527.fw.plus.ai.askdata.chart;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
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

    /**
     * 生成该数据集所依据的 SQL（可选）。对齐产品 CHART 节点携带 {@code <sql>} 上下文的行为，
     * 让图表决策能参考查询意图而非仅看结果数据。
     */
    private final String sql;

    /**
     * 与 SQL 对应的 schema 上下文（可选，如 m-schema）。与 {@link #sql} 配合，
     * 帮助模型理解字段含义与来源。
     */
    private final String mSchema;

    /** 额外约束（可选）。 */
    @lombok.Builder.Default
    private final List<String> constraints = Collections.emptyList();

    public void validate() {
        if (question == null || question.trim().isEmpty()) {
            throw new ChartGenerationException(50005803, I18nMessageResolver.resolveKey("askdata.input.question_required"));
        }
        if (data == null || data.rowCount() == 0) {
            throw new ChartGenerationException(50005803, I18nMessageResolver.resolveKey("askdata.input.chart_data_required"));
        }
    }
}
