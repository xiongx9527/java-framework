package com.xiongx9527.fw.plus.jdbc.execution;

import java.util.List;

/**
 * SQL 执行端口（§6.38 后端无关）。由产品侧用真实数据源（如 JDBC DataSource）实现，
 * 框架只依赖此端口，不直接接触连接池 / 驱动。
 */
public interface SqlExecutorPort {
    QueryResult execute(String sql, ExecutionSettings settings);

    /**
     * 参数化执行：以占位符绑定入参，避免拼接 SQL 带来的注入风险（§6.38 参数化）。
     * 默认实现回退到 {@link #execute(String, ExecutionSettings)}（忽略参数），具体驱动实现应重写。
     *
     * @param sql    含 {@code ?} 占位符的 SQL
     * @param params 占位符入参（按位置对应）
     * @param settings 执行设置
     */
    default QueryResult execute(String sql, List<Object> params, ExecutionSettings settings) {
        return execute(sql, settings);
    }
}
