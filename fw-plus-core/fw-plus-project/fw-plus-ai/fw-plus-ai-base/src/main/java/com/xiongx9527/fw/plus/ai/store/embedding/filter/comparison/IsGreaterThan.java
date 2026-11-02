package com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison;

/**
 * 大于过滤：{@code metadata[key] > comparisonValue}。
 *
 * <p><b>作用</b>：对数值或可比较类型（如时间字符串）做区间筛选，例如「只检索最近 7 天的文档」。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.filter.comparison.IsGreaterThan}。</p>
 *
 * <p><b>实现约定</b>：判断实际值大于比较值；数值走 {@link NumberComparator}，
 * 其它类型直接调用 {@link Comparable#compareTo}。</p>
 *
 * @author xiongxiao
 */
public class IsGreaterThan extends ComparisonFilter {

    /**
     * 构造。
     *
     * @param key             元数据键，不能为空白
     * @param comparisonValue 比较值，不能为 {@code null}
     */
    public IsGreaterThan(String key, Comparable<?> comparisonValue) {
        super(key, comparisonValue);
    }

    /**
     * 比较值。
     *
     * @return 比较值
     */
    @Override
    public Comparable<?> comparisonValue() {
        return (Comparable<?>) super.comparisonValue();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public boolean test(Object metadata) {
        if (!containsKey(metadata)) {
            return false;
        }
        Object actual = metadataValue(metadata);
        TypeChecker.ensureTypesAreCompatible(actual, comparisonValue(), key);
        if (actual instanceof Number) {
            return NumberComparator.isGreaterThan(actual, comparisonValue());
        }
        return ((Comparable) actual).compareTo(comparisonValue()) > 0;
    }

    @Override
    public String toString() {
        return key + " > " + comparisonValue();
    }
}
