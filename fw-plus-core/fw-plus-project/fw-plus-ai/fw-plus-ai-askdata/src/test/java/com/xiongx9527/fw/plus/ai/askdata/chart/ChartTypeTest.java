package com.xiongx9527.fw.plus.ai.askdata.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ChartTypeTest {

    @Test
    void englishAliasMatches() {
        assertEquals(ChartType.PIE, ChartType.fromHint("pie"));
        assertEquals(ChartType.SCATTER, ChartType.fromHint("scatter"));
        assertEquals(ChartType.TABLE, ChartType.fromHint("grid"));
        assertEquals(ChartType.BAR, ChartType.fromHint("bar chart"));
    }

    @Test
    void chineseSubstringMatches() {
        assertEquals(ChartType.PIE, ChartType.fromHint("饼图"));
        assertEquals(ChartType.BAR, ChartType.fromHint("柱状"));
        assertEquals(ChartType.LINE, ChartType.fromHint("折线趋势"));
        assertEquals(ChartType.AREA, ChartType.fromHint("面积图"));
        assertEquals(ChartType.HEATMAP, ChartType.fromHint("热力图 heatmap"));
    }

    @Test
    void nullAndBlankReturnUnknown() {
        assertEquals(ChartType.UNKNOWN, ChartType.fromHint(null));
        assertEquals(ChartType.UNKNOWN, ChartType.fromHint("   "));
    }

    @Test
    void noMatchReturnsUnknown() {
        assertEquals(ChartType.UNKNOWN, ChartType.fromHint("gantt"));
        assertEquals(ChartType.UNKNOWN, ChartType.fromHint("雷达图"));
    }
}
