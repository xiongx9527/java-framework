package com.xiongx9527.fw.plus.ai.askdata.execution;

import lombok.Builder;
import lombok.Getter;

/** SQL 执行的轻量设置（§6.38 / §6.20 EXECUTE 节点）。只读、限行、超时由本对象承载。 */
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
}
