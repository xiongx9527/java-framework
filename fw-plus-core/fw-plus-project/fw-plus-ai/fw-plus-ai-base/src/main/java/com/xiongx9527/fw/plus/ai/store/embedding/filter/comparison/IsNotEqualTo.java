package com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison;

import java.util.UUID;

/**
 * 不等过滤：{@code metadata[key] != comparisonValue}。
 *
 * <p><b>作用</b>：排除型过滤，例如「排除已删除文档」。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.filter.comparison.IsNotEqualTo}。</p>
 *
 * <p><b>实现约定</b>：语义为 {@link IsEqualTo} 的取反，类型校验规则一致。</p>
 *
 * @author xiongxiao
 */
public class IsNotEqualTo extends ComparisonFilter {

    /**
     * 构造。
     *
     * @param key             元数据键，不能为空白
     * @param comparisonValue 比较值，不能为 {@code null}
     */
    public IsNotEqualTo(String key, Object comparisonValue) {
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
            return !NumberComparator.isEqualTo(actual, comparisonValue);
        }
        if (comparisonValue instanceof UUID && actual instanceof String) {
            return !actual.equals(comparisonValue.toString());
        }
        return !actual.equals(comparisonValue);
    }

    @Override
    public String toString() {
        return key + " != " + comparisonValue;
    }
}
