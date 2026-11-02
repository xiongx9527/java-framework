package com.xiongx9527.fw.plus.ai.askdata.chart;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartColumn;
import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 根据图表建议与数据集构造可视化配置（ECharts 风格 option），与具体前端库解耦。 */
public class ChartConfigBuilder {

    public Map<String, Object> build(ChartSuggestion suggestion, ChartDataset ds, String question) {
        ChartType type = suggestion.getType();
        Map<String, Object> cfg = new LinkedHashMap<>();
        cfg.put("type", type.name().toLowerCase());
        cfg.put("title", question == null ? "" : question);
        cfg.put("source", source(ds));
        switch (type) {
            case PIE:
                cfg.put("series", pieSeries(suggestion, ds));
                break;
            case SCATTER:
                cfg.put("xAxis", nameAxis(suggestion.getAxes().getCategoryField()));
                cfg.put("yAxis", nameAxis(join(valueFields(suggestion))));
                cfg.put("series", scatterSeries(suggestion, ds));
                break;
            case LINE:
            case AREA:
            case BAR:
            default:
                cfg.put("xAxis", catAxis(suggestion, ds));
                cfg.put("yAxis", nameAxis(join(valueFields(suggestion))));
                cfg.put("series", barSeries(suggestion, ds, type));
                break;
            case TABLE:
                break;
        }
        return cfg;
    }

    private Map<String, Object> source(ChartDataset ds) {
        Map<String, Object> m = new LinkedHashMap<>();
        List<String> cols = new ArrayList<>();
        if (ds.getColumns() != null) {
            for (ChartColumn c : ds.getColumns()) {
                cols.add(c.getName());
            }
        }
        m.put("columns", cols);
        m.put("rows", ds.getRows());
        return m;
    }

    private List<String> valueFields(ChartSuggestion s) {
        List<String> v = s.getAxes().getValueFields();
        return v == null ? new ArrayList<String>() : v;
    }

    private String join(List<String> v) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.size(); i++) {
            if (i > 0) {
                sb.append("/");
            }
            sb.append(v.get(i));
        }
        return sb.toString();
    }

    private Map<String, Object> nameAxis(String name) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name == null ? "" : name);
        return m;
    }

    private Map<String, Object> catAxis(ChartSuggestion s, ChartDataset ds) {
        Map<String, Object> m = nameAxis(s.getAxes().getCategoryField());
        List<Object> data = new ArrayList<>();
        int idx = indexOf(ds, s.getAxes().getCategoryField());
        if (idx >= 0) {
            data = ds.columnValues(idx);
        }
        m.put("data", data);
        return m;
    }

    private List<Map<String, Object>> barSeries(ChartSuggestion s, ChartDataset ds, ChartType type) {
        List<Map<String, Object>> series = new ArrayList<>();
        String seriesType = type == ChartType.LINE ? "line" : (type == ChartType.AREA ? "area" : "bar");
        for (String vf : valueFields(s)) {
            Map<String, Object> ser = new LinkedHashMap<>();
            ser.put("name", vf);
            ser.put("type", seriesType);
            int idx = indexOf(ds, vf);
            ser.put("data", idx >= 0 ? ds.columnValues(idx) : new ArrayList<Object>());
            series.add(ser);
        }
        return series;
    }

    private List<Map<String, Object>> pieSeries(ChartSuggestion s, ChartDataset ds) {
        List<Map<String, Object>> series = new ArrayList<>();
        String catField = s.getAxes().getCategoryField();
        List<String> vfs = valueFields(s);
        String vField = vfs.isEmpty() ? null : vfs.get(0);
        int ci = indexOf(ds, catField);
        int vi = vField == null ? -1 : indexOf(ds, vField);
        List<Map<String, Object>> data = new ArrayList<>();
        if (ds.getRows() != null) {
            for (List<Object> row : ds.getRows()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", ci >= 0 && ci < row.size() ? row.get(ci) : null);
                item.put("value", vi >= 0 && vi < row.size() ? row.get(vi) : null);
                data.add(item);
            }
        }
        Map<String, Object> ser = new LinkedHashMap<>();
        ser.put("type", "pie");
        ser.put("data", data);
        series.add(ser);
        return series;
    }

    private List<Map<String, Object>> scatterSeries(ChartSuggestion s, ChartDataset ds) {
        String xField = s.getAxes().getCategoryField();
        List<String> vfs = valueFields(s);
        String yField = vfs.isEmpty() ? null : vfs.get(0);
        int xi = indexOf(ds, xField);
        int yi = yField == null ? -1 : indexOf(ds, yField);
        List<Object> data = new ArrayList<>();
        if (ds.getRows() != null) {
            for (List<Object> row : ds.getRows()) {
                List<Object> pt = new ArrayList<>();
                pt.add(xi >= 0 && xi < row.size() ? row.get(xi) : null);
                pt.add(yi >= 0 && yi < row.size() ? row.get(yi) : null);
                data.add(pt);
            }
        }
        Map<String, Object> ser = new LinkedHashMap<>();
        ser.put("type", "scatter");
        ser.put("data", data);
        List<Map<String, Object>> series = new ArrayList<>();
        series.add(ser);
        return series;
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
