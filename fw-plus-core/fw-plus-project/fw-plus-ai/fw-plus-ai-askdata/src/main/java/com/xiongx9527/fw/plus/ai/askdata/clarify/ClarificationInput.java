package com.xiongx9527.fw.plus.ai.askdata.clarify;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 澄清/反问的输入：原始问题 + 导致低置信或生成失败的问题点（如表名不明确、缺时间范围、多义）。
 */
@Getter
@Builder
public class ClarificationInput {

    /** 用户原始问题。 */
    private final String question;

    /** 已识别的问题点（如表不在可见 schema、缺少时间范围、指代歧义）。 */
    @lombok.Builder.Default
    private final List<String> issues = Collections.emptyList();

    /** 可见表名提示（可选，帮助模型给出更具体的反问）。 */
    @lombok.Builder.Default
    private final List<String> candidateTables = Collections.emptyList();
}
