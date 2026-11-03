package com.xiongx9527.fw.plus.ai.askdata.execution;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import javax.sql.DataSource;

/**
 * 基于标准 JDBC 的 {@link SqlExecutorPort} 默认实现（§6.38）。
 * 接受任意 {@link DataSource}，落实只读 / 限行 / 超时，并把结果集收口为 {@link QueryResult}。
 * 不引入具体驱动，由产品侧提供 DataSource。
 */
public class JdbcSqlExecutor implements SqlExecutorPort {

    private final DataSource dataSource;

    public JdbcSqlExecutor(DataSource dataSource) {
        if (dataSource == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.datasource_required"));
        }
        this.dataSource = dataSource;
    }

    @Override
    public QueryResult execute(String sql, ExecutionSettings settings) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new SqlExecutionException(50005802, I18nMessageResolver.resolveKey("askdata.exec.sql_empty"));
        }
        ExecutionSettings s = settings == null ? ExecutionSettings.builder().build() : settings;
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (s.isReadOnly()) {
                try {
                    conn.setReadOnly(true);
                } catch (SQLException ignore) {
                    // 部分驱动不支持只读提示，交给执行本身去约束
                }
            }
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
                List<List<Object>> rows = readRows(rs, cols.size(), s.getMaxRows());
                boolean truncated = s.getMaxRows() > 0 && rows.size() >= s.getMaxRows();
                return QueryResult.builder()
                        .columns(cols)
                        .rows(rows)
                        .truncated(truncated)
                        .elapsedMillis(System.currentTimeMillis() - start)
                        .build();
            }
        } catch (SQLException e) {
            throw new SqlExecutionException(50005802, I18nMessageResolver.resolveKey("askdata.exec.failed"), e);
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

    private List<List<Object>> readRows(ResultSet rs, int colCount, int maxRows) throws SQLException {
        List<List<Object>> rows = new ArrayList<>();
        while (rs.next()) {
            List<Object> row = new ArrayList<>(colCount);
            for (int i = 1; i <= colCount; i++) {
                row.add(rs.getObject(i));
            }
            rows.add(row);
            if (maxRows > 0 && rows.size() >= maxRows) {
                break;
            }
        }
        return rows;
    }
}
