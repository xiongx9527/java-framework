package com.xiongx9527.fw.plus.ai.askdata.chart;

import java.util.ArrayList;
import java.util.List;

/** 从模型输出解析图表决策 JSON（受控字段解析，不依赖 JSON 库）。 */
public class ChartLlmParser {

    private static final String Q = String.valueOf('"');

    public ChartSuggestion parse(String modelOutput) {
        if (modelOutput == null) {
            return null;
        }
        int a = modelOutput.indexOf('{');
        int b = modelOutput.lastIndexOf('}');
        if (a < 0 || b <= a) {
            return null;
        }
        String json = modelOutput.substring(a, b + 1);
        String type = field(json, "chartType");
        if (type == null) {
            return null;
        }
        ChartType ct = ChartType.fromHint(type);
        if (ct == ChartType.UNKNOWN) {
            ct = ChartType.BAR;
        }
        String cat = field(json, "categoryField");
        List<String> vals = arrayField(json, "valueFields");
        return new ChartSuggestion(ct, ChartAxes.builder().categoryField(cat).valueFields(vals).build());
    }

    private String field(String json, String key) {
        String token = Q + key + Q;
        int i = json.indexOf(token);
        if (i < 0) {
            return null;
        }
        int colon = json.indexOf(':', i + token.length());
        if (colon < 0) {
            return null;
        }
        int q1 = json.indexOf(Q, colon + 1);
        if (q1 < 0) {
            return null;
        }
        int q2 = json.indexOf(Q, q1 + 1);
        if (q2 < 0) {
            return null;
        }
        return json.substring(q1 + 1, q2);
    }

    private List<String> arrayField(String json, String key) {
        String token = Q + key + Q;
        List<String> out = new ArrayList<>();
        int i = json.indexOf(token);
        if (i < 0) {
            return out;
        }
        int bracket = json.indexOf('[', i + token.length());
        if (bracket < 0) {
            return out;
        }
        int end = json.indexOf(']', bracket + 1);
        if (end < 0) {
            return out;
        }
        String inner = json.substring(bracket + 1, end);
        for (String part : inner.split(",")) {
            String s = part.trim();
            if (s.startsWith(Q)) {
                s = s.substring(1);
            }
            if (s.endsWith(Q)) {
                s = s.substring(0, s.length() - 1);
            }
            s = s.trim();
            if (!s.isEmpty()) {
                out.add(s);
            }
        }
        return out;
    }
}
