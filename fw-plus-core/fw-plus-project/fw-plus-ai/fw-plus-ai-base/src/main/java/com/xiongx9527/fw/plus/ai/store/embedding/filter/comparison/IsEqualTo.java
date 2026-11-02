package com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison;

import java.util.UUID;

/**
 * 相等过滤：{@code metadata[key] == comparisonValue}。
 *
 * <p><b>作用</b>：最常见的元数据过滤形态，例如「只检索某租户 / 某文档集合」。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.filter.comparison.IsEqualTo}。</p>
 *
 * <p><b>实现约定</b>：数值走 {@link NumberComparator} 做跨类型比较；
 * {@link UUID} 比较值可与字符串形式的元数据值匹配。</p>
 *
 * @author xiongxiao
 */
public class IsEqualTo extends ComparisonFilter {

    /**
     * 构造。
     *
     * @param key             元数据键，不能为空白
     * @param comparisonValue 比较值，不能为 {@code null}
     */
    public IsEqualTo(String key, Object comparisonValue) {
        super(key, comparisonValue);
    }

    @Override
    public boolean test(Object metadata) {
        if (!containsKey(metadata)) {
            return false;
        }
        Object actual = metadataValue(metadata);
        TypeChecker.ensureTypesAreCompatible(actual, comparisonValue, key);
        if (actual instanceof Number) {
            return NumberComparator.isEqualTo(actual, comparisonValue);
        }
        if (comparisonValue instanceof UUID && actual instanceof String) {
            return actual.equals(comparisonValue.toString());
        }
        return actual.equals(comparisonValue);
    }

    @Override
    public String toString() {
        return key + " == " + comparisonValue;
    }
}
