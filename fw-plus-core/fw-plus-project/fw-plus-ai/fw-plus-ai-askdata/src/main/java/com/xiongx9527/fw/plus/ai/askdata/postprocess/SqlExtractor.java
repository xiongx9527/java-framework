package com.xiongx9527.fw.plus.ai.askdata.postprocess;

import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenerationException;
import com.xiongx9527.fw.plus.ai.askdata.chart.ChartType;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.regex.Pattern;

/**
 * 从模型原始输出中提取 SQL（§6.38 后处理）。
 * 支持：反引号代码块（自动剔除首行 sql 语言标签）、JSON 中的 sql 字段、以 SELECT/WITH 开头的原生文本。
 */
public final class SqlExtractor {

    private static final String FENCE = String.valueOf('\u0060') + String.valueOf('\u0060') + String.valueOf('\u0060');

    /** 代码块首行可能出现的语言标识（用于剔除 sql 这类语言标签）。 */
    private static final Pattern LANG_TAG = Pattern.compile(
            "(?i)(sql|mysql|postgresql|sqlite|clickhouse|doris|starrocks|oracle|dm|kingbase|"
            + "sqlserver|redshift|hive|bigquery|snowflake|spark)");

    private SqlExtractor() {
    }

    public static String extract(String modelOutput) {
        if (modelOutput == null || modelOutput.trim().isEmpty()) {
            throw new SqlGenerationException(50005801, I18nMessageResolver.resolveKey("askdata.extract.empty_output"));
        }
        String out = modelOutput;
        int fStart = out.indexOf(FENCE);
        if (fStart >= 0) {
            int fEnd = out.indexOf(FENCE, fStart + FENCE.length());
            if (fEnd > fStart) {
                return clean(stripLanguageTag(out.substring(fStart + FENCE.length(), fEnd).trim()));
            }
        }
        int key = out.indexOf("\"sql\"");
        if (key >= 0) {
            int colon = out.indexOf(':', key + 5);
            int q1 = out.indexOf('"', colon + 1);
            if (q1 >= 0) {
                int q2 = out.indexOf('"', q1 + 1);
                if (q2 > q1) {
                    return clean(out.substring(q1 + 1, q2));
                }
            }
        }
        String low = out.toLowerCase();
        int idx = -1;
        for (String h : new String[]{"select", "with ", "show ", "explain ", "describe "}) {
            int i = low.indexOf(h);
            if (i >= 0 && (idx < 0 || i < idx)) {
                idx = i;
            }
        }
        if (idx >= 0) {
            String sql = out.substring(idx).trim();
            int semi = sql.indexOf(';');
            if (semi >= 0) {
                sql = sql.substring(0, semi);
            }
            return clean(sql);
        }
        throw new SqlGenerationException(50005801, I18nMessageResolver.resolveKey("askdata.extract.no_sql"));
    }

    public static String extractExplanation(String modelOutput) {
        if (modelOutput == null) {
            return null;
        }
        int idx = modelOutput.indexOf(FENCE);
        if (idx <= 0) {
            return null;
        }
        String before = modelOutput.substring(0, idx).trim();
        return before.isEmpty() ? null : before;
    }

    /**
     * 从模型输出抽取「会话摘要」（产品 SQL JSON 协议中的 brief）。模型未按 JSON 返回该字段时返回 null，
     * 不影响主链路 SQL 抽取。仅做轻量 JSON 字符串字段解析，不做完整 JSON 反序列化。
     */
    public static String extractBrief(String modelOutput) {
        if (modelOutput == null) {
            return null;
        }
        return jsonStringField(modelOutput, "brief");
    }

    /**
     * 从模型输出抽取「建议图表类型」（产品 SQL JSON 协议中的 chart-type / chartType）。
     * 命中时映射为 {@link ChartType} 枚举，未返回或无法识别时返回 null。
     */
    public static ChartType extractChartType(String modelOutput) {
        if (modelOutput == null) {
            return null;
        }
        String raw = jsonStringField(modelOutput, "chart-type");
        if (raw == null) {
            raw = jsonStringField(modelOutput, "chartType");
        }
        return raw == null ? null : ChartType.fromHint(raw);
    }

    /** 轻量抽取 JSON 字符串字段 {@code "key":"value"} 中的 value（不限空白）。 */
    private static String jsonStringField(String out, String key) {
        String qKey = "\"" + key + "\"";
        int keyIdx = out.indexOf(qKey);
        if (keyIdx < 0) {
            return null;
        }
        int colon = out.indexOf(':', keyIdx + qKey.length());
        if (colon < 0) {
            return null;
        }
        int q1 = out.indexOf('"', colon + 1);
        if (q1 < 0) {
            return null;
        }
        int q2 = out.indexOf('"', q1 + 1);
        if (q2 < q1) {
            return null;
        }
        return out.substring(q1 + 1, q2);
    }

    private static String stripLanguageTag(String s) {
        if (s == null) {
            return null;
        }
        int nl = s.indexOf('\n');
        if (nl < 0) {
            return s;
        }
        String first = s.substring(0, nl).trim();
        if (LANG_TAG.matcher(first).matches()) {
            return s.substring(nl + 1).trim();
        }
        return s;
    }

    private static String clean(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        if (t.endsWith(";")) {
            t = t.substring(0, t.length() - 1).trim();
        }
        return t;
    }
}
