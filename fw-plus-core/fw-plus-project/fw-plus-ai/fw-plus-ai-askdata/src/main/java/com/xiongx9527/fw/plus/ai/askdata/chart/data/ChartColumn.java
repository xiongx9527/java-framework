package com.xiongx9527.fw.plus.ai.askdata.chart.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 数据集的单个列。 */
@Getter
@AllArgsConstructor
public class ChartColumn {
    private final String name;
    private final String type;
}
