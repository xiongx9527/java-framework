package com.xiongx9527.fw.plus.ai.askdata.execution;

import com.xiongx9527.fw.plus.ai.askdata.postprocess.SqlValidator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认 {@link SqlExecutionCapability} 实现（§6.38）：输入自检 →（只读时）拒绝写入型 DML →
 * 委托端口执行 →（可选）结果自检 VERIFY。三个后端共用同一份守卫逻辑，只有执行端口不同。
 */
public class DefaultSqlExecutionCapability implements SqlExecutionCapability {

    private static final Logger log = LoggerFactory.getLogger(DefaultSqlExecutionCapability.class);

    private final SqlExecutorPort executor;
    private final SqlValidator validator;
    private final ResultVerifier verifier;

    public DefaultSqlExecutionCapability(SqlExecutorPort executor) {
        this(executor, new SqlValidator(), new DefaultResultVerifier());
    }

    public DefaultSqlExecutionCapability(SqlExecutorPort executor, SqlValidator validator) {
        this(executor, validator, new DefaultResultVerifier());
    }

    public DefaultSqlExecutionCapability(SqlExecutorPort executor, SqlValidator validator, ResultVerifier verifier) {
        if (executor == null) {
            throw new IllegalArgumentException("executor is required");
        }
        this.executor = executor;
        this.validator = validator == null ? new SqlValidator() : validator;
        this.verifier = verifier == null ? new DefaultResultVerifier() : verifier;
    }

    @Override
    public QueryResult execute(SqlExecInput input) {
        if (input == null) {
            throw new SqlExecutionException("input is null");
        }
        input.validate();
        ExecutionSettings settings = input.toSettings();
        if (settings.isReadOnly() && validator.containsWriteDml(input.getSql())) {
            throw new SqlExecutionException("只读执行模式禁止写入型语句: " + input.getSql());
        }
        long start = System.currentTimeMillis();
        QueryResult result = executor.execute(input.getSql(), settings);
        long elapsed = System.currentTimeMillis() - start;
        if (settings.isSelfCheck() && verifier != null) {
            List<String> issues = verifier.verify(result, settings);
            if (!issues.isEmpty()) {
                result = result.withWarnings(issues);
            }
        }
        // 全链路日志：只记行数与耗时，不记行数据 / SQL 原文，避免泄露业务数据。
        log.info("sql-execution readOnly={} rows={} elapsedMs={} warnings={}",
                settings.isReadOnly(), result.rowCount(), elapsed, result.getWarnings().size());
        return result;
    }
}

