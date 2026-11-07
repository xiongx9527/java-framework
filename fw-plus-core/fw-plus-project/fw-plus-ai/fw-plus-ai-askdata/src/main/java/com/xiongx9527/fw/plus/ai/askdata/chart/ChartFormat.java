package com.xiongx9527.fw.plus.ai.askdata.chart;

/**
 * 图表配置输出格式（§6.20 CHART 节点）。
 *
 * <p>框架默认产出 ECharts 风格配置（{@link #ECHARTS}）；部分前端（如 askdata 的 G2 图表块）
 * 消费 G2 风格配置（{@code type/title/columns/axis}），此时用 {@link #G2} 模式：
 * 提示词改为要求模型直接输出 G2 配置 JSON，能力层跳过 ECharts 构造 / 校验，原样返回该 Map。</p>
 */
public enum ChartFormat {
    ECHARTS,
    G2
}
