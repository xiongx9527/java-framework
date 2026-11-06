package com.xiongx9527.fw.plus.ai.askdata;

import com.xiongx9527.fw.plus.ai.askdata.chart.ChartType;
import com.xiongx9527.fw.plus.jdbc.execution.QueryResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/**
 * Output of the end-to-end data-QA orchestration.
 */
@Getter
@Builder
public class AskDataResult {

    /** 生成的 SQL（含方言正确的引号 / 分页）。 */
    private final String sql;

    /** 给用户的简短 SQL 说明。 */
    private final String explanation;

    /** 执行得到的结构化结果集（可能为空或未执行）。 */
    private final QueryResult queryResult;

    /** 0~1 置信度，低置信走澄清（§6.32）。 */
    @lombok.Builder.Default
    private final double confidence = 0.0d;

    /** 生成的 SQL 是否通过校验；false 时不执行。 */
    private final boolean valid;

    /** 引用到的表，供执行期与审计。 */
    @lombok.Builder.Default
    private final List<String> referencedTables = Collections.emptyList();

    /** 漂离时的可选 SQL。 */
    @lombok.Builder.Default
    private final List<String> alternatives = Collections.emptyList();

    /** 执行/结果自检告警。 */
    @lombok.Builder.Default
    private final List<String> warnings = Collections.emptyList();

    /** 是否已执行。 */
    @lombok.Builder.Default
    private final boolean executed = false;

    /** 自然语言答案（§6.38 答案合成）；未启用 AnswerCapability 时为 null。 */
    private final String answer;

    /** 澄清反问文本（低置信 / 生成无效时）；未触发时为 null。 */
    private final String clarification;

    /** 自动推荐的图表类型（§6.38 图表联动）；未启用 ChartGenerationCapability 时为 null。 */
    private final ChartType chartType;

    /** 自动推荐的图表配置（ECharts 风格 option）；未启用时为 null。 */
    private final Map<String, Object> chartConfig;
}
