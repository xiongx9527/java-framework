package com.xiongx9527.fw.plus.ai.base.store.vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 元数据过滤 DSL：向量检索时按文档属性收敛候选集（与按主键收敛的 {@code candidateIds} 互补）。
 *
 * <p>中性结构：base 给出条件组合与求值器 {@link #matches(Map)}，adapter 只需在检索前对候选文档
 * 调一次 {@code request.getFilter().matches(metadata)}，不必懂各向量库自己的 filter 语法。</p>
 *
 * <p>示例：{@code Filter.builder().eq("dsId", 7).gt("score", 0.5).build()}
 * 表示 {@code dsId == 7 AND score > 0.5}。</p>
 */
public final class Filter {

    /** 永真的空过滤（不过滤）。 */
    public static final Filter NO_OP = new Filter(Op.AND, null, null, Collections.<Filter>emptyList());

    private enum Op {
        EQ, NE, IN, GT, GTE, LT, LTE, CONTAINS, AND, OR
    }

    private final Op op;

    private final String key;

    private final Object value;

    private final List<Filter> children;

    private Filter(Op op, String key, Object value, List<Filter> children) {
        this.op = op;
        this.key = key;
        this.value = value;
        this.children = children == null ? Collections.<Filter>emptyList()
                : Collections.unmodifiableList(new ArrayList<Filter>(children));
    }

    /** 判断给定元数据是否满足本过滤条件。 */
    public boolean matches(Map<String, Object> metadata) {
        if (op == Op.AND) {
            for (Filter child : children) {
                if (!child.matches(metadata)) {
                    return false;
                }
            }
            return true;
        }
        if (op == Op.OR) {
            for (Filter child : children) {
                if (child.matches(metadata)) {
                    return true;
                }
            }
            return false;
        }
        Object actual = metadata == null ? null : metadata.get(key);
        switch (op) {
            case EQ:
                return equals(actual, value);
            case NE:
                return !equals(actual, value);
            case IN:
                return value instanceof Collection && ((Collection<?>) value).contains(actual);
            case GT:
                return compare(actual, value) > 0;
            case GTE:
                return compare(actual, value) >= 0;
            case LT:
                return compare(actual, value) < 0;
            case LTE:
                return compare(actual, value) <= 0;
            case CONTAINS:
                return actual != null && String.valueOf(actual).contains(String.valueOf(value));
            default:
                return false;
        }
    }

    private static boolean equals(Object actual, Object expected) {
        if (actual == null && expected == null) {
            return true;
        }
        if (actual == null || expected == null) {
            return false;
        }
        if (actual instanceof Number && expected instanceof Number) {
            return ((Number) actual).doubleValue() == ((Number) expected).doubleValue();
        }
        return actual.equals(expected);
    }

    private static int compare(Object actual, Object expected) {
        if (actual == null || expected == null) {
            return -1;
        }
        if (actual instanceof Number && expected instanceof Number) {
            return Double.compare(((Number) actual).doubleValue(), ((Number) expected).doubleValue());
        }
        if (actual instanceof Comparable && expected instanceof Comparable) {
            try {
                @SuppressWarnings("unchecked")
                int c = ((Comparable<Object>) actual).compareTo(expected);
                return c;
            } catch (ClassCastException ignored) {
                return -1;
            }
        }
        return -1;
    }

    @Override
    public String toString() {
        switch (op) {
            case EQ:
                return key + "==" + value;
            case NE:
                return key + "!=" + value;
            case IN:
                return key + " in " + value;
            case GT:
                return key + ">" + value;
            case GTE:
                return key + ">=" + value;
            case LT:
                return key + "<" + value;
            case LTE:
                return key + "<=" + value;
            case CONTAINS:
                return key + " contains " + value;
            case AND:
                return "AND" + children;
            case OR:
                return "OR" + children;
            default:
                return "Filter";
        }
    }

    /** 构造器：默认把所有条件用 AND 组合。 */
    public static final class Builder {

        private final List<Filter> conditions = new ArrayList<Filter>();

        private Builder() {
        }

        public Builder eq(String key, Object value) {
            return add(new Filter(Op.EQ, key, value, null));
        }

        public Builder ne(String key, Object value) {
            return add(new Filter(Op.NE, key, value, null));
        }

        public Builder in(String key, Collection<?> values) {
            return add(new Filter(Op.IN, key, values == null ? Collections.emptyList() : values, null));
        }

        public Builder gt(String key, Object value) {
            return add(new Filter(Op.GT, key, value, null));
        }

        public Builder gte(String key, Object value) {
            return add(new Filter(Op.GTE, key, value, null));
        }

        public Builder lt(String key, Object value) {
            return add(new Filter(Op.LT, key, value, null));
        }

        public Builder lte(String key, Object value) {
            return add(new Filter(Op.LTE, key, value, null));
        }

        public Builder contains(String key, Object value) {
            return add(new Filter(Op.CONTAINS, key, value, null));
        }

        /** 显式 AND 一组已有条件。 */
        public Builder and(Filter... filters) {
            return add(new Filter(Op.AND, null, null, Arrays.asList(filters)));
        }

        /** 显式 OR 一组已有条件。 */
        public Builder or(Filter... filters) {
            return add(new Filter(Op.OR, null, null, Arrays.asList(filters)));
        }

        public Builder add(Filter filter) {
            if (filter != null && filter != NO_OP) {
                conditions.add(filter);
            }
            return this;
        }

        public Filter build() {
            if (conditions.isEmpty()) {
                return NO_OP;
            }
            if (conditions.size() == 1) {
                return conditions.get(0);
            }
            return new Filter(Op.AND, null, null, conditions);
        }
    }

    /** 开启构造器。 */
    public static Builder builder() {
        return new Builder();
    }
}
