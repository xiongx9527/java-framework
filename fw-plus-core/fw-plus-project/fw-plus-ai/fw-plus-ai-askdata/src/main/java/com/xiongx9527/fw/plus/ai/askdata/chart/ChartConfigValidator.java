package com.xiongx9527.fw.plus.ai.askdata.chart;

import java.util.List;
import java.util.Map;

/** 图表配置校验（§6.14 L1）。类型未知或轴 / 数据长度不一致时判为无效，触发降级为表格。 */
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
}
