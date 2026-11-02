package com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison;

import java.util.Collection;
import java.util.UUID;

/**
 * UUID 比较工具（包内可见）。
 *
 * <p><b>作用</b>：元数据里的 UUID 可能以字符串形式存储，而过滤条件里给的是 {@link UUID} 对象；
 * 本工具统一解析后再比较，避免因表示形式不同导致漏匹配。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.filter.comparison.UUIDComparator}。</p>
 *
 * @author xiongxiao
 */
final class UUIDComparator {

    private UUIDComparator() {
    }

    /**
     * 判断实际值是否命中候选 UUID 集合。
     *
     * @param actual 元数据实际值，可为 {@link UUID} 或字符串形式
     * @param values 候选集合，元素可为 {@link UUID} 或字符串形式
     * @return 命中返回 {@code true}；无法解析为 UUID 时返回 {@code false}
     */
    static boolean containsAsUUID(Object actual, Collection<?> values) {
        UUID actualUuid = toUuid(actual);
        if (actualUuid == null) {
            return false;
        }
        for (Object value : values) {
            if (actualUuid.equals(toUuid(value))) {
                return true;
            }
        }
        return false;
    }

    private static UUID toUuid(Object value) {
        if (value instanceof UUID) {
            return (UUID) value;
        }
        if (value instanceof String) {
            try {
                return UUID.fromString((String) value);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        return null;
    }
}
