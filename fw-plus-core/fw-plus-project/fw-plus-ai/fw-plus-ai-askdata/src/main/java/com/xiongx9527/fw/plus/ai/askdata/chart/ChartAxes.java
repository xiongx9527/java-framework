package com.xiongx9527.fw.plus.ai.askdata.chart;

import java.util.Collections;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/** 图表轴映射：分类轴（X）字段与一个或多个数值轴（Y）字段。 */
@Getter
@Builder
@AllArgsConstructor
public class ChartAxes {
    private final String categoryField;

    @lombok.Builder.Default
    private final List<String> valueFields = Collections.emptyList();
}
