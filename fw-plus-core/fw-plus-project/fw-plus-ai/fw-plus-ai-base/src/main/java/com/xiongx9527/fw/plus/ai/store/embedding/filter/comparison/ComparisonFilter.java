package com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison;

import com.xiongx9527.fw.plus.ai.data.document.Metadata;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.Filter;

/**
 * 比较类过滤器的公共基类（包内可见）。
 *
 * <p><b>作用</b>：所有「键 op 值」形态的过滤器共享 {@code key} / {@code comparisonValue} 两个字段、
 * 元数据取值逻辑与相等性语义，避免 9 个子类重复实现。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。对应
 * {@code dev.langchain4j.store.embedding.filter.comparison} 包内同名的包级可见基类
 * （官方未对外暴露该类型）。</p>
 *
 * <p><b>实现约定</b>：{@code test(Object)} 的入参必须是 {@link Metadata}，
 * 其它类型一律返回 {@code false}；键不存在也返回 {@code false}，交由具体子类决定后续比较方式。</p>
 *
 * @author xiongxiao
 */
abstract class ComparisonFilter implements Filter {

    /** 元数据键。 */
    protected final String key;

    /** 比较值。 */
    protected final Object comparisonValue;

    /**
     * 构造。
     *
     * @param key             元数据键，不能为空白
     * @param comparisonValue 比较值，不能为 {@code null}
     */
    protected ComparisonFilter(String key, Object comparisonValue) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        this.key = key;
        this.comparisonValue = TypeChecker.ensureTypeIsSupported(comparisonValue, key);
    }

    /**
     * 元数据键。
     *
     * @return 键
     */
    public String key() {
        return key;
    }

    /**
     * 比较值。
     *
     * @return 比较值
     */
    public Object comparisonValue() {
        return comparisonValue;
    }

    /**
     * 从 {@link Metadata} 中取出本过滤器关注的键值。
     *
     * @param metadata 待判定的元数据，必须为 {@link Metadata}
     * @return 键对应的值；非 {@link Metadata} 或键不存在时返回 {@code null}
     */
    protected Object metadataValue(Object metadata) {
        if (!(metadata instanceof Metadata)) {
            return null;
        }
        Metadata md = (Metadata) metadata;
        if (!md.containsKey(key)) {
            return null;
        }
        return md.toMap().get(key);
    }

    /**
     * 判断元数据是否包含本过滤器关注的键。
     *
     * @param metadata 待判定的元数据
     * @return 包含返回 {@code true}
     */
    protected boolean containsKey(Object metadata) {
        return metadata instanceof Metadata && ((Metadata) metadata).containsKey(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ComparisonFilter that = (ComparisonFilter) o;
        return key.equals(that.key) && comparisonValue.equals(that.comparisonValue);
    }

    @Override
    public int hashCode() {
        return 31 * key.hashCode() + comparisonValue.hashCode();
    }
}
