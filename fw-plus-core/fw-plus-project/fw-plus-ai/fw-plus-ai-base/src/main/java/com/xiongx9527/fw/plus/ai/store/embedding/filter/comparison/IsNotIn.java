package com.xiongx9527.fw.plus.ai.store.embedding.filter.comparison;

import com.xiongx9527.fw.plus.ai.data.document.Metadata;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.Filter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * 集合排除过滤：{@code metadata[key] not in (v1, v2, ...)}。
 *
 * <p><b>作用</b>：一次排除多个候选值，例如「排除这几个标签的片段」。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.filter.comparison.IsNotIn}。</p>
 *
 * <p><b>实现约定</b>：语义为 {@link IsIn} 的取反，类型校验规则一致。</p>
 *
 * @author xiongxiao
 */
public class IsNotIn implements Filter {

    private final String key;
    private final Collection<?> comparisonValues;

    /**
     * 构造。
     *
     * @param key              元数据键，不能为空白
     * @param comparisonValues 候选值集合，不能为 {@code null} 或空
     */
    public IsNotIn(String key, Collection<?> comparisonValues) {
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
            return !NumberComparator.isIn(actual, comparisonValues);
        }
        if (first instanceof UUID) {
            return !UUIDComparator.containsAsUUID(actual, comparisonValues);
        }
        return !comparisonValues.contains(actual);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IsNotIn)) {
            return false;
        }
        IsNotIn that = (IsNotIn) o;
        return key.equals(that.key) && comparisonValues.equals(that.comparisonValues);
    }

    @Override
    public int hashCode() {
        return 31 * key.hashCode() + comparisonValues.hashCode();
    }

    @Override
    public String toString() {
        return key + " NOT IN " + comparisonValues;
    }
}
