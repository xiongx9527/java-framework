package com.xiongx9527.fw.plus.ai.askdata.schema;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 表字段元数据。 */
@Getter
@Builder
public class ColumnSchema {
    private final String name;
    private final String type;
    private final String comment;
    @Builder.Default
    private final boolean primaryKey = false;
    @Builder.Default
    private final boolean nullable = true;
    @Builder.Default
    private final List<String> sampleValues = java.util.Collections.emptyList();
}
