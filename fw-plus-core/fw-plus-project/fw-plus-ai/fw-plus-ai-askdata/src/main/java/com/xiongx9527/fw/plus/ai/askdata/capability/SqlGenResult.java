package com.xiongx9527.fw.plus.ai.askdata.capability;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * SQL 生成能力的输出（§6.38.2）。
 */
@Getter
@Builder
public class SqlGenResult {
    /** 生成的 SQL（含方言正确的引号 / 分页）。 */
    private final String sql;

    /** 给用户的简短说明。 */
    private final String explanation;

    /** 引用到的表，供执行期与审计。 */
    @Builder.Default
    private final List<String> referencedTables = Collections.emptyList();

    /** 0~1 置信度，低置信走 §6.32 反问。 */
    @Builder.Default
    private final double confidence = 0.0d;

    /** 漂离时的可选 SQL。 */
    @Builder.Default
    private final List<String> alternatives = Collections.emptyList();
}
