package com.xiongx9527.fw.plus.ai.askdata.chart;

/** 图表决策提示词模板（§6.20 CHART 节点，模型产出图表类型与轴字段）。 */
public final class ChartPromptTemplates {
    private ChartPromptTemplates() {
    }

    public static final String SYSTEM = ""
            + "你是一个数据可视化助手。任务：根据数据概要与自然语言问题，选择最合适的图表类型，并给出分类轴字段与数值轴字段。\n"
            + "只允许从 bar / line / area / pie / scatter / table 中选择。\n"
            + "严格只输出一个 JSON 对象，格式：\n"
            + "{\"chartType\":\"<类型>\",\"categoryField\":\"<分类轴字段名或空>\",\"valueFields\":[\"<数值字段名>\"]}";

    public static final String USER_TEMPLATE = ""
            + "问题：{question}\n"
            + "数据概要：\n{schema}\n"
            + "样本数据（最多 {sampleLimit} 行）：\n{sample}\n"
            + "请给出图表配置 JSON。";
}
