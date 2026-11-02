package com.xiongx9527.fw.plus.ai.askdata.schema;

import lombok.Builder;
import lombok.Getter;

/** 表间关系（外键）。 */
@Getter
@Builder
public class RelationSchema {
    private final String fromTable;
    private final String fromColumn;
    private final String toTable;
    private final String toColumn;
}
