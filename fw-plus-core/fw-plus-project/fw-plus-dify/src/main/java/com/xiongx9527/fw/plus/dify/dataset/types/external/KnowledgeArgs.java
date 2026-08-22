package com.xiongx9527.fw.plus.dify.dataset.types.external;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeArgs {
    /**
     * 知识库唯一 ID
     */
    private String knowledge_id;

    /**
     * 用户的查询
     */
    private String query;

    /**
     * 知识检索参数
     */
    private Setting retrieval_setting;

    /**
     * 元数据过滤条件
     */
    private KnowledgeMetadataCondition metadata_condition;

    @Setter
    @Getter
    public static class Setting {
        /**
         * 检索结果的最大数量
         */
        private Integer top_k;

        /**
         * 结果与查询相关性的分数限制，范围：0~1
         */
        private Double score_threshold;
    }
}
