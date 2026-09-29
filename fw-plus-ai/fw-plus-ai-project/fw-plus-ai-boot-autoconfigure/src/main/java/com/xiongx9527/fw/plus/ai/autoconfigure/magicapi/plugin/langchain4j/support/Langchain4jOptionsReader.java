package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support;

import com.alibaba.fastjson2.JSON;

import java.time.Duration;
import java.util.*;

/**
 * options 差异参数的强类型读取：封装风格对齐 MagicAPIConfigUtils（集中读取 + 默认值 + 缺失报错带完整路径）。
 * options 中不同 provider 的键集合不同；各工厂按 builder 需要的键取值。
 */
public class Langchain4jOptionsReader {

    private final String type;
    private final String key;
    private final Map<String, Object> options;

    public Langchain4jOptionsReader(String type, String key, Map<String, Object> options) {
        this.type = type;
        this.key = key;
        this.options = options == null ? Collections.<String, Object>emptyMap() : options;
    }

    public boolean has(String name) {
        return options.get(name) != null;
    }

    public Set<String> names() {
        return options.keySet();
    }

    public String required(String name) {
        String value = string(name, null);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(path(name) + " 不能为空（请在 datasource options 中配置）");
        }
        return value;
    }

    public String string(String name, String defaultValue) {
        Object value = options.get(name);
        return value == null ? defaultValue : String.valueOf(value);
    }

    public int integer(String name, Integer defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue == null ? 0 : defaultValue;
        }
        long parsed = toLong(name, value);
        if (parsed > Integer.MAX_VALUE || parsed < Integer.MIN_VALUE) {
            throw new IllegalArgumentException(path(name) + " 超出 int 范围: " + parsed);
        }
        return (int) parsed;
    }

    public long longValue(String name, Long defaultValue) {
        Object value = options.get(name);
        return value == null ? (defaultValue == null ? 0L : defaultValue) : toLong(name, value);
    }

    /**
     * 读取可空的双精度参数：缺省时返回 {@code defaultValue}（允许为 {@code null}，
     * 表示不设置该参数、由 langchain4j 使用自身默认值）。
     */
    public Double dbl(String name, Double defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(path(name) + " 不是合法数字: " + value);
        }
    }

    public Float floatValue(String name, Float defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).floatValue();
        }
        try {
            return Float.parseFloat(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(path(name) + " 不是合法数字: " + value);
        }
    }

    public Boolean bool(String name, Boolean defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        String text = String.valueOf(value).trim();
        if ("true".equalsIgnoreCase(text) || "false".equalsIgnoreCase(text)) {
            return Boolean.parseBoolean(text);
        }
        throw new IllegalArgumentException(path(name) + " 不是合法布尔值: " + value);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <E extends Enum<E>> E enumValue(String name, Class<E> type, E defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue;
        }
        String text = String.valueOf(value).trim();
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equalsIgnoreCase(text)) {
                return constant;
            }
        }
        throw new IllegalArgumentException(path(name) + " 不是合法枚举值，可选: " + Arrays.toString(type.getEnumConstants()));
    }

    public Duration duration(String name, Duration defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Duration) {
            return (Duration) value;
        }
        String text = String.valueOf(value).trim();
        try {
            if (text.startsWith("PT") || text.startsWith("pt")) {
                return Duration.parse(text);
            }
            if (text.matches("\\d+")) {
                return Duration.ofMillis(Long.parseLong(text));
            }
            String unit = text.substring(text.length() - 1);
            long amount = Long.parseLong(text.substring(0, text.length() - 1).trim());
            switch (unit) {
                case "s": return Duration.ofSeconds(amount);
                case "m": return Duration.ofMinutes(amount);
                case "h": return Duration.ofHours(amount);
                case "d": return Duration.ofDays(amount);
                default: throw new NumberFormatException(text);
            }
        } catch (RuntimeException e) {
            throw new IllegalArgumentException(path(name) + " 不是合法时长（支持 60s/2m/PT1M/毫秒）: " + value);
        }
    }

    public List<String> stringList(String name, List<String> defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue == null ? Collections.<String>emptyList() : defaultValue;
        }
        if (value instanceof List) {
            List<String> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item != null) {
                    result.add(String.valueOf(item));
                }
            }
            return result;
        }
        String text = String.valueOf(value);
        if (text.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(Arrays.asList(text.split("\\s*,\\s*")));
    }

    /**
     * 读取键值表（如 metadata 过滤条件）：值统一转成字符串。
     * 控制台的单行输入只能给出文本，故同时接受 JSON 对象文本与 {@code k=v} 逗号分隔写法。
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> stringMap(String name, Map<String, String> defaultValue) {
        Object value = options.get(name);
        if (value == null) {
            return defaultValue == null ? Collections.<String, String>emptyMap() : defaultValue;
        }
        if (value instanceof Map) {
            Map<String, String> result = new LinkedHashMap<>();
            for (Map.Entry<String, ?> entry : ((Map<String, ?>) value).entrySet()) {
                Object item = entry.getValue();
                result.put(entry.getKey(), item == null ? null : String.valueOf(item));
            }
            return result;
        }
        if (value instanceof String) {
            return parseStringMap(name, ((String) value).trim());
        }
        throw new IllegalArgumentException(path(name) + " 不是合法键值表: " + value);
    }

    private Map<String, String> parseStringMap(String name, String text) {
        if (text.isEmpty()) {
            return Collections.emptyMap();
        }
        if (text.startsWith("{")) {
            Map<String, String> result = new LinkedHashMap<>();
            try {
                for (Map.Entry<String, Object> entry : JSON.parseObject(text).entrySet()) {
                    result.put(entry.getKey(), entry.getValue() == null ? null : String.valueOf(entry.getValue()));
                }
            } catch (RuntimeException e) {
                throw new IllegalArgumentException(path(name) + " 不是合法键值表（JSON 对象解析失败）: " + text);
            }
            return result;
        }
        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : text.split("[,;]")) {
            String item = pair.trim();
            if (item.isEmpty()) {
                continue;
            }
            int index = item.indexOf('=');
            if (index <= 0) {
                throw new IllegalArgumentException(
                        path(name) + " 不是合法键值表（支持 JSON 对象或 k=v 逗号分隔）: " + text);
            }
            result.put(item.substring(0, index).trim(), item.substring(index + 1).trim());
        }
        return result;
    }

    private long toLong(String name, Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(path(name) + " 不是合法整数: " + value);
        }
    }

    private String path(String name) {
        // type 传入完整 datasource type（如 langchain4j-chat-model），错误信息形如
        // langchain4j-chat-model[qwen-main].options.api-key
        return type + "[" + key + "].options." + name;
    }
}
