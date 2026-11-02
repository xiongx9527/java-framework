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
    /** 是否 fail-closed（P0 #6 / 附录 B.1.1）：修复环耗尽后校验仍不通过，直接抛异常而非返回不可信 SQL。 */
    @Builder.Default
    private final boolean failClosed = false;
}

