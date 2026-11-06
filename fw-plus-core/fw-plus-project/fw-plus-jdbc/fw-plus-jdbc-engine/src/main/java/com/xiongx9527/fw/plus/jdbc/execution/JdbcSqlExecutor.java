package com.xiongx9527.fw.plus.jdbc.execution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.jdbc.dialect.SqlDialect;
import javax.sql.DataSource;

/**
 * 基于标准 JDBC 的 {@link SqlExecutorPort} 默认实现（§6.38）。
 * 接受任意 {@link DataSource}，落实只读（引擎级 DML 拒绝 + 连接只读提示）/ 限行 / 超时 / 方言分页重写，
 * 并把结果集收口为 {@link QueryResult}。支持执行前后拦截器与列级脱敏钩子。
 * 不引入具体驱动，由产品侧提供 DataSource。
 */
public class JdbcSqlExecutor implements SqlExecutorPort {

    private static final int CLOB_DISPLAY_LIMIT = 2000;

    private final DataSource dataSource;
    private final List<ExecutionInterceptor> interceptors;
    private final ColumnMasker columnMasker;

    public JdbcSqlExecutor(DataSource dataSource) {
        this(dataSource, Collections.<ExecutionInterceptor>emptyList(), null);
    }

    public JdbcSqlExecutor(DataSource dataSource, List<ExecutionInterceptor> interceptors, ColumnMasker columnMasker) {
        if (dataSource == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.datasource_required"));
        }
        this.dataSource = dataSource;
        this.interceptors = interceptors == null ? Collections.<ExecutionInterceptor>emptyList() : interceptors;
        this.columnMasker = columnMasker;
    }

    @Override
    public QueryResult execute(String sql, ExecutionSettings settings) {
        return execute(sql, null, settings);
    }

    @Override
    public QueryResult execute(String sql, List<Object> params, ExecutionSettings settings) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new SqlExecutionException(50005802, I18nMessageResolver.resolveKey("askdata.exec.sql_empty"));
        }
        ExecutionSettings s = settings == null ? ExecutionSettings.builder().build() : settings;
        long start = System.currentTimeMillis();
        try {
            // 引擎级只读防御：只读模式下直接拒绝写入型语句，不依赖驱动 setReadOnly 的静默实现。
            if (s.isReadOnly() && SqlWriteGuard.isWriteDml(sql)) {
                throw new SqlExecutionException(50005809,
                        I18nMessageResolver.resolveKey("askdata.exec.write_dml_blocked",
                                SqlWriteGuard.firstWriteKeyword(sql)));
            }
            for (ExecutionInterceptor it : interceptors) {
                it.beforeExecute(sql, s);
            }
            String effectiveSql = sql;
            if (s.isRewritePagination() && s.getDialectHint() != null) {
                SqlDialect dialect = SqlDialect.fromHint(s.getDialectHint());
                if (PaginationRewriter.needsRewrite(effectiveSql, dialect)) {
                    effectiveSql = PaginationRewriter.rewrite(effectiveSql, dialect);
                }
            }
            try (Connection conn = dataSource.getConnection();
                 PreparedStatement ps = conn.prepareStatement(effectiveSql)) {
                if (s.isReadOnly()) {
                    try {
                        conn.setReadOnly(true);
                    } catch (SQLException ignore) {
                        // 部分驱动不支持只读提示，交给 SQL 级守卫约束
                    }
                }
                bindParams(ps, params);
                if (s.getMaxRows() > 0) {
                    ps.setMaxRows(s.getMaxRows());
                }
                if (s.getQueryTimeoutSeconds() > 0) {
                    try {
                        ps.setQueryTimeout(s.getQueryTimeoutSeconds());
                    } catch (SQLException ignore) {
                        // 驱动不支持超时设置时忽略
                    }
                }
                try (ResultSet rs = ps.executeQuery()) {
                    List<ColumnMeta> cols = readColumns(rs.getMetaData());
                    List<List<Object>> rows = readRows(rs, cols, s.getMaxRows());
                    boolean truncated = s.getMaxRows() > 0 && rows.size() >= s.getMaxRows();
                    QueryResult result = QueryResult.builder()
                            .columns(cols)
                            .rows(rows)
                            .truncated(truncated)
                            .elapsedMillis(System.currentTimeMillis() - start)
                            .build();
                    for (ExecutionInterceptor it : interceptors) {
                        result = it.afterExecute(result, s);
                    }
                    return result;
                }
            }
        } catch (SqlExecutionException e) {
            throw e;
        } catch (SQLException e) {
            SqlErrorCategory category = SqlErrorClassifier.classify(e);
            int code = category == SqlErrorCategory.TIMEOUT ? 50005803
                    : category == SqlErrorCategory.CONNECTION ? 50005804
                    : category == SqlErrorCategory.CONSTRAINT ? 50005805
                    : category == SqlErrorCategory.SYNTAX ? 50005806 : 50005802;
            String key = category == SqlErrorCategory.TIMEOUT ? "askdata.exec.error_timeout"
                    : category == SqlErrorCategory.CONNECTION ? "askdata.exec.error_connection"
                    : category == SqlErrorCategory.CONSTRAINT ? "askdata.exec.error_constraint"
                    : category == SqlErrorCategory.SYNTAX ? "askdata.exec.error_syntax"
                    : "askdata.exec.error_unknown";
            String message = category == SqlErrorCategory.TIMEOUT
                    ? I18nMessageResolver.resolveKey(key, s.getQueryTimeoutSeconds())
                    : I18nMessageResolver.resolveKey(key);
            throw new SqlExecutionException(code, category, message, e);
        }
    }

    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        if (params == null || params.isEmpty()) {
            return;
        }
        for (int i = 0; i < params.size(); i++) {
            Object v = params.get(i);
            if (v == null) {
                ps.setNull(i + 1, Types.NULL);
            } else {
                ps.setObject(i + 1, v);
            }
        }
    }

    private List<ColumnMeta> readColumns(ResultSetMetaData md) throws SQLException {
        int n = md.getColumnCount();
        List<ColumnMeta> cols = new ArrayList<>(n);
        for (int i = 1; i <= n; i++) {
            cols.add(new ColumnMeta(md.getColumnLabel(i), md.getColumnTypeName(i), md.isNullable(i) != 0));
        }
        return cols;
    }

    private List<List<Object>> readRows(ResultSet rs, List<ColumnMeta> cols, int maxRows) throws SQLException {
        List<List<Object>> rows = new ArrayList<>();
        int colCount = cols.size();
        while (rs.next()) {
            List<Object> row = new ArrayList<>(colCount);
            for (int i = 1; i <= colCount; i++) {
                Object raw = rs.getObject(i);
                Object value = coerce(raw);
                if (columnMasker != null) {
                    String name = cols.get(i - 1).getName();
                    Object masked = columnMasker.mask(name, value);
                    value = masked;
                }
                row.add(value);
            }
            rows.add(row);
            if (maxRows > 0 && rows.size() >= maxRows) {
                break;
            }
        }
        return rows;
    }

    /** 大对象/数值类型归一：CLOB→截断文本，BLOB→占位，BigDecimal/BigInteger→字符串保留精度。 */
    private Object coerce(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof java.sql.Clob) {
            java.sql.Clob clob = (java.sql.Clob) raw;
            try {
                long len = clob.length();
                int read = (int) Math.min(len, CLOB_DISPLAY_LIMIT);
                String text = clob.getSubString(1, read);
                return read < len ? text + "…("
                        + I18nMessageResolver.resolveKey("askdata.exec.clob_truncated", CLOB_DISPLAY_LIMIT) + ")"
                        : text;
            } catch (SQLException ignore) {
                return "[clob]";
            }
        }
        if (raw instanceof java.sql.Blob) {
            try {
                return "[blob:" + ((java.sql.Blob) raw).length() + "B]";
            } catch (SQLException ignore) {
                return "[blob]";
            }
        }
        if (raw instanceof java.math.BigDecimal || raw instanceof java.math.BigInteger) {
            return raw.toString();
        }
        return raw;
    }
}
