package com.xiongx9527.fw.plus.ai.askdata.chart;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartColumn;
import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;

/** 根据 {@link ChartGenInput} 构造图表决策提示词。 */
public class ChartPromptBuilder {

    public String buildSystem() {
        return ChartPromptTemplates.SYSTEM;
    }

    public String buildUser(ChartGenInput input) {
        ChartDataset ds = input.getData();
        String schema = summarizeSchema(ds);
        String sample = summarizeSample(ds, 5);
        return ChartPromptTemplates.USER_TEMPLATE
                .replace("{question}", nullToEmpty(input.getQuestion()))
                .replace("{schema}", schema)
                .replace("{sampleLimit}", "5")
                .replace("{sample}", sample);
    }

    private String summarizeSchema(ChartDataset ds) {
        StringBuilder sb = new StringBuilder();
        if (ds.getColumns() != null) {
            for (ChartColumn c : ds.getColumns()) {
                sb.append("- ").append(c.getName()).append(" : ")
                        .append(c.getType() == null ? "?" : c.getType())
                        .append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    private String summarizeSample(ChartDataset ds, int limit) {
        StringBuilder sb = new StringBuilder();
        if (ds.getRows() != null) {
            int n = Math.min(limit, ds.rowCount());
            for (int i = 0; i < n; i++) {
                sb.append(ds.getRows().get(i)).append(System.lineSeparator());
            }
        }
        return sb.toString();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
