package com.xiongx9527.fw.plus.ai.askdata.capability;

import com.xiongx9527.fw.plus.ai.askdata.chart.ChartType;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * SQL 生成能力的输出（§6.38.2）。
 */
@Getter
@Builder
public class SqlGenResult {
    /** 生成的 SQL（含方言正确的引号 / 分页）。 */
    private final String sql;

    /** 给用户的简短说明。 */
    private final String explanation;

    /** 引用到的表，供执行期与审计。 */
    @lombok.Builder.Default
    private final List<String> referencedTables = Collections.emptyList();

    /** 0~1 置信度，低置信走 §6.32 反问。 */
    @lombok.Builder.Default
    private final double confidence = 0.0d;

    /** 漂离时的可选 SQL。 */
    @lombok.Builder.Default
    private final List<String> alternatives = Collections.emptyList();

    /** 最终校验是否通过；false 表示 SQL 未通过本地 / LLM 校验，调用方不应直接执行。 */
    @lombok.Builder.Default
    private final boolean valid = false;

    /**
     * 建议图表类型（产品 SQL JSON 协议中的 chart-type）。由模型在结构化输出中附带，
     * 框架透明抽取；模型未返回时为 null，调用方可改由图表能力独立决策。
     */
    private final ChartType chartType;

    /**
     * 会话摘要（产品 SQL JSON 协议中的 brief），用于重命名会话 / 概览。
     * 模型未返回时为 null，调用方回退到完整问题。
     */
    private final String brief;
}

