package com.xiongx9527.fw.plus.ai.langchain4j.adapter.support;

import com.xiongx9527.fw.plus.ai.base.store.vector.Filter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 把 base 的 {@link Filter} 条件树翻译成 LangChain4j 0.35 的
 * {@code dev.langchain4j.store.embedding.filter.Filter}，实现库内过滤下推。
 *
 * <p>返回 {@code null} 表示「无法完整下推」：CONTAINS 无原生比较器、或比较值不是
 * {@code Comparable} 时，条件树中任一节点不能翻译就整体放弃，调用方必须退回到
 * {@link Filter#matches(java.util.Map)} 后过滤，避免静默放宽过滤条件。</p>
 */
public final class LcFilters {

    private LcFilters() {
    }

    public static dev.langchain4j.store.embedding.filter.Filter toUpstream(Filter filter) {
        if (filter == null || filter.isNoOp()) {
            return null;
        }
        return convert(filter);
    }

    private static dev.langchain4j.store.embedding.filter.Filter convert(Filter filter) {
        Filter.Op op = filter.op();
        if (op == Filter.Op.AND || op == Filter.Op.OR) {
            List<dev.langchain4j.store.embedding.filter.Filter> children =
                    new ArrayList<dev.langchain4j.store.embedding.filter.Filter>();
            for (Filter child : filter.children()) {
                dev.langchain4j.store.embedding.filter.Filter converted = convert(child);
                if (converted == null) {
                    return null;
                }
                children.add(converted);
            }
            if (children.isEmpty()) {
                return null;
            }
            dev.langchain4j.store.embedding.filter.Filter result = children.get(0);
            for (int i = 1; i < children.size(); i++) {
                result = op == Filter.Op.AND
                        ? result.and(children.get(i)) : result.or(children.get(i));
            }
            return result;
        }
        String key = filter.key();
        Object value = filter.value();
        switch (op) {
            case EQ:
                return new dev.langchain4j.store.embedding.filter.comparison.IsEqualTo(key, value);
            case NE:
                return new dev.langchain4j.store.embedding.filter.comparison.IsNotEqualTo(key, value);
            case IN:
                return value instanceof Collection
                        ? new dev.langchain4j.store.embedding.filter.comparison.IsIn(
                                key, (Collection<?>) value) : null;
            case GT:
                return value instanceof Comparable
                        ? new dev.langchain4j.store.embedding.filter.comparison.IsGreaterThan(
                                key, (Comparable<?>) value) : null;
            case GTE:
                return value instanceof Comparable
                        ? new dev.langchain4j.store.embedding.filter.comparison.IsGreaterThanOrEqualTo(
                                key, (Comparable<?>) value) : null;
            case LT:
                return value instanceof Comparable
                        ? new dev.langchain4j.store.embedding.filter.comparison.IsLessThan(
                                key, (Comparable<?>) value) : null;
            case LTE:
                return value instanceof Comparable
                        ? new dev.langchain4j.store.embedding.filter.comparison.IsLessThanOrEqualTo(
                                key, (Comparable<?>) value) : null;
            case CONTAINS:
                // 0.35 没有字符串包含比较器
                return null;
            default:
                return null;
        }
    }
}
