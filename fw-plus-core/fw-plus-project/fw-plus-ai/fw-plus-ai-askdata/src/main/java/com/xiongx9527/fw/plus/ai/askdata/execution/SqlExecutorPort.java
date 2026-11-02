package com.xiongx9527.fw.plus.ai.askdata.execution;

/**
 * SQL 执行端口（§6.38 后端无关）。由产品侧用真实数据源（如 JDBC DataSource）实现，
 * 框架只依赖此端口，不直接接触连接池 / 驱动。
 */
public interface SqlExecutorPort {
    QueryResult execute(String sql, ExecutionSettings settings);
}
