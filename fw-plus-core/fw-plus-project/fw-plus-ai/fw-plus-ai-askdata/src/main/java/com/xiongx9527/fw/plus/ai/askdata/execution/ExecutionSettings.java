package com.xiongx9527.fw.plus.ai.askdata.execution;

import lombok.Builder;
import lombok.Getter;

/** SQL 执行的轻量设置（§6.38 / §6.20 EXECUTE 节点）。只读、限行、超时、自检由本对象承载。 */
@Getter
@Builder
public class ExecutionSettings {

    /** 最大返回行数，防止大结果集拖垮内存；<=0 表示不限制。 */
    @Builder.Default
    private final int maxRows = 1000;

    /** 查询超时（秒），<=0 表示不限制。 */
    @Builder.Default
    private final int queryTimeoutSeconds = 30;

    /** 是否只读执行；为 true 时执行器应拒绝写入型语句（见 DefaultSqlExecutionCapability）。 */
    @Builder.Default
    private final boolean readOnly = true;

    /** 执行后是否做结果自检 VERIFY（§6.20.5 / P1 #18）：空结果 / 截断 / 主键重复。 */
    @Builder.Default
    private final boolean selfCheck = true;

    /** 用于主键重复自检的列下标（0-based）；为 null 时跳过重复检查。 */
    @Builder.Default
    private final Integer duplicateKeyColumn = null;

    /** 用于主键重复自检的列名（按名称解析，优先于 {@link #duplicateKeyColumn}）；为 null 时跳过。
     * 通常由调用方从 {@code SchemaContext.primaryKeyColumn(table)} 注入（见 §6.38.2）。 */
    @Builder.Default
    private final String primaryKeyColumn = null;
}

