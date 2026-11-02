package com.xiongx9527.fw.plus.ai.askdata.chart;

/** 图表生成能力（§6.20 CHART 节点）。后端无关：给定数据集与问题，产出可视化配置。 */
public interface ChartGenerationCapability {
    ChartGenResult generate(ChartGenInput input);
}
