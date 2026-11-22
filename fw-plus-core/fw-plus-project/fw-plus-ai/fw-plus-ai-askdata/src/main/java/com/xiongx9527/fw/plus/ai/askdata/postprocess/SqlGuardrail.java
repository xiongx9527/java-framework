package com.xiongx9527.fw.plus.ai.askdata.postprocess;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcParameter;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.update.Update;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SQL 确定性护栏（通用后处理能力）：在不调用 LLM 的前提下，对小模型生成的 SQL 做规则化修复。
 *
 * <p>目标：让小模型也能稳定产出可执行 SQL。当语义校验给出精确错误时，用元数据里的合法列做
 * 确定性修正（最相似列名替换 / 补 GROUP BY），修不了返回 {@code null} 交回 LLM 回喂。
 * 只处理可确定性判断的错误类型，不猜语义。</p>
 *
 * <p><b>线程安全</b>：全静态无状态，可被业务侧任意并发调用。</p>
 *
 * <p><b>兼容性</b>：主线 Java 8 + JSqlParser 4.6。列名替换走 {@code Expression#accept(visitor)}
 * （JSqlParser 4.x 中 {@code Statement.accept} 只接受 {@code StatementVisitor}，不能传
 * {@code ExpressionVisitorAdapter}）；去尾空白用正则而非 Java 11 的 {@code String.trimEnd()}。</p>
 */
public final class SqlGuardrail {

    private static final Logger log = LoggerFactory.getLogger(SqlGuardrail.class);

    /** 校验错误文案标记（与业务侧语义校验器约定，保持与上游一致）。 */
    public static final String MARKER_QUALIFIED_COLUMN = "SQL 引用了表 ";
    public static final String MARKER_QUALIFIED_COLUMN_MID = " 中不存在的列：";
    public static final String MARKER_BARE_COLUMN = "SQL 引用了不存在的列：";
    public static final String MARKER_GROUP_BY = "聚合查询缺少 GROUP BY 维度：";

    private SqlGuardrail() {
    }

    /**
     * 尝试确定性修复。
     *
     * @param sql    原始 SQL
     * @param error  语义校验返回的错误文案
     * @param schema 合法表结构（表名 → 列名 → 类型）
     * @return 修复后的 SQL；无法安全修复时返回 {@code null}
     */
    public static String repair(String sql, String error, Map<String, Map<String, String>> schema) {
        if (sql == null || error == null || schema == null || schema.isEmpty()) {
            return null;
        }
        String fixed = repairMissingColumn(sql, error, schema);
        if (fixed == null) {
            fixed = repairMissingGroupBy(sql, error);
        }
        if (fixed != null && !fixed.equals(sql)) {
            log.info("SQL 确定性护栏修复生效: {} -> {}", error, sqlOneLine(fixed));
        }
        return fixed;
    }

    /** 修复「表 X 中不存在的列：Y」：在合法列中找最相似列名替换。 */
    public static String repairMissingColumn(String sql, String error,
                                             Map<String, Map<String, String>> schema) {
        int at = error.indexOf(MARKER_QUALIFIED_COLUMN);
        if (at < 0) {
            return repairBareColumn(sql, error, schema);
        }
        String rest = error.substring(at + MARKER_QUALIFIED_COLUMN.length());
        int mid = rest.indexOf(MARKER_QUALIFIED_COLUMN_MID);
        if (mid < 0) {
            return null;
        }
        String table = rest.substring(0, mid).toLowerCase(Locale.ROOT);
        String badColumn = rest.substring(mid + MARKER_QUALIFIED_COLUMN_MID.length()).trim();
        Map<String, String> columns = findTable(schema, table);
        if (columns == null || columns.isEmpty() || badColumn.isEmpty()) {
            return null;
        }
        String best = closest(badColumn, columns.keySet());
        return best == null ? null : replaceColumn(sql, table, badColumn, best);
    }

    /** 无表限定的列错误：跨所有表找最相似列。 */
    public static String repairBareColumn(String sql, String error,
                                          Map<String, Map<String, String>> schema) {
        int at = error.indexOf(MARKER_BARE_COLUMN);
        if (at < 0) {
            return null;
        }
        String badColumn = error.substring(at + MARKER_BARE_COLUMN.length()).trim();
        List<String> all = new ArrayList<String>();
        for (Map<String, String> cols : schema.values()) {
            all.addAll(cols.keySet());
        }
        String best = closest(badColumn, all);
        return best == null ? null : replaceColumn(sql, null, badColumn, best);
    }

    /** 修复「聚合查询缺少 GROUP BY 维度：X」：把该列加进 GROUP BY。 */
    public static String repairMissingGroupBy(String sql, String error) {
        int at = error.indexOf(MARKER_GROUP_BY);
        if (at < 0) {
            return null;
        }
        String column = error.substring(at + MARKER_GROUP_BY.length()).trim();
        if (column.isEmpty()) {
            return null;
        }
        try {
            Statement st = CCJSqlParserUtil.parse(sql);
            if (!(st instanceof Select)) {
                return null;
            }
            String lower = sql.toLowerCase(Locale.ROOT);
            int groupPos = lower.lastIndexOf("group by");
            if (groupPos >= 0) {
                int insertAt = findClauseEnd(lower, groupPos + "group by".length());
                String head = trimRight(sql.substring(0, insertAt));
                String tail = sql.substring(insertAt);
                return head + ", " + column + " " + tail;
            }
            int insertAt = sql.length();
            int orderPos = lower.lastIndexOf("order by");
            int limitPos = lower.lastIndexOf("limit");
            if (orderPos >= 0) {
                insertAt = orderPos;
            } else if (limitPos >= 0) {
                insertAt = limitPos;
            }
            String head = trimRight(sql.substring(0, insertAt));
            String tail = sql.substring(insertAt);
            return head + " GROUP BY " + column + " " + tail;
        } catch (JSQLParserException e) {
            return null;
        }
    }

    /**
     * 按 AST 精确替换列名节点（带表限定时同时匹配表名，{@code table==null} 表示不限定）。
     * 用 {@code Expression#accept(visitor)} 逐个表达式访问，兼容 JSqlParser 4.6。
     */
    public static String replaceColumn(String sql, String table, String bad, String best) {
        if (sql == null || bad == null || best == null) {
            return null;
        }
        try {
            Statement st = CCJSqlParserUtil.parse(sql);
            final boolean[] changed = {false};
            ExpressionVisitorAdapter visitor = new ExpressionVisitorAdapter() {
                @Override
                public void visit(Column column) {
                    String cn = column.getColumnName();
                    if (cn != null && cn.equalsIgnoreCase(bad)) {
                        String q = column.getTable() == null ? null : column.getTable().getName();
                        if (table == null || q == null || q.equalsIgnoreCase(table)) {
                            column.setColumnName(best);
                            changed[0] = true;
                        }
                    }
                }
            };
            visitExpressions(st, visitor);
            return changed[0] ? st.toString() : null;
        } catch (JSQLParserException e) {
            return null;
        }
    }

    /** 把 visitor 应用到语句中的所有列表达式（SELECT 列表 / WHERE / GROUP BY / HAVING / ORDER BY / INSERT / UPDATE）。 */
    private static void visitExpressions(Statement st, ExpressionVisitorAdapter visitor) {
        if (st instanceof Select) {
            PlainSelect plain = plainOf((Select) st);
            if (plain == null) {
                return;
            }
            if (plain.getSelectItems() != null) {
                for (SelectItem item : plain.getSelectItems()) {
                    if (item instanceof SelectExpressionItem) {
                        acceptQuietly(((SelectExpressionItem) item).getExpression(), visitor);
                    }
                }
            }
            acceptQuietly(plain.getWhere(), visitor);
            if (plain.getGroupBy() != null && plain.getGroupBy().getGroupByExpressions() != null) {
                for (Expression e : plain.getGroupBy().getGroupByExpressions()) {
                    acceptQuietly(e, visitor);
                }
            }
            acceptQuietly(plain.getHaving(), visitor);
            if (plain.getOrderByElements() != null) {
                for (net.sf.jsqlparser.statement.select.OrderByElement e : plain.getOrderByElements()) {
                    acceptQuietly(e.getExpression(), visitor);
                }
            }
        } else if (st instanceof Insert) {
            Insert ins = (Insert) st;
            if (ins.getSelect() != null) {
                visitExpressions(ins.getSelect(), visitor);
            }
            acceptExpressionList(ins.getItemsList() instanceof ExpressionList
                    ? ((ExpressionList) ins.getItemsList()).getExpressions() : null, visitor);
        } else if (st instanceof Update) {
            Update upd = (Update) st;
            acceptExpressionList(upd.getExpressions(), visitor);
            acceptQuietly(upd.getWhere(), visitor);
        }
    }

    private static void acceptExpressionList(List<Expression> expressions, ExpressionVisitorAdapter visitor) {
        if (expressions == null) {
            return;
        }
        for (Expression e : expressions) {
            acceptQuietly(e, visitor);
        }
    }

    private static void acceptQuietly(Expression expression, ExpressionVisitorAdapter visitor) {
        if (expression != null) {
            expression.accept(visitor);
        }
    }

    private static PlainSelect plainOf(Select select) {
        if (select.getSelectBody() instanceof PlainSelect) {
            return (PlainSelect) select.getSelectBody();
        }
        return null;
    }

    /** 找 GROUP BY 子句结束位置（到下一个关键字前）。 */
    public static int findClauseEnd(String lower, int from) {
        int end = lower.length();
        String[] stops = {" order by ", " limit ", " having ", " union "};
        for (String stop : stops) {
            int p = lower.indexOf(stop, from);
            if (p >= 0 && p < end) {
                end = p;
            }
        }
        return end;
    }

    public static Map<String, String> findTable(Map<String, Map<String, String>> schema, String table) {
        for (Map.Entry<String, Map<String, String>> e : schema.entrySet()) {
            if (e.getKey().toLowerCase(Locale.ROOT).equals(table)) {
                return e.getValue();
            }
        }
        return null;
    }

    /** 编辑距离找最相似列名；过长差异不修复（避免错误替换）。 */
    public static String closest(String bad, Iterable<String> candidates) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        String badLower = bad.toLowerCase(Locale.ROOT);
        for (String c : candidates) {
            int d = levenshtein(badLower, c.toLowerCase(Locale.ROOT));
            if (d < bestDist) {
                bestDist = d;
                best = c;
            }
        }
        int maxAllowed = Math.max(2, bad.length() / 2);
        return bestDist <= maxAllowed ? best : null;
    }

    public static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return prev[b.length()];
    }

    /** Java 8 兼容的去尾部空白（替代 Java 11 的 String.trimEnd()）。 */
    private static String trimRight(String s) {
        return s.replaceAll("\\s+$", "");
    }

    private static String sqlOneLine(String sql) {
        return sql.replaceAll("\\s+", " ");
    }
}
