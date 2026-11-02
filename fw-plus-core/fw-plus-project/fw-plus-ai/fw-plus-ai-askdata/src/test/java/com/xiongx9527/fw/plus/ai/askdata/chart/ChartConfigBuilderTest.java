package com.xiongx9527.fw.plus.ai.askdata.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartColumn;
import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class ChartConfigBuilderTest {

    private ChartDataset ds() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("east", 10.0));
        rows.add(Arrays.asList("west", 20.0));
        return ChartDataset.builder()
                .columns(Arrays.asList(new ChartColumn("region", "VARCHAR"),
                        new ChartColumn("amount", "DOUBLE")))
                .rows(rows)
                .build();
    }

    @SuppressWarnings("unchecked")
    @Test
    void buildsBarConfigWithMatchingAxisAndSeriesLength() {
        ChartSuggestion s = new ChartSuggestion(ChartType.BAR,
                ChartAxes.builder().categoryField("region")
                        .valueFields(Arrays.asList("amount")).build());
        Map<String, Object> cfg = new ChartConfigBuilder().build(s, ds(), "title");
        assertEquals("bar", cfg.get("type"));
        Map<String, Object> xAxis = (Map<String, Object>) cfg.get("xAxis");
        List<Object> xdata = (List<Object>) xAxis.get("data");
        assertEquals(2, xdata.size());
        List<Object> series = (List<Object>) cfg.get("series");
        assertEquals(1, series.size());
        Map<String, Object> ser = (Map<String, Object>) series.get(0);
        assertEquals("bar", ser.get("type"));
        List<Object> sdata = (List<Object>) ser.get("data");
        assertEquals(2, sdata.size());
    }
}
