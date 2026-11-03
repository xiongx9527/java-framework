package com.xiongx9527.fw.plus.ai.askdata.chart;

/** 图表决策提示词模板（§6.20 CHART 节点，模型产出图表类型与轴字段）。 */
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;

/** 图表决策提示词模板（§6.20 CHART 节点，模型产出图表类型与轴字段）。文案来自 i18n 资源。 */
public final class ChartPromptTemplates {
    private ChartPromptTemplates() {
    }

    public static final String SYSTEM = I18nMessageResolver.resolveKey("askdata.prompt.chart.system");

    public static final String USER_TEMPLATE = I18nMessageResolver.resolveKey("askdata.prompt.chart.user");
}
