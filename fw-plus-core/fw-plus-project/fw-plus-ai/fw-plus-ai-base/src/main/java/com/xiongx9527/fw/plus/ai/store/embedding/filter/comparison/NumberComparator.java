package com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison;

import java.math.BigDecimal;
import java.util.Collection;

/**
 * 数值比较工具（包内可见）。
 *
 * <p><b>作用</b>：把不同数值类型（Integer / Long / Float / Double / BigDecimal…）统一折算成
 * {@code double} 再比较，避免 {@code Integer.equals(Long)} 恒为 {@code false} 这类跨类型陷阱；
 * 同时对 {@code NaN} / {@code Infinity} 做保守处理。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.filter.comparison.NumberComparator}。</p>
 *
 * <p><b>实现约定</b>：只接受 {@link Number} 入参，调用方需先用
 * {@link TypeChecker#ensureTypesAreCompatible} 完成类型校验。</p>
 *
 * @author xiongxiao
 */
final class NumberComparator {

    private NumberComparator() {
    }

    /**
     * 判断两数相等。
     *
     * @param actual     元数据实际值
     * @param comparison 比较值
     * @return 相等返回 {@code true}；任一为 {@code NaN} 返回 {@code false}
     */
    static boolean isEqualTo(Object actual, Object comparison) {
        if (!neitherIsNaN(actual, comparison)) {
            return false;
        }
        return compare(actual, comparison) == 0;
    }

    /**
     * 判断实际值大于比较值。
     *
     * @param actual     元数据实际值
     * @param comparison 比较值
     * @return 大于返回 {@code true}
     */
    static boolean isGreaterThan(Object actual, Object comparison) {
        return compare(actual, comparison) > 0;
    }

    /**
     * 判断实际值大于等于比较值。
     *
     * @param actual     元数据实际值
     * @param comparison 比较值
     * @return 大于等于返回 {@code true}
     */
    static boolean isGreaterThanOrEqualTo(Object actual, Object comparison) {
        return compare(actual, comparison) >= 0;
    }

    /**
     * 判断实际值小于比较值。
     *
     * @param actual     元数据实际值
     * @param comparison 比较值
     * @return 小于返回 {@code true}
     */
    static boolean isLessThan(Object actual, Object comparison) {
        return compare(actual, comparison) < 0;
    }

    /**
     * 判断实际值小于等于比较值。
     *
     * @param actual     元数据实际值
     * @param comparison 比较值
     * @return 小于等于返回 {@code true}
     */
    static boolean isLessThanOrEqualTo(Object actual, Object comparison) {
        return compare(actual, comparison) <= 0;
    }

    /**
     * 判断实际值是否落在候选集合中。
     *
     * @param actual 元数据实际值
     * @param values 候选集合
     * @return 命中任一候选返回 {@code true}
     */
    static boolean isIn(Object actual, Collection<?> values) {
        for (Object value : values) {
            if (isEqualTo(actual, value)) {
                return true;
            }
        }
        return false;
    }

    private static int compare(Object actual, Object comparison) {
        return Double.compare(doubleValue(actual), doubleValue(comparison));
    }

    private static boolean neitherIsNaN(Object first, Object second) {
        return !isNaN(first) && !isNaN(second);
    }

    private static boolean isNaN(Object value) {
        return value instanceof Double && ((Double) value).isNaN()
                || value instanceof Float && ((Float) value).isNaN();
    }

    private static double doubleValue(Object value) {
        if (value instanceof BigDecimal) {
            return ((BigDecimal) value).doubleValue();
        }
        return ((Number) value).doubleValue();
    }
}
