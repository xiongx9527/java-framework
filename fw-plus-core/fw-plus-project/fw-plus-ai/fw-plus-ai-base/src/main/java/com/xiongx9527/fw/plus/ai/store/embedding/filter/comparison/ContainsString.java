package com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison;

/**
 * 字符串包含过滤：{@code metadata[key].contains(comparisonValue)}。
 *
 * <p><b>作用</b>：对标题、路径、标签等文本型元数据做子串匹配。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.filter.comparison.ContainsString}。</p>
 *
 * <p><b>实现约定</b>：元数据值必须是 String，否则抛 {@link IllegalArgumentException}
 * （类型不匹配属于调用方配置错误，静默返回 {@code false} 会掩盖问题）。</p>
 *
 * @author xiongxiao
 */
public class ContainsString extends ComparisonFilter {

    /**
     * 构造。
     *
     * @param key             元数据键，不能为空白
     * @param comparisonValue 待匹配的子串，不能为 {@code null}
     */
    public ContainsString(String key, String comparisonValue) {
        super(key, comparisonValue);
    }

    /**
     * 待匹配的子串。
     *
     * @return 子串
     */
    @Override
    public String comparisonValue() {
        return (String) super.comparisonValue();
    }

    @Override
    public boolean test(Object metadata) {
        if (!containsKey(metadata)) {
            return false;
        }
        Object actual = metadataValue(metadata);
        if (actual instanceof String) {
            return ((String) actual).contains(comparisonValue());
        }
        throw new IllegalArgumentException("Type mismatch: actual value of metadata key '" + key + "' ("
                + actual + ") has type " + actual.getClass().getName()
                + ", while it is expected to be a string");
    }

    @Override
    public String toString() {
        return key + " CONTAINS " + comparisonValue();
    }
}
