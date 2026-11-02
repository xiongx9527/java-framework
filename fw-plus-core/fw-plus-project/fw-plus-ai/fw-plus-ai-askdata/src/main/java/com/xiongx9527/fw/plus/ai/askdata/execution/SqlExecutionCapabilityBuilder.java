package com.xiongx9527.fw.plus.ai.askdata.execution;

import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlValidator;

/** {@link SqlExecutionCapability} 的流式构建器。至少提供执行端口。 */
public class SqlExecutionCapabilityBuilder {

    private SqlExecutorPort executor;
    private SqlValidator validator;

    public static SqlExecutionCapabilityBuilder builder() {
        return new SqlExecutionCapabilityBuilder();
    }

    public SqlExecutionCapabilityBuilder executor(SqlExecutorPort v) {
        this.executor = v;
        return this;
    }

    public SqlExecutionCapabilityBuilder validator(SqlValidator v) {
        this.validator = v;
        return this;
    }

    public SqlExecutionCapability build() {
        if (executor == null) {
            throw new IllegalStateException("executor is required to build SqlExecutionCapability");
        }
        return new DefaultSqlExecutionCapability(executor, validator);
    }
}
