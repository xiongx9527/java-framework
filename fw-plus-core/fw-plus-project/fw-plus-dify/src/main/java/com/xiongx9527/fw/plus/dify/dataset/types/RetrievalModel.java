package com.xiongx9527.fw.plus.dify.dataset.types;

import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.alibaba.fastjson2.annotation.JSONType;
import com.xiongx9527.fw.plus.dify.dataset.types.external.KnowledgeMetadataCondition;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@JSONType(naming = PropertyNamingStrategy.SnakeCase)
public class RetrievalModel {
    /**
     * 检索方法
     */
    private SearchMethod searchMethod;

    /**
     * 元数据过滤条件（dify v1.10+ 的 metadata_filtering_conditions，
     * 结构与外部知识库 metadata_condition 一致，复用同一模型）
     */
    private KnowledgeMetadataCondition metadataFilteringConditions;

    /**
     * 是否开启rerank
     */
    private Boolean rerankingEnable;

    /**
     * Rerank 模型配置
     */
    private RerankingModel rerankingModel;

    /**
     * 混合检索模式下语意检索的权重设置
     */
    private Double weights;

    /**
     * 召回条数
     */
    private Integer topK;

    /**
     * 是否开启召回分数限制
     */
    private Boolean scoreThresholdEnabled;

    /**
     * 召回分数限制
     */
    private Double scoreThreshold;

    /**
     * 检索方法
     */
    public enum SearchMethod {
        /**
         * 混合检索
         */
        hybrid_search,

        /**
         * 语义检索
         */
        semantic_search,

        /**
         * 全文检索
         */
        full_text_search,

        /**
         * 关键字检索
         */
        keyword_search,
    }

    /**
     * Rerank 模型配置
     */
    @Getter
    @Setter
    @Builder
    @JSONType(naming = PropertyNamingStrategy.SnakeCase)
    public static class RerankingModel {
        /**
         * Rerank 模型的提供商
         */
        private String rerankingProviderName;

        /**
         * Rerank 模型的名称
         */
        private String rerankingModelName;
    }

}
