package com.xiongx9527.fw.plus.ai.askdata.chart;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;

/** 基于数据形状的图表类型启发式（§6.20 CHART 节点降级 / 缺模型时的默认决策）。 */
public class ChartTypeGuesser {

    public ChartSuggestion guess(ChartDataset ds) {
        if (ds == null || ds.rowCount() == 0 || ds.columnCount() == 0) {
            return new ChartSuggestion(ChartType.TABLE, ChartAxes.builder().build());
        }
        java.util.List<String> numeric = ds.numericFieldNames();
        java.util.List<String> cat = ds.categoricalFieldNames();
        if (numeric.isEmpty()) {
            return new ChartSuggestion(ChartType.TABLE, ChartAxes.builder().build());
        }
        if (cat.isEmpty()) {
            if (numeric.size() == 2) {
                return new ChartSuggestion(ChartType.SCATTER,
                        ChartAxes.builder().categoryField(numeric.get(0)).valueFields(numeric).build());
            }
            return new ChartSuggestion(ChartType.TABLE, ChartAxes.builder().build());
        }
        String catField = cat.get(0);
        int catIdx = indexOf(ds, catField);
        int card = catIdx >= 0 ? ds.cardinality(catIdx) : Integer.MAX_VALUE;
        if (numeric.size() == 1) {
            String vField = numeric.get(0);
            if (card >= 2 && card <= 8 && ds.rowCount() <= 50) {
                return new ChartSuggestion(ChartType.PIE,
                        ChartAxes.builder().categoryField(catField).valueFields(numeric).build());
            }
            return new ChartSuggestion(ChartType.BAR,
                    ChartAxes.builder().categoryField(catField).valueFields(numeric).build());
        }
        return new ChartSuggestion(ChartType.BAR,
                ChartAxes.builder().categoryField(catField).valueFields(numeric).build());
    }

    private int indexOf(ChartDataset ds, String name) {
        if (ds.getColumns() == null || name == null) {
            return -1;
        }
        for (int i = 0; i < ds.columnCount(); i++) {
            if (name.equals(ds.getColumns().get(i).getName())) {
                return i;
            }
        }
        return -1;
    }
}
