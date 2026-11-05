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
 * <p>两种消费方式，adapter 按后端能力选择：</p>
 * <ul>
 *   <li><b>下推</b>（推荐）：通过 {@link #op()} / {@link #key()} / {@link #value()} /
 *       {@link #children()} 递归读取条件树，翻译成后端原生 filter 语法，让过滤在库内完成；</li>
 *   <li><b>后过滤</b>：召回候选后对每条调 {@link #matches(Map)}。实现简单，但会放大召回量、
 *       且过滤后可能不足 topK，仅用于不支持原生过滤的后端。</li>
 * </ul>
 *
 * <p>示例：{@code Filter.builder().eq("dsId", 7).gt("score", 0.5).build()}
 * 表示 {@code dsId == 7 AND score > 0.5}。</p>
 */
public final class Filter {

    /** 永真的空过滤（不过滤）。 */
    public static final Filter NO_OP = new Filter(Op.AND, null, null, Collections.<Filter>emptyList());

    /** 条件操作符；adapter 据此把条件树翻译成后端原生 filter 语法。 */
    public enum Op {
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

    /** 条件操作符；AND / OR 表示组合节点，其余为叶子条件。 */
    public Op op() {
        return op;
    }

    /** 元数据键；AND / OR 组合节点为 {@code null}。 */
    public String key() {
        return key;
    }

    /** 期望值；AND / OR 组合节点为 {@code null}，IN 条件为 {@code Collection}。 */
    public Object value() {
        return value;
    }

    /** 子条件；叶子条件为空列表，返回的列表不可修改。 */
    public List<Filter> children() {
        return children;
    }

    /** 是否为永真的空过滤（无任何条件）；adapter 可据此跳过 filter 下推。 */
    public boolean isNoOp() {
        return op == Op.AND && children.isEmpty();
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
