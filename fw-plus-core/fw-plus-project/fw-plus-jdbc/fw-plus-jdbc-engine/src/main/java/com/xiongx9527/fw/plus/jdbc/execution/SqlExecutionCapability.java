package com.xiongx9527.fw.plus.jdbc.execution;

/** SQL 执行能力（§6.20 EXECUTE 节点）。后端无关：只读守卫 + 限行 / 超时由端口落实。 */
public interface SqlExecutionCapability {
    QueryResult execute(SqlExecInput input);
}
