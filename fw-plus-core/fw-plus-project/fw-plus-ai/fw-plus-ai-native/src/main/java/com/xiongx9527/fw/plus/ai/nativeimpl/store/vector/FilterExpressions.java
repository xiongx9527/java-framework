package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import com.xiongx9527.fw.plus.ai.base.store.vector.Filter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把 base 的中性 {@link Filter} 条件树翻译成各后端原生过滤表达式的参考实现。
 *
 * <p>base 在 {@code Filter} 上暴露了 {@code op()/key()/value()/children()} 只读结构后，
 * adapter 就可以把条件下推到库内执行，而不必一律「放大召回 + 客户端后过滤」。
 * 本类给出 Milvus / Elasticsearch / Qdrant / PgVector / Weaviate / Redis 六种翻译，
 * 各 native 后端在 search 时优先调用对应的 {@code toXxx}，把过滤交给向量库在库内完成。</p>
 *
 * <p><b>返回 {@code null} 表示无法完整翻译</b>（如 CONTAINS 无原生对应、组合节点含不可翻译子节点），
 * 调用方必须退回客户端 {@link Filter#matches(Map)} 后过滤，绝不静默放宽条件。
 * 字符串字面量都会做转义，避免把用户输入直接拼进表达式。</p>
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

    /** 翻译成 Qdrant filter JSON（{@code must/should/must_not} 结构）。 */
    public static Map<String, Object> toQdrant(Filter filter) {
        if (filter == null || filter.isNoOp()) {
            return null;
        }
        return qdrant(filter);
    }

    /** 翻译成 PgVector 的 SQL WHERE 片段（不含 {@code WHERE} 关键字，可拼接到已有条件后）。 */
    public static String toPgVector(Filter filter) {
        if (filter == null || filter.isNoOp()) {
            return null;
        }
        return pgVector(filter);
    }

    /** 翻译成 Weaviate GraphQL {@code where} 节点（{@code operator/operands/path/value*} 结构）。 */
    public static Map<String, Object> toWeaviate(Filter filter) {
        if (filter == null || filter.isNoOp()) {
            return null;
        }
        return weaviate(filter);
    }

    /** 翻译成 RediSearch 过滤表达式（@字段 语法，元数据以 {@code m_<key>} 字段承载）。 */
    public static String toRedis(Filter filter) {
        if (filter == null || filter.isNoOp()) {
            return null;
        }
        return redis(filter);
    }

    // ---------------------------------------------------------------- Milvus

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

    // ------------------------------------------------------------ Elasticsearch

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

    // ------------------------------------------------------------------ Qdrant

    private static Map<String, Object> qdrant(Filter filter) {
        Filter.Op op = filter.op();
        if (op == Filter.Op.AND || op == Filter.Op.OR) {
            String clause = op == Filter.Op.AND ? "must" : "should";
            List<Map<String, Object>> parts = new ArrayList<Map<String, Object>>();
            for (Filter child : filter.children()) {
                Map<String, Object> part = qdrant(child);
                if (part == null) {
                    return null;
                }
                parts.add(part);
            }
            if (parts.isEmpty()) {
                return null;
            }
            Map<String, Object> root = new LinkedHashMap<String, Object>();
            root.put(clause, parts);
            return root;
        }
        if (filter.key() == null) {
            return null;
        }
        if (op == Filter.Op.CONTAINS) {
            return null;
        }
        if (op == Filter.Op.NE) {
            Map<String, Object> leaf = qdrantLeaf(filter);
            Map<String, Object> root = new LinkedHashMap<String, Object>();
            root.put("must_not", Collections.singletonList(leaf));
            return root;
        }
        return qdrantLeaf(filter);
    }

    private static Map<String, Object> qdrantLeaf(Filter filter) {
        Filter.Op op = filter.op();
        Map<String, Object> cond = new LinkedHashMap<String, Object>();
        cond.put("key", filter.key());
        if (op == Filter.Op.EQ || op == Filter.Op.IN) {
            cond.put("match", map("value", filter.value()));
        } else {
            Map<String, Object> range = new LinkedHashMap<String, Object>();
            range.put(op.name().toLowerCase(), filter.value());
            cond.put("range", range);
        }
        return cond;
    }

    // ----------------------------------------------------------------- PgVector

    private static String pgVector(Filter filter) {
        Filter.Op op = filter.op();
        if (op == Filter.Op.AND || op == Filter.Op.OR) {
            String join = op == Filter.Op.AND ? " AND " : " OR ";
            List<String> parts = new ArrayList<String>();
            for (Filter child : filter.children()) {
                String part = pgVector(child);
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
        if (filter.key() == null) {
            return null;
        }
        String key = sqlIdent(filter.key());
        switch (op) {
            case EQ:
                return "metadata @> '" + jsonScalar(filter.key(), filter.value()) + "'";
            case NE:
                return "NOT (metadata @> '" + jsonScalar(filter.key(), filter.value()) + "')";
            case IN:
                return "(metadata->>'" + key + "') = ANY (ARRAY[" + pgInList(filter.value()) + "])";
            case GT:
                return "(metadata->>'" + key + "')::numeric > " + num(filter.value());
            case GTE:
                return "(metadata->>'" + key + "')::numeric >= " + num(filter.value());
            case LT:
                return "(metadata->>'" + key + "')::numeric < " + num(filter.value());
            case LTE:
                return "(metadata->>'" + key + "')::numeric <= " + num(filter.value());
            case CONTAINS:
                return "(metadata->>'" + key + "') LIKE '%" + sqlLike(filter.value()) + "%'";
            default:
                return null;
        }
    }

    private static String jsonScalar(String key, Object value) {
        StringBuilder sb = new StringBuilder("{");
        sb.append(jsonString(key)).append(": ");
        if (value instanceof Number || value instanceof Boolean) {
            sb.append(String.valueOf(value));
        } else {
            sb.append(jsonString(String.valueOf(value)));
        }
        return sb.append("}").toString();
    }

    private static String pgInList(Object value) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (value instanceof Collection) {
            for (Object o : (Collection<?>) value) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(sqlString(String.valueOf(o)));
                first = false;
            }
        } else if (value != null) {
            sb.append(sqlString(String.valueOf(value)));
        }
        return sb.toString();
    }

    // ---------------------------------------------------------------- Weaviate

    private static Map<String, Object> weaviate(Filter filter) {
        Filter.Op op = filter.op();
        if (op == Filter.Op.AND || op == Filter.Op.OR) {
            List<Map<String, Object>> operands = new ArrayList<Map<String, Object>>();
            for (Filter child : filter.children()) {
                Map<String, Object> part = weaviate(child);
                if (part == null) {
                    return null;
                }
                operands.add(part);
            }
            if (operands.isEmpty()) {
                return null;
            }
            Map<String, Object> node = new LinkedHashMap<String, Object>();
            node.put("operator", op == Filter.Op.AND ? "And" : "Or");
            node.put("operands", operands);
            return node;
        }
        if (filter.key() == null) {
            return null;
        }
        Map<String, Object> node = new LinkedHashMap<String, Object>();
        node.put("path", Collections.singletonList(filter.key()));
        switch (op) {
            case EQ:
                node.put("operator", "Equal");
                putWeaviateValue(node, filter.value());
                break;
            case NE:
                node.put("operator", "NotEqual");
                putWeaviateValue(node, filter.value());
                break;
            case IN:
                node.put("operator", "ContainsAny");
                node.put("valueText", toTextList(filter.value()));
                break;
            case GT:
                node.put("operator", "GreaterThan");
                node.put("valueNumber", num(filter.value()));
                break;
            case GTE:
                node.put("operator", "GreaterThanEqual");
                node.put("valueNumber", num(filter.value()));
                break;
            case LT:
                node.put("operator", "LessThan");
                node.put("valueNumber", num(filter.value()));
                break;
            case LTE:
                node.put("operator", "LessThanEqual");
                node.put("valueNumber", num(filter.value()));
                break;
            case CONTAINS:
                node.put("operator", "Like");
                node.put("valueText", "*" + String.valueOf(filter.value()) + "*");
                break;
            default:
                return null;
        }
        return node;
    }

    private static void putWeaviateValue(Map<String, Object> node, Object value) {
        if (value instanceof Boolean) {
            node.put("valueBoolean", value);
        } else if (value instanceof Number) {
            node.put("valueNumber", value);
        } else {
            node.put("valueString", String.valueOf(value));
        }
    }

    private static List<String> toTextList(Object value) {
        List<String> out = new ArrayList<String>();
        if (value instanceof Collection) {
            for (Object o : (Collection<?>) value) {
                out.add(String.valueOf(o));
            }
        } else if (value != null) {
            out.add(String.valueOf(value));
        }
        return out;
    }

    // ------------------------------------------------------------------- Redis

    private static String redis(Filter filter) {
        Filter.Op op = filter.op();
        if (op == Filter.Op.AND || op == Filter.Op.OR) {
            String join = op == Filter.Op.AND ? " " : " | ";
            List<String> parts = new ArrayList<String>();
            for (Filter child : filter.children()) {
                String part = redis(child);
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
            return "(" + joinStrings(join, parts) + ")";
        }
        if (filter.key() == null) {
            return null;
        }
        String field = "@m_" + filter.key();
        switch (op) {
            case EQ:
                return field + ":(" + redisTerm(filter.value()) + ")";
            case NE:
                return "-" + field + ":(" + redisTerm(filter.value()) + ")";
            case IN:
                return field + ":(" + redisInList(filter.value()) + ")";
            // 范围算子：Redis 端元数据以 TEXT 字段存储，数值按字典序比较会出错，
            // 故交给客户端 matches() 后过滤（返回 null 触发回退）
            case GT:
            case GTE:
            case LT:
            case LTE:
                return null;
            case CONTAINS:
                return field + ":(*" + redisEscape(String.valueOf(filter.value())) + "*)";
            default:
                return null;
        }
    }

    private static String joinStrings(String join, List<String> parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) {
            if (i > 0) {
                sb.append(join);
            }
            sb.append(parts.get(i));
        }
        return sb.toString();
    }

    private static String redisInList(Object value) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if (value instanceof Collection) {
            for (Object o : (Collection<?>) value) {
                if (!first) {
                    sb.append("|");
                }
                sb.append(redisTerm(o));
                first = false;
            }
        } else if (value != null) {
            sb.append(redisTerm(value));
        }
        return sb.toString();
    }

    private static String redisTerm(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "\"" + redisEscape(String.valueOf(value)) + "\"";
    }

    private static String redisEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace(" ", "\\ ");
    }

    // ------------------------------------------------------------------ helpers

    private static Map<String, Object> map(String key, Object value) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put(key, value);
        return m;
    }

    private static Number num(Object value) {
        if (value instanceof Number) {
            return (Number) value;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String literal(Object value) {
        if (value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return "\"" + String.valueOf(value)
                .replace("\\", "\\\\")
                .replace("\"", "\\\"") + "\"";
    }

    private static String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static String sqlIdent(String key) {
        return key.replace("'", "''");
    }

    private static String sqlString(String value) {
        return "'" + value.replace("'", "''") + "'";
    }

    private static String sqlLike(Object value) {
        return String.valueOf(value).replace("'", "''").replace("%", "\\%").replace("_", "\\_");
    }
}
