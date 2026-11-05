package com.xiongx9527.fw.plus.ai.askdata.chart;

import lombok.Builder;
import lombok.Getter;

/** 图表配置降级策略（§6.20 CHART 节点失败 DEGRADE 为表格）。 */
@Getter
@Builder
public class ChartDegradePolicy {
    @lombok.Builder.Default
    private final boolean enabled = true;
}
