package com.xiongx9527.fw.plus.ai.askdata.chart;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 图表类型建议 + 轴映射。 */
@Getter
@AllArgsConstructor
public class ChartSuggestion {
    private final ChartType type;
    private final ChartAxes axes;
}
