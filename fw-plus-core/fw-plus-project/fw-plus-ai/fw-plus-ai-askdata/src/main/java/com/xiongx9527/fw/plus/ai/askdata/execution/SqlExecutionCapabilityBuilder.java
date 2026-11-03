package com.xiongx9527.fw.plus.ai.askdata.execution;

import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlValidator;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;

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
            throw new IllegalStateException(I18nMessageResolver.resolveKey("askdata.contract.build_executor"));
        }
        return new DefaultSqlExecutionCapability(executor, validator);
    }
}
