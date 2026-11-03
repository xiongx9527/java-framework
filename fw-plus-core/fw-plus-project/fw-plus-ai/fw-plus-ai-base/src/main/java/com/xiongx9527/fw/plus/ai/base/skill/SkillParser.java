package com.xiongx9527.fw.plus.ai.base.skill;

import java.util.ArrayList;
import java.util.List;

/**
 * SKILL.md 解析器：把「frontmatter + 指令正文」拆成 {@link SkillDescriptor}。
 *
 * <p>只认 SKILL.md 约定里用得到的那一小撮 YAML：{@code key: value}、行内列表
 * {@code key: [a, b]}、块列表（{@code key:} 换行后若干 {@code - a}）。不引入 YAML 依赖，
 * 复杂结构请改用远程技能目录。</p>
 *
 * <pre>
 * ---
 * id: monthly-report
 * version: 1.2.0
 * description: 按月份生成经营分析报告
 * tools: [exec_sql, chart_render]
 * permissions: [datasource:read, export:write]
 * requiresConfirm: false
 * ---
 * 指令正文……
 * </pre>
 */
public final class SkillParser {

    private static final String DELIMITER = "---";

    private SkillParser() {
    }

    /**
     * 解析技能文件内容。
     *
     * @param content SKILL.md 全文
     * @return 技能描述
     * @throws IllegalArgumentException 缺少 frontmatter 或缺少 id
     */
    public static SkillDescriptor parse(String content) {
        if (content == null) {
            throw new IllegalArgumentException("skill content is required");
        }
        String text = content.replace("\r\n", "\n").replace('\r', '\n');
        int start = text.indexOf(DELIMITER);
        if (start < 0) {
            throw new IllegalArgumentException("skill frontmatter is required");
        }
        int headerStart = text.indexOf('\n', start);
        if (headerStart < 0) {
            throw new IllegalArgumentException("skill frontmatter is not closed");
        }
        int end = text.indexOf("\n" + DELIMITER, headerStart);
        if (end < 0) {
            throw new IllegalArgumentException("skill frontmatter is not closed");
        }
        String header = text.substring(headerStart + 1, end);
        String body = text.substring(end + DELIMITER.length() + 1).trim();

        SkillDescriptor.Builder builder = SkillDescriptor.builder();
        String pendingListKey = null;
        List<String> pendingValues = new ArrayList<String>();

        for (String rawLine : header.split("\n")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("- ")) {
                if (pendingListKey != null) {
                    pendingValues.add(unquote(line.substring(2).trim()));
                }
                continue;
            }
            pendingListKey = applyPending(builder, pendingListKey, pendingValues);
            int colon = line.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String key = line.substring(0, colon).trim();
            String value = line.substring(colon + 1).trim();
            if (value.isEmpty()) {
                pendingListKey = key;
                pendingValues.clear();
                continue;
            }
            applyScalar(builder, key, value);
        }
        applyPending(builder, pendingListKey, pendingValues);

        builder.instructions(body.isEmpty() ? null : body);
        return builder.build();
    }

    private static String applyPending(SkillDescriptor.Builder builder, String key, List<String> values) {
        if (key == null) {
            return null;
        }
        if (!values.isEmpty()) {
            for (String value : values) {
                applyListValue(builder, key, value);
            }
        }
        values.clear();
        return null;
    }

    private static void applyScalar(SkillDescriptor.Builder builder, String key, String rawValue) {
        String value = unquote(rawValue);
        if ("id".equals(key)) {
            builder.id(value);
        } else if ("name".equals(key)) {
            builder.name(value);
        } else if ("version".equals(key)) {
            builder.version(value);
        } else if ("description".equals(key)) {
            builder.description(value);
        } else if ("requiresConfirm".equals(key) || "requires_confirm".equals(key)) {
            builder.requiresConfirm(Boolean.parseBoolean(value));
        } else if ("transport".equals(key)) {
            builder.transport(value);
        } else if ("source".equals(key)) {
            builder.source("REMOTE".equalsIgnoreCase(value) ? SkillSource.REMOTE : SkillSource.LOCAL);
        } else if (isListKey(key)) {
            for (String item : splitInlineList(value)) {
                applyListValue(builder, key, item);
            }
        }
    }

    private static void applyListValue(SkillDescriptor.Builder builder, String key, String value) {
        if ("tools".equals(key) || "toolWhitelist".equals(key) || "tool_whitelist".equals(key)) {
            builder.addTool(value);
        } else if ("permissions".equals(key)) {
            builder.addPermission(value);
        }
    }

    private static boolean isListKey(String key) {
        return "tools".equals(key) || "toolWhitelist".equals(key) || "tool_whitelist".equals(key)
                || "permissions".equals(key);
    }

    private static List<String> splitInlineList(String value) {
        List<String> items = new ArrayList<String>();
        String inner = value;
        if (inner.startsWith("[") && inner.endsWith("]")) {
            inner = inner.substring(1, inner.length() - 1);
        }
        for (String item : inner.split(",")) {
            String trimmed = unquote(item.trim());
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return items;
    }

    private static String unquote(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2) {
            char first = trimmed.charAt(0);
            char last = trimmed.charAt(trimmed.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }
}
