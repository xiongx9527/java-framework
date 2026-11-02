package com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.UUID;

/**
 * 过滤条件类型校验工具（包内可见）。
 *
 * <p><b>作用</b>：在 {@code test()} 真正比较之前，先校验「元数据实际值的类型」与「比较值类型」是否兼容，
 * 把「拿字符串比数字」这类隐性错误变成带键名的明确异常，而不是静默返回 {@code false}。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.filter.comparison.TypeChecker}。</p>
 *
 * <p><b>实现约定</b>：支持的类型为 String、UUID 与各类 {@link Number}（含 BigDecimal / BigInteger）；
 * 元数据值缺失（{@code null}）时跳过校验，由调用方返回 {@code false}。</p>
 *
 * @author xiongxiao
 */
final class TypeChecker {

    /** 支持的类型提示语，用于异常消息。 */
    private static final String SUPPORTED_TYPES_HINT = "String, UUID, int, long, float, double, BigDecimal, BigInteger";

    private TypeChecker() {
    }

    /**
     * 校验单个比较值类型受支持。
     *
     * @param value 比较值
     * @param key   元数据键，仅用于异常消息
     * @param <T>   值类型
     * @return 原值
     */
    static <T> T ensureTypeIsSupported(T value, String key) {
        if (value == null) {
            throw new IllegalArgumentException(
                    "Comparison value for metadata key '" + key + "' must not be null");
        }
        if (!isSupportedType(value)) {
            throw new IllegalArgumentException(
                    "Type " + value.getClass().getName() + " of comparison value for metadata key '" + key
                            + "' is not supported. Supported types: " + SUPPORTED_TYPES_HINT);
        }
        return value;
    }

    /**
     * 校验集合中所有比较值类型受支持。
     *
     * @param values 比较值集合
     * @param key    元数据键，仅用于异常消息
     */
    static void ensureTypesAreSupported(Collection<?> values, String key) {
        if (values == null || values.isEmpty()) {
            throw new IllegalArgumentException(
                    "Comparison values for metadata key '" + key + "' must not be null or empty");
        }
        for (Object value : values) {
            ensureTypeIsSupported(value, key);
        }
    }

    /**
     * 校验元数据实际值与比较值类型兼容。
     *
     * @param actualValue     元数据中的实际值，可为 {@code null}
     * @param comparisonValue 比较值
     * @param key             元数据键，仅用于异常消息
     */
    static void ensureTypesAreCompatible(Object actualValue, Object comparisonValue, String key) {
        if (actualValue == null) {
            return;
        }
        boolean actualIsNumber = actualValue instanceof Number;
        boolean comparisonIsNumber = comparisonValue instanceof Number;
        if (actualIsNumber != comparisonIsNumber) {
            throw new IllegalArgumentException(
                    "Type mismatch for metadata key '" + key + "': actual value is "
                            + actualValue.getClass().getName() + " but comparison value is "
                            + comparisonValue.getClass().getName());
        }
        if (actualIsNumber) {
            return;
        }
        if (comparisonValue instanceof UUID && actualValue instanceof String) {
            return;
        }
        if (comparisonValue instanceof String && actualValue instanceof String) {
            return;
        }
        if (!actualValue.getClass().equals(comparisonValue.getClass())) {
            throw new IllegalArgumentException(
                    "Type mismatch for metadata key '" + key + "': actual value is "
                            + actualValue.getClass().getName() + " but comparison value is "
                            + comparisonValue.getClass().getName());
        }
    }

    private static boolean isSupportedType(Object value) {
        return value instanceof String
                || value instanceof UUID
                || value instanceof Integer
                || value instanceof Long
                || value instanceof Float
                || value instanceof Double
                || value instanceof Short
                || value instanceof Byte
                || value instanceof BigDecimal
                || value instanceof BigInteger;
    }
}
