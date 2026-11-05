package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import com.xiongx9527.fw.plus.ai.base.store.vector.Filter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把 base 的中性 {@link Filter} 条件树翻译成后端原生过滤表达式的参考实现。
 *
 * <p>base 在 {@code Filter} 上暴露了 {@code op()/key()/value()/children()} 只读结构后，
 * adapter 就可以把条件下推到库内执行，而不必一律「放大召回 + 客户端后过滤」。
 * 本类给出 Milvus 表达式与 Elasticsearch 查询 DSL 两种翻译样例；各 native 后端可按需接入。</p>
 *
 * <p><b>返回 {@code null} 表示无法完整翻译</b>（如 CONTAINS 无原生对应、AND 里有子节点不可翻译），
 * 调用方必须退回客户端 {@link Filter#matches(Map)} 后过滤，绝不静默放宽条件。
 * 字符串字面量会做转义，避免把用户输入直接拼进表达式。</p>
 */
public final class FilterExpressions {

    private FilterExpressions() {
    }

    /** 翻译成 Milvus {@code expr} 字符串。 */
    public static String toMilvus(Filter filter) {
        if (filter == null || filter.isNoOp()) {
            return null;
        }
        return milvus(filter);
    }

    /** 翻译成 Elasticsearch / OpenSearch 的查询 DSL 片段。 */
    public static Map<String, Object> toElasticsearch(Filter filter) {
        if (filter == null || filter.isNoOp()) {
            return null;
        }
        return elasticsearch(filter);
    }

    private static String milvus(Filter filter) {
        Filter.Op op = filter.op();
        if (op == Filter.Op.AND || op == Filter.Op.OR) {
            String join = op == Filter.Op.AND ? " and " : " or ";
            List<String> parts = new ArrayList<String>();
            for (Filter child : filter.children()) {
                String part = milvus(child);
                if (part == null) {
                    return null;
                }
                parts.add(part);
            }
            if (parts.isEmpty()) {
                return null;
            }
            if (parts.size() == 1) {
                return parts.get(0);
            }
            StringBuilder sb = new StringBuilder("(");
            for (int i = 0; i < parts.size(); i++) {
                if (i > 0) {
                    sb.append(join);
                }
                sb.append(parts.get(i));
            }
            return sb.append(")").toString();
        }
        if (op == Filter.Op.IN && filter.value() instanceof Iterable) {
            StringBuilder sb = new StringBuilder(filter.key()).append(" in [");
            boolean first = true;
            for (Object value : (Iterable<?>) filter.value()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(literal(value));
                first = false;
            }
            return sb.append("]").toString();
        }
        String operator;
        switch (op) {
            case EQ:
                operator = "==";
                break;
            case NE:
                operator = "!=";
                break;
            case GT:
                operator = ">";
                break;
            case GTE:
                operator = ">=";
                break;
            case LT:
                operator = "<";
                break;
            case LTE:
                operator = "<=";
                break;
            default:
                // CONTAINS 等无原生对应
                return null;
        }
        return filter.key() + " " + operator + " " + literal(filter.value());
    }

    private static Map<String, Object> elasticsearch(Filter filter) {
        Filter.Op op = filter.op();
        if (op == Filter.Op.AND || op == Filter.Op.OR) {
            String clause = op == Filter.Op.AND ? "must" : "should";
            List<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();
            for (Filter child : filter.children()) {
                Map<String, Object> part = elasticsearch(child);
                if (part == null) {
                    return null;
                }
                parts.add(part);
            }
            if (parts.isEmpty()) {
                return null;
            }
            Map<String, Object> bool = new LinkedHashMap<String, Object>();
            bool.put(clause, parts);
            Map<String, Object> root = new LinkedHashMap<String, Object>();
            root.put("bool", bool);
            return root;
        }
        if (op == Filter.Op.EQ || op == Filter.Op.NE) {
            Map<String, Object> term = new LinkedHashMap<String, Object>();
            term.put(filter.key(), filter.value());
            Map<String, Object> root = new LinkedHashMap<String, Object>();
            root.put("term", term);
            if (op == Filter.Op.NE) {
                Map<String, Object> bool = new LinkedHashMap<String, Object>();
                bool.put("must_not", root);
                Map<String, Object> wrapped = new LinkedHashMap<String, Object>();
                wrapped.put("bool", bool);
                return wrapped;
            }
            return root;
        }
        if (op == Filter.Op.GT || op == Filter.Op.GTE || op == Filter.Op.LT || op == Filter.Op.LTE) {
            String bound = op == Filter.Op.GT ? "gt" : op == Filter.Op.GTE ? "gte"
                    : op == Filter.Op.LT ? "lt" : "lte";
            Map<String, Object> bounds = new LinkedHashMap<String, Object>();
            bounds.put(bound, filter.value());
            Map<String, Object> field = new LinkedHashMap<String, Object>();
            field.put(filter.key(), bounds);
            Map<String, Object> root = new LinkedHashMap<String, Object>();
            root.put("range", field);
            return root;
        }
        // IN / CONTAINS 等交给客户端后过滤
        return null;
    }

    private static String literal(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "\"" + String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }
}
