package com.xiongx9527.fw.plus.jdbc.dialect;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;

/**
 * 方言感知 SQL 静态检查（§6.38.4 / P0 防御）：仅“标记”方言不一致，不重写不可信 SQL，
 * 交由既有修复环（§6.17）让模型按提示与失败原因重出。
 *
 * <p>覆盖三类确定性错误：
 * <ol>
 *   <li>标识符引号风格错配：双引号方言（PostgreSQL / Oracle / DM / Kingbase / SQL Server /
 *       Redshift / Hive）里出现反引号标识符，属明确语法错误；</li>
 *   <li>分页关键字不支持：LIMIT 在 SQL Server / Oracle / DM / Kingbase 等方言不支持；</li>
 *   <li>TOP 仅 SQL Server 支持，其它方言应改用 LIMIT / OFFSET / FETCH。</li>
 * </ol>
 *
 * <p>方言未知（UNKNOWN）或无方言提示时全部跳过，不惩罚。引号检查仅单向标记反引号误用，
 * 不把双引号在反引号方言中的使用判为错误（MySQL 中双引号多为字符串字面量，易误报）。</p>
 */
public final class DialectSqlChecker {

    private static final Pattern BACKTICK_QUOTED = Pattern.compile("`[^`]*`");
    private static final Pattern LIMIT_CLAUSE = Pattern.compile("(?i)\\bLIMIT\\b");
    private static final Pattern TOP_CLAUSE = Pattern.compile("(?i)\\bTOP\\s+\\d");

    private DialectSqlChecker() {
    }

    /**
     * 返回方言不一致问题列表；空列表表示无问题。
     *
     * @param sql     SQL 文本
     * @param dialect 已解析方言；为 null 或 UNKNOWN 时直接返回空
     */
    public static List<String> check(String sql, SqlDialect dialect) {
        List<String> issues = new ArrayList<>();
        if (sql == null || sql.trim().isEmpty() || dialect == null || dialect == SqlDialect.UNKNOWN) {
            return issues;
        }
        if (usesDoubleQuote(dialect) && BACKTICK_QUOTED.matcher(sql).find()) {
            issues.add(I18nMessageResolver.resolveKey("askdata.dialect.backtick", dialect));
        }
        if (!supportsLimit(dialect) && LIMIT_CLAUSE.matcher(sql).find()) {
            issues.add(I18nMessageResolver.resolveKey("askdata.dialect.limit", dialect));
        }
        if (dialect != SqlDialect.SQLSERVER && TOP_CLAUSE.matcher(sql).find()) {
            issues.add(I18nMessageResolver.resolveKey("askdata.dialect.top", dialect));
        }
        return issues;
    }

    /** 该方言是否使用双引号作为标识符引用字符。 */
    public static boolean usesDoubleQuote(SqlDialect dialect) {
        switch (dialect) {
            case MYSQL:
            case SQLITE:
            case CLICKHOUSE:
            case DORIS:
            case STARROCKS:
                return false;
            default:
                return true;
        }
    }

    /** 该方言是否原生支持 LIMIT 分页。 */
    public static boolean supportsLimit(SqlDialect dialect) {
        switch (dialect) {
            case MYSQL:
            case SQLITE:
            case POSTGRESQL:
            case REDSHIFT:
            case HIVE:
            case CLICKHOUSE:
            case DORIS:
            case STARROCKS:
                return true;
            default:
                return false;
        }
    }
}
