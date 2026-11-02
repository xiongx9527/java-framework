package com.xiongx9527.fw.plus.ai.askdata.execution;

import lombok.Builder;
import lombok.Getter;

/** SQL 执行的显式输入（全部显式，无隐式全局状态，见 §6.38.2）。 */
@Getter
@Builder
public class SqlExecInput {
    /** 待执行的只读 SQL。 */
    private final String sql;

    /** 方言提示字符串（产品侧 DatasourceType.sqlDialectHint() 映射），可选，仅用于日志 / 校验。 */
    private final String dialectHint;

    /** 最大返回行数。 */
    @Builder.Default
    private final int maxRows = 1000;

    /** 查询超时（秒）。 */
    @Builder.Default
    private final int queryTimeoutSeconds = 30;

    /** 是否只读。 */
    @Builder.Default
    private final boolean readOnly = true;

    public void validate() {
        if (sql == null || sql.trim().isEmpty()) {
            throw new SqlExecutionException("sql 不可为空");
        }
    }

    public ExecutionSettings toSettings() {
        return ExecutionSettings.builder()
                .maxRows(maxRows)
                .queryTimeoutSeconds(queryTimeoutSeconds)
                .readOnly(readOnly)
                .build();
    }
}
