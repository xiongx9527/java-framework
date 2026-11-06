package com.xiongx9527.fw.plus.jdbc.execution;

import com.xiongx9527.fw.plus.jdbc.dialect.DialectSqlChecker;
import com.xiongx9527.fw.plus.jdbc.dialect.SqlDialect;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 方言感知的分页重写（§6.38 分页翻译，默认关闭，需 {@code ExecutionSettings.rewritePagination=true} 启用）。
 *
 * <p>把 {@code LIMIT n [OFFSET m]} 翻成目标方言的分页语法：
 * <ul>
 *   <li>原生支持 LIMIT 的方言（MySQL/PostgreSQL/...）原样返回；</li>
 *   <li>SQL Server / Oracle / DM / Kingbase 翻成 {@code OFFSET m ROWS FETCH NEXT n ROWS ONLY}；</li>
 * </ul>
 * </p>
 *
 * <p><b>注意</b>：SQL Server / Oracle 的 OFFSET FETCH 要求语句含 {@code ORDER BY}；本重写不做语法注入，
 * 调用方（如 askdata 的 top-N 生成）应已自行产出 {@code ORDER BY}。因此默认关闭，
 * 仅当明确开启且方言需要时才转换，避免对原生 LIMIT 方言产生任何影响。</p>
 */
public final class PaginationRewriter {

    private static final Pattern LIMIT_OFFSET = Pattern.compile(
            "(?i)\\bLIMIT\\s+(\\d+)\\s*(?:OFFSET\\s+(\\d+))?");

    private PaginationRewriter() {
    }

    /** 是否需要重写（仅不支持原生 LIMIT 的方言且含有 LIMIT 子句时）。 */
    public static boolean needsRewrite(String sql, SqlDialect dialect) {
        if (sql == null || dialect == null || dialect == SqlDialect.UNKNOWN) {
            return false;
        }
        return !DialectSqlChecker.supportsLimit(dialect) && LIMIT_OFFSET.matcher(sql).find();
    }

    /**
     * 重写分页子句。原生 LIMIT 方言或无需重写时返回原 SQL。
     *
     * @param sql     SQL 文本
     * @param dialect 目标方言
     */
    public static String rewrite(String sql, SqlDialect dialect) {
        if (!needsRewrite(sql, dialect)) {
            return sql;
        }
        Matcher m = LIMIT_OFFSET.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String limit = m.group(1);
            String offset = m.group(2);
            String replacement = offset == null
                    ? "OFFSET 0 ROWS FETCH NEXT " + limit + " ROWS ONLY"
                    : "OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }
}
