package com.xiongx9527.fw.plus.jdbc.execution;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import lombok.Getter;

/**
 * SQL 执行的显式入参（6.38.2 执行节点）。
 *
 * <p>执行设置（限行 / 超时 / 只读 / 自检 / 主键重复检查）由本对象承载，toSettings() 将其折叠为
 * ExecutionSettings 交给底层执行器。validate() 在入参进入执行前校验 SQL 非空。</p>
 */
@Getter
@lombok.Builder
public class SqlExecInput {

    /** 待执行的 SQL（必填）。 */
    private final String sql;

    /** SQL 方言提示（如 mysql / postgres），用于驱动/语法适配。 */
    private final String dialectHint;

    /** 最大返回行数，防止大结果集拖垮内存；<=0 表示不限制。 */
    @lombok.Builder.Default
    private final int maxRows = 1000;

    /** 查询超时（秒），<=0 表示不限制。 */
    @lombok.Builder.Default
    private final int queryTimeoutSeconds = 30;

    /** 是否只读执行；为 true 时执行器应拒绝写入型语句（见 DefaultSqlExecutionCapability）。 */
    @lombok.Builder.Default
    private final boolean readOnly = true;

    /** 执行后是否做结果自检 VERIFY（6.20.5 / P1 #18）：空结果 / 截断 / 主键重复。 */
    @lombok.Builder.Default
    private final boolean selfCheck = true;

    /** 用于主键重复自检的列下标（0-based）；为 null 时跳过重复检查。 */
    @lombok.Builder.Default
    private final Integer duplicateKeyColumn = null;

    /** 用于主键重复自检的列名（优先于 {@link #duplicateKeyColumn}）；为 null 时跳过。
     * 通常由调用方从 {@code SchemaContext.primaryKeyColumn(table)} 注入（见 6.38.2）。 */
    @lombok.Builder.Default
    private final String primaryKeyColumn = null;

    /** 执行前校验：SQL 不可为空。 */
    public void validate() {
        if (sql == null || sql.trim().isEmpty()) {
            throw new SqlExecutionException(50005802, I18nMessageResolver.resolveKey("askdata.exec.sql_required"));
        }
    }

    /** 折叠为底层执行器使用的 ExecutionSettings。 */
    public ExecutionSettings toSettings() {
        return ExecutionSettings.builder()
                .maxRows(maxRows)
                .queryTimeoutSeconds(queryTimeoutSeconds)
                .readOnly(readOnly)
                .selfCheck(selfCheck)
                .duplicateKeyColumn(duplicateKeyColumn)
                .primaryKeyColumn(primaryKeyColumn)
                .build();
    }
}
