package com.xiongx9527.fw.plus.ai.base.template;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 单条提示词模板：{@code {{name}}} 占位，缺失变量保留原样（便于排查是哪个变量没传）。
 */
public final class PromptTemplate {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\{\\{\\s*([A-Za-z0-9_.-]+)\\s*\\}\\}");

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

    /** 模板里出现的变量名（去重，保持出现顺序）。 */
    public Set<String> variables() {
        Set<String> names = new LinkedHashSet<String>();
        Matcher matcher = PLACEHOLDER.matcher(template);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return Collections.unmodifiableSet(names);
    }

    /** 填充变量；值为 {@code null} 的变量按缺失处理。 */
    public String apply(Map<String, Object> values) {
        if (values == null || values.isEmpty()) {
            return template;
        }
        Matcher matcher = PLACEHOLDER.matcher(template);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            Object value = values.get(matcher.group(1));
            String replacement = value == null ? matcher.group(0) : Matcher.quoteReplacement(String.valueOf(value));
            matcher.appendReplacement(sb, replacement);
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /** 按位置填充（{@code {{0}}}、{@code {{1}}}…）。 */
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
