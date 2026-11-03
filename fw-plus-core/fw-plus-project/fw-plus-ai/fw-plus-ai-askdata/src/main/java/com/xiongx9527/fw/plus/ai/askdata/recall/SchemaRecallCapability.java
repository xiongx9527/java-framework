package com.xiongx9527.fw.plus.ai.askdata.recall;

import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;

/** 表结构召回能力：问题 + 候选表 → 与问题相关的可见 schema。 */
public interface SchemaRecallCapability {
    SchemaContext recall(SchemaRecallInput input);
}
