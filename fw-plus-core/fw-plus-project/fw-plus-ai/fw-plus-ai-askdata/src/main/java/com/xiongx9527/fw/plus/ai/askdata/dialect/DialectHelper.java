package com.xiongx9527.fw.plus.ai.askdata.dialect;

/**
 * 方言相关的轻量帮助类：标识符引号与分页提示。
 * 只做通用规范化，不重复实现产品侧的 14 种方言细节（见 §6.38.4）。
 */
public final class DialectHelper {

    private static final String BACKTICK = String.valueOf('`');
    private static final String DOUBLE_QUOTE = String.valueOf('"');

    private DialectHelper() {
    }

    public static String identifierQuote(SqlDialect dialect) {
        switch (dialect) {
            case MYSQL:
            case SQLITE:
            case CLICKHOUSE:
            case DORIS:
            case STARROCKS:
                return BACKTICK;
            case SQLSERVER:
            case ORACLE:
            case DM:
            case KINGBASE:
            case POSTGRESQL:
            case REDSHIFT:
            case HIVE:
            default:
                return DOUBLE_QUOTE;
        }
    }

    public static String quote(SqlDialect dialect, String identifier) {
        if (identifier == null) {
            return null;
        }
        String q = identifierQuote(dialect);
        String cleaned = identifier.replace(q, "");
        return q + cleaned + q;
    }

    public static String paginationHint(SqlDialect dialect) {
        switch (dialect) {
            case MYSQL:
            case SQLITE:
            case DORIS:
            case STARROCKS:
            case CLICKHOUSE:
                return "MySQL 风格分页用 LIMIT n OFFSET m。";
            case POSTGRESQL:
            case REDSHIFT:
            case HIVE:
                return "PostgreSQL 风格分页用 LIMIT n OFFSET m。";
            case SQLSERVER:
                return "SQL Server 分页用 OFFSET m ROWS FETCH NEXT n ROWS ONLY。";
            case ORACLE:
            case DM:
            case KINGBASE:
                return "Oracle 风格分页用 OFFSET m ROWS FETCH NEXT n ROWS ONLY 或 ROWNUM 嵌套。";
            default:
                return "如需分页请使用该方言标准的 LIMIT/OFFSET 语法。";
        }
    }
}
