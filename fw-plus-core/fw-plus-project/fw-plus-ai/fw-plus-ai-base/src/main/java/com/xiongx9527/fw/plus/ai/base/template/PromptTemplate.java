package com.xiongx9527.fw.plus.ai.base.template;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 单条提示词模板：{{name}} 占位，缺失变量保留原样（便于排查是哪个变量没传）。
 *
 * <p>占位还支持「格式化」与「默认值」两种轻量表达式：</p>
 * <ul>
 *   <li>{{name:pattern}} —— 对日期 / 时间 / 数字按 pattern 格式化；</li>
 *   <li>{{name ?? default}} —— 变量缺失或为空时使用默认值；</li>
 *   <li>{{name:pattern ?? default}} —— 两者可组合。</li>
 * </ul>
 *
 * <p>内置系统变量（由 AI 服务在渲染时注入）见 AiServices：now（当前时间）、
 * today（今天）、intent（意图引用）、memoryId（会话 id）、
 * traceId（链路 id）、service（服务类型名）。</p>
 */
public final class PromptTemplate {

    private static final Pattern PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*(.*?)\\s*\\}\\s*\\}");

    private final String template;

    private PromptTemplate(String template) {
        this.template = template;
    }

    public static PromptTemplate from(String template) {
        return new PromptTemplate(template == null ? "" : template);
    }

    public String template() {
        return template;
    }

    /** 模板里出现的变量名（去重，保持出现顺序，不含格式化 / 默认值表达式）。 */
    public Set<String> variables() {
        Set<String> names = new LinkedHashSet<String>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return Collections.unmodifiableSet(names);
    }

    /** 填充变量；值为 null 的变量按缺失处理。 */
    public String apply(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String name = matcher.group(1);
            String spec = matcher.group(2) == null ? "" : matcher.group(2).trim();
            String format = null;
            String def = null;
            if (!spec.isEmpty()) {
                if (spec.startsWith(":")) {
                    String rest = spec.substring(1).trim();
                    int q = rest.indexOf("??");
                    if (q >= 0) {
                        format = rest.substring(0, q).trim();
                        def = rest.substring(q + 2).trim();
                    } else {
                        format = rest;
                    }
                } else if (spec.startsWith("??")) {
                    def = spec.substring(2).trim();
                }
            }
            Object value = values.get(name);
            boolean missing = value == null || (value instanceof String && ((String) value).isEmpty());
            String replacement;
            if (missing && def != null) {
                replacement = def;
            } else if (value == null) {
                replacement = matcher.group(0);
            } else if (format != null) {
                replacement = format(value, format);
            } else {
                replacement = String.valueOf(value);
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String format(Object value, String pattern) {
        try {
            if (value instanceof TemporalAccessor) {
                return DateTimeFormatter.ofPattern(pattern).format((TemporalAccessor) value);
            }
            if (value instanceof Date) {
                return new SimpleDateFormat(pattern).format((Date) value);
            }
            if (value instanceof LocalDate) {
                return DateTimeFormatter.ofPattern(pattern).format((LocalDate) value);
            }
            if (value instanceof Number) {
                return new DecimalFormat(pattern).format((Number) value);
            }
        } catch (RuntimeException ignored) {
            // 格式不合法时退化为原值，避免整段渲染失败
        }
        return String.valueOf(value);
    }

    /** 按位置填充（{{0}}、{{1}}…）。 */
    public String apply(Object... values) {
        if (values == null || values.length == 0) {
            return template;
        }
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<String, Object>();
        for (int i = 0; i < values.length; i++) {
            map.put(String.valueOf(i), values[i]);
        }
        return apply(map);
    }

    @Override
    public String toString() {
        return "PromptTemplate{" + template + '}';
    }
}
