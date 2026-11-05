package com.xiongx9527.fw.plus.jdbc.dialect;

import java.util.Locale;

/**
 * 数据库方言。产品侧的 DatasourceType.sqlDialectHint() 会映射成字符串传入本模块，
 * 框架不依赖产品枚举（见 §6.38.4）。
 */
public enum SqlDialect {
    MYSQL,
    POSTGRESQL,
    SQLSERVER,
    SQLITE,
    CLICKHOUSE,
    DORIS,
    STARROCKS,
    ORACLE,
    DM,
    KINGBASE,
    REDSHIFT,
    ELASTICSEARCH,
    HIVE,
    EXCEL,
    UNKNOWN;

    public static SqlDialect fromHint(String hint) {
        if (hint == null || hint.trim().isEmpty()) {
            return UNKNOWN;
        }
        String h = hint.trim().toLowerCase(Locale.ROOT);
        // 优先匹配产品 DatasourceType.sqlDialectHint() 的直接码（mysql/pg/sqlServer/sqlite/ck/doris/starrocks/oracle/dm/kingbase/redshift/excel）
        if (h.contains("mysql")) return MYSQL;
        if (h.equals("pg") || h.contains("postgre")) return POSTGRESQL;
        if (h.contains("sqlserver") || h.contains("mssql")) return SQLSERVER;
        if (h.contains("sqlite")) return SQLITE;
        if (h.equals("ck") || h.contains("clickhouse")) return CLICKHOUSE;
        if (h.contains("doris")) return DORIS;
        if (h.contains("starrocks")) return STARROCKS;
        if (h.contains("oracle")) return ORACLE;
        if (h.equals("dm") || h.contains("dameng")) return DM;
        if (h.contains("kingbase")) return KINGBASE;
        if (h.contains("redshift")) return REDSHIFT;
        if (h.contains("elastic") || h.equals("es") || h.contains(" es ")) return ELASTICSEARCH;
        if (h.contains("hive")) return HIVE;
        if (h.contains("excel")) return EXCEL;
        return UNKNOWN;
    }
}
