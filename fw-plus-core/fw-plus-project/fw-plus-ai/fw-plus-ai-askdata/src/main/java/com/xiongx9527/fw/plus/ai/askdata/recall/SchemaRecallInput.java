package com.xiongx9527.fw.plus.ai.askdata.recall;

import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 表结构召回输入：问题 + 已权限过滤的候选表。预算/阈值等配置在召回能力上。 */
@Getter
@Builder
public class SchemaRecallInput {
    private final String question;
    @Builder.Default
    private final List<TableSchema> candidates = Collections.emptyList();
}
