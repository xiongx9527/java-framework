package com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison;

import com.xiongx9527.fw.plus.ai.data.document.Metadata;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.Filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 集合命中过滤：{@code metadata[key] in (v1, v2, ...)}。
 *
 * <p><b>作用</b>：一次表达多个候选值，例如「检索属于这几个标签的片段」。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.filter.comparison.IsIn}。</p>
 *
 * <p><b>实现约定</b>：数值走 {@link NumberComparator#isIn} 做跨类型比较；
 * UUID 走 {@link UUIDComparator#containsAsUUID}；其它类型用 {@link Collection#contains}。</p>
 *
 * @author xiongxiao
 */
public class IsIn implements Filter {

    private final String key;
    private final Collection<?> comparisonValues;

    /**
     * 构造。
     *
     * @param key             元数据键，不能为空白
     * @param comparisonValues 候选值集合，不能为 {@code null} 或空
     */
    public IsIn(String key, Collection<?> comparisonValues) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("key must not be blank");
        }
        TypeChecker.ensureTypesAreSupported(comparisonValues, key);
        this.key = key;
        this.comparisonValues = Collections.unmodifiableList(new ArrayList<Object>(comparisonValues));
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
     * 候选值集合。
     *
     * @return 只读集合
     */
    public Collection<?> comparisonValues() {
        return comparisonValues;
    }

    @Override
    public boolean test(Object metadata) {
        if (!(metadata instanceof Metadata)) {
            return false;
        }
        Metadata md = (Metadata) metadata;
        if (!md.containsKey(key)) {
            return false;
        }
        Object actual = md.toMap().get(key);
        Object first = comparisonValues.iterator().next();
        TypeChecker.ensureTypesAreCompatible(actual, first, key);
        if (first instanceof Number) {
            return NumberComparator.isIn(actual, comparisonValues);
        }
        if (first instanceof UUID) {
            return UUIDComparator.containsAsUUID(actual, comparisonValues);
        }
        return comparisonValues.contains(actual);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IsIn)) {
            return false;
        }
        IsIn that = (IsIn) o;
        return key.equals(that.key) && comparisonValues.equals(that.comparisonValues);
    }

    @Override
    public int hashCode() {
        return 31 * key.hashCode() + comparisonValues.hashCode();
    }

    @Override
    public String toString() {
        return key + " IN " + comparisonValues;
    }
}
