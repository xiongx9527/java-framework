package com.xiongx9527.fw.plus.ai.askdata.postprocess;

import com.xiongx9527.fw.plus.ai.askdata.capability.SqlGenerationException;
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
            throw new SqlGenerationException("model output is null or empty");
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
        throw new SqlGenerationException("no SQL found in model output");
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
