package com.xiongx9527.fw.plus.ai.askdata.chart;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

/** 图表生成输出（§6.20 CHART 节点）。 */
@Getter
@Builder
public class ChartGenResult {
    /** 最终采用的图表类型（降级后为 TABLE）。 */
    private final ChartType chartType;

    /** 可视化配置（ECharts 风格 option）；含 source 供前端回退表格。 */
    private final Map<String, Object> config;

    /** 说明。 */
    private final String explanation;

    /** 0~1 置信度；降级时降低。 */
    @Builder.Default
    private final double confidence = 0.0d;

    /** 是否因校验失败而降级为表格。 */
    @Builder.Default
    private final boolean degraded = false;

    /** 备选图表类型。 */
    @Builder.Default
    private final List<ChartType> alternatives = Collections.emptyList();
}
