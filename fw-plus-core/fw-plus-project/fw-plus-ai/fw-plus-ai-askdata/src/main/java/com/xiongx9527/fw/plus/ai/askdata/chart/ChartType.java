package com.xiongx9527.fw.plus.ai.askdata.chart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * 图表类型（§6.20 CHART 节点）。与可视化库解耦，仅表达语义类型。
 *
 * <p>类型识别用的别名（中英文同义词，属匹配令牌而非展示文案）外置到
 * {@code chart/chart-type-aliases.properties}，以便下游通过 classpath 覆盖或追加，
 * 不在代码中散落 {@code contains} 硬编码。{@link #fromHint(String)} 对别名做小写子串匹配，
 * 保持与原实现一致的宽松语义；匹配优先级按资源文件声明顺序（靠前的先匹配）。
 */
public enum ChartType {
    TABLE, BAR, LINE, AREA, PIE, SCATTER, HEATMAP, UNKNOWN;

    /** 类型 → 别名列表（按资源文件声明顺序，决定匹配优先级）。 */
    private static final Map<ChartType, String[]> ALIASES = loadAliases();

    public static ChartType fromHint(String hint) {
        if (hint == null || hint.trim().isEmpty()) {
            return UNKNOWN;
        }
        String h = hint.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<ChartType, String[]> e : ALIASES.entrySet()) {
            for (String alias : e.getValue()) {
                if (h.contains(alias)) {
                    return e.getKey();
                }
            }
        }
        return UNKNOWN;
    }

    /**
     * 从 classpath 的 {@code chart/chart-type-aliases.properties} 加载别名表。
     * 资源缺失时返回空表（{@code fromHint} 退化为全 UNKNOWN），不抛异常。
     */
    private static Map<ChartType, String[]> loadAliases() {
        Map<ChartType, String[]> map = new LinkedHashMap<>();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ChartType.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream("chart/chart-type-aliases.properties")) {
            if (in == null) {
                return map;
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                        continue;
                    }
                    int eq = trimmed.indexOf('=');
                    if (eq < 0) {
                        continue;
                    }
                    String name = trimmed.substring(0, eq).trim();
                    String value = trimmed.substring(eq + 1).trim();
                    ChartType type;
                    try {
                        type = ChartType.valueOf(name);
                    } catch (IllegalArgumentException ignored) {
                        continue;
                    }
                    if (type == UNKNOWN) {
                        continue;
                    }
                    String[] aliases = value.isEmpty() ? new String[0] : value.split(",");
                    for (int i = 0; i < aliases.length; i++) {
                        aliases[i] = aliases[i].trim().toLowerCase(Locale.ROOT);
                    }
                    map.put(type, aliases);
                }
            }
        } catch (IOException ignored) {
            // 资源读取失败时退化为空表，fromHint 返回 UNKNOWN
        }
        return map;
    }
}
