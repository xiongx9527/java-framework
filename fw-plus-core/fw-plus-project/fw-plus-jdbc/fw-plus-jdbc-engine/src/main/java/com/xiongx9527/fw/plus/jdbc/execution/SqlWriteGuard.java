package com.xiongx9527.fw.plus.jdbc.execution;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 写入型 DML 静态守卫（§6.38 只读防御下沉）。
 *
 * <p>原先只读只在 {@link JdbcSqlExecutor} 里调用 {@code Connection.setReadOnly(true)}，而该调用对多数驱动只是
 * 建议且被静默忽略（见 {@code JdbcSqlExecutor} 的 catch 块）。本工具把“拒绝写入”下沉到 SQL 文本层，
 * 让纯 JDBC 引擎在只读模式下也能真正拦住 INSERT/UPDATE/DELETE/DDL 等，而不依赖驱动实现；
 * askdata 的 {@code SqlValidator.containsWriteDml} 可复用同一正则，保证两层口径一致。</p>
 */
public final class SqlWriteGuard {

    /** 与 askdata 端 {@code SqlValidator.WRITE_DML} 保持同源，避免双份正则漂移。 */
    private static final Pattern WRITE_DML = Pattern.compile(
            "(?i)\\b(INSERT|UPDATE|DELETE|DROP|ALTER|TRUNCATE|CREATE|REPLACE|GRANT|REVOKE|MERGE|UPSERT)\\b");

    private SqlWriteGuard() {
    }

    /** 该 SQL 是否包含写入型语句（含 DDL）。空/空白 SQL 视为非写入。 */
    public static boolean isWriteDml(String sql) {
        return sql != null && !sql.trim().isEmpty() && WRITE_DML.matcher(sql).find();
    }

    /** 返回首个命中的写入关键字（大写），无则返回 null。 */
    public static String firstWriteKeyword(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return null;
        }
        Matcher m = WRITE_DML.matcher(sql);
        return m.find() ? m.group(1).toUpperCase(java.util.Locale.ROOT) : null;
    }
}
