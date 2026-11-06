package com.xiongx9527.fw.plus.jdbc.execution;

import java.sql.SQLException;

/**
 * 从 {@link SQLException} 推断 {@link SqlErrorCategory}（§6.38 错误分类）。
 *
 * <p>依据 SQLState 前缀与常见错误码做启发式判断；识别失败时回退 {@link SqlErrorCategory#UNKNOWN}，
 * 绝不抛异常，保证执行路径上的错误分类本身不会引发二次异常。</p>
 */
public final class SqlErrorClassifier {

    private SqlErrorClassifier() {
    }

    public static SqlErrorCategory classify(SQLException e) {
        if (e == null) {
            return SqlErrorCategory.UNKNOWN;
        }
        String state = e.getSQLState();
        if (state != null) {
            if (state.startsWith("08")) {
                return SqlErrorCategory.CONNECTION;
            }
            if ("HYT00".equals(state) || "S1T00".equals(state) || "57014".equals(state)) {
                return SqlErrorCategory.TIMEOUT;
            }
            if (state.startsWith("42") || state.startsWith("37") || "42000".equals(state)) {
                return SqlErrorCategory.SYNTAX;
            }
            if (state.startsWith("23")) {
                return SqlErrorCategory.CONSTRAINT;
            }
        }
        // SQLState 不足时，退而依据驱动消息关键字（常见驱动英文措辞）。
        String msg = (e.getMessage() == null ? "" : e.getMessage().toLowerCase(java.util.Locale.ROOT));
        if (msg.contains("timeout") || msg.contains("timed out") || msg.contains("ora-01013")) {
            return SqlErrorCategory.TIMEOUT;
        }
        if (msg.contains("connection") || msg.contains("communicat") || msg.contains("socket") || msg.contains("ora-03135")) {
            return SqlErrorCategory.CONNECTION;
        }
        if (msg.contains("syntax") || msg.contains("unknown column") || msg.contains("invalid identifier")
                || msg.contains("ora-009") || msg.contains("multiple") || msg.contains("ambiguous")) {
            return SqlErrorCategory.SYNTAX;
        }
        if (msg.contains("duplicate") || msg.contains("unique") || msg.contains("foreign key")
                || msg.contains("constraint") || msg.contains("not null") || msg.contains("ora-00001")
                || msg.contains("ora-0229")) {
            return SqlErrorCategory.CONSTRAINT;
        }
        return SqlErrorCategory.UNKNOWN;
    }
}
