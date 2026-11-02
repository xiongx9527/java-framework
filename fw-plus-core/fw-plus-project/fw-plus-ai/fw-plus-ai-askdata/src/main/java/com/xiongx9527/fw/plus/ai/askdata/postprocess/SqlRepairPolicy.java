package com.xiongx9527.fw.plus.ai.askdata.postprocess;

import lombok.Builder;
import lombok.Getter;

/** SQL 自我修复环配置（§6.17 SQL 修复环）。 */
@Getter
@Builder
public class SqlRepairPolicy {
    @Builder.Default
    private final boolean enabled = true;
    @Builder.Default
    private final int maxAttempts = 1;
}
