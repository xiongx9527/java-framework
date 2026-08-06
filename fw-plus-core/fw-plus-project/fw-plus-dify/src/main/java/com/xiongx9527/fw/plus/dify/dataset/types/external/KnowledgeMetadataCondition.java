package com.xiongx9527.fw.plus.dify.dataset.types.external;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public final class KnowledgeMetadataCondition {
    private String logical_operator;
    private List<Condition> conditions;

    @Getter
    @Setter
    public static final class Condition {
        private String name;
        private String comparison_operator;
        private Object value;
    }
}
