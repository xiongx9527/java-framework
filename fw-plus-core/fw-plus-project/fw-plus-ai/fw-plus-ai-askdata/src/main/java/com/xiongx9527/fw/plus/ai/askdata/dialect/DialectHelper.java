package com.xiongx9527.fw.plus.ai.askdata.dialect;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;

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
        String suffix = suffixOf(dialect);
        String resolved = I18nMessageResolver.resolveKey("askdata.dialect.pagination." + suffix);
        if (resolved == null) {
            resolved = I18nMessageResolver.resolveKey("askdata.dialect.pagination.default");
        }
        return resolved;
    }

    private static String suffixOf(SqlDialect dialect) {
        if (dialect == null) {
            return "default";
        }
        switch (dialect) {
            case MYSQL:
            case SQLITE:
            case DORIS:
            case STARROCKS:
            case CLICKHOUSE:
                return "mysql";
            case POSTGRESQL:
            case REDSHIFT:
            case HIVE:
                return "postgresql";
            case SQLSERVER:
                return "sqlserver";
            case ORACLE:
            case DM:
            case KINGBASE:
                return "oracle";
            default:
                return "default";
        }
    }
}
