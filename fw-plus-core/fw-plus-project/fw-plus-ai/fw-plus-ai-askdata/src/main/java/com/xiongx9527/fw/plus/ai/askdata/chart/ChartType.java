package com.xiongx9527.fw.plus.ai.askdata.chart;

import java.util.Locale;

/** 图表类型（§6.20 CHART 节点）。与可视化库解耦，仅表达语义类型。 */
public enum ChartType {
    TABLE, BAR, LINE, AREA, PIE, SCATTER, HEATMAP, UNKNOWN;

    public static ChartType fromHint(String hint) {
        if (hint == null || hint.trim().isEmpty()) {
            return UNKNOWN;
        }
        String h = hint.trim().toLowerCase(Locale.ROOT);
        if (h.contains("pie") || h.contains("饼")) return PIE;
        if (h.contains("scatter") || h.contains("散点")) return SCATTER;
        if (h.contains("heat") || h.contains("热力")) return HEATMAP;
        if (h.contains("area") || h.contains("面积")) return AREA;
        if (h.contains("line") || h.contains("线") || h.contains("趋势")) return LINE;
        if (h.contains("bar") || h.contains("柱状") || h.contains("柱")) return BAR;
        if (h.contains("table") || h.contains("表") || h.contains("grid")) return TABLE;
        return UNKNOWN;
    }
}
