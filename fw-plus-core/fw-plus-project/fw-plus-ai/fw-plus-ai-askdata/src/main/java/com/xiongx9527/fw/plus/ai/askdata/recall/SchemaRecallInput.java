package com.xiongx9527.fw.plus.ai.askdata.recall;

import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import com.xiongx9527.fw.plus.ai.support.retrieval.IndexMode;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 表结构召回输入：问题 + 已权限过滤的候选表。
 *
 * <p>indexMode 控制本次召回走质量型（HIGH_QUALITY）还是经济型（ECONOMICAL）；为 null 时由召回能力取默认
 * （质量型）。预算 / 阈值 / 重排等配置在召回能力上。</p>
 */
@Getter
@Builder
public class SchemaRecallInput {

    private final String question;

    @Builder.Default
    private final List<TableSchema> candidates = Collections.emptyList();

    /** 召回索引策略（质量型 / 经济型）；null 表示由召回能力决定默认策略。 */
    @Builder.Default
    private final IndexMode indexMode = null;
}
