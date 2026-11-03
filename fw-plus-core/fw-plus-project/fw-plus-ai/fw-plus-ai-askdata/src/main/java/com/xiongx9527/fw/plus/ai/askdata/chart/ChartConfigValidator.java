package com.xiongx9527.fw.plus.ai.askdata.chart;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartColumn;
import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import java.util.List;
import java.util.Map;

/**
 * 图表配置校验（L1 护栏）。两层：
 * <ol>
 *   <li>配置形态：类型合法、轴 / 数据长度一致（isValid(Map)）；</li>
 *   <li>字段存在性：建议引用的分类 / 数值字段必须真实存在于数据集列中（isValid(ChartSuggestion, ChartDataset)）。</li>
 * </ol>
 * 第二层直接封堵风险登记 A.2.2：模型幻觉不存在的列名会让前端 / SSR 画空图，
 * 且 chart 不为空导致既不降级也不告警。这里把字段不在数据集中判为无效，触发降级为表格。
 */
public class ChartConfigValidator {

    @SuppressWarnings("unchecked")
    public boolean isValid(Map<String, Object> config) {
        if (config == null) {
            return false;
        }
        Object t = config.get("type");
        if (!(t instanceof String)) {
            return false;
        }
        String type = ((String) t).toLowerCase();
        if (!type.matches("bar|line|area|pie|scatter|table")) {
            return false;
        }
        if ("table".equals(type)) {
            return true;
        }
        Object series = config.get("series");
        if (!(series instanceof List) || ((List<?>) series).isEmpty()) {
            return false;
        }
        if ("pie".equals(type)) {
            return true;
        }
        Object xAxis = config.get("xAxis");
        if (xAxis instanceof Map) {
            Object xdata = ((Map<String, Object>) xAxis).get("data");
            if (xdata instanceof List) {
                int xlen = ((List<?>) xdata).size();
                for (Object o : (List<Object>) series) {
                    if (o instanceof Map) {
                        Object d = ((Map<String, Object>) o).get("data");
                        if (d instanceof List && ((List<?>) d).size() != xlen) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /** 校验建议引用的字段是否真实存在于数据集（封堵 A.2.2 幻觉列名）。TABLE / UNKNOWN 视为无需校验。 */
    public boolean isValid(ChartSuggestion suggestion, ChartDataset ds) {
        if (suggestion == null || suggestion.getType() == null) {
            return false;
        }
        ChartType type = suggestion.getType();
        if (type == ChartType.UNKNOWN || type == ChartType.TABLE) {
            return true;
        }
        if (ds == null || ds.columnCount() == 0) {
            return false;
        }
        ChartAxes axes = suggestion.getAxes();
        if (axes == null) {
            return false;
        }
        if (!hasField(ds, axes.getCategoryField())) {
            return false;
        }
        List<String> vfs = axes.getValueFields();
        if (vfs == null || vfs.isEmpty()) {
            return false;
        }
        for (String f : vfs) {
            if (!hasField(ds, f)) {
                return false;
            }
        }
        return true;
    }

    /** 数据集完整性自检（扩展 VERIFY）：列数 > 0 且每一行长度都等于列数（矩形，无残缺行）。
     * 非矩形数据集会让 x 轴 / 系列长度不一致，必须降级为表格。 */
    public boolean isDatasetConsistent(ChartDataset ds) {
        if (ds == null || ds.columnCount() == 0 || ds.getRows() == null) {
            return false;
        }
        int cols = ds.columnCount();
        for (List<Object> row : ds.getRows()) {
            if (row == null || row.size() != cols) {
                return false;
            }
        }
        return true;
    }

    private boolean hasField(ChartDataset ds, String name) {
        if (name == null || name.trim().isEmpty() || ds.getColumns() == null) {
            return false;
        }
        for (ChartColumn c : ds.getColumns()) {
            if (name.equals(c.getName())) {
                return true;
            }
        }
        return false;
    }
}
