package com.xiongx9527.fw.plus.ai.nativeimpl.retrieval;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dify 知识库检索配置：服务端地址、API Key、目标知识库（支持多库 fan-out）与可选检索模型参数。
 *
 * <p>服务端默认取 Dify 官方云 {@code https://api.dify.ai/v1}；自部署时通过 {@code server} 覆盖。
 * {@code datasetId} 与 {@code datasetIds} 二选一，后者用于跨多知识库检索后按分数融合。</p>
 */
public final class DifyRetrieverConfig {

    private final String server;

    private final String apiKey;

    private final String datasetId;

    private final List<String> datasetIds;

    private final String searchMethod;

    private final Integer topK;

    private final Boolean scoreThresholdEnabled;

    private final Double scoreThreshold;

    private final Boolean rerankingEnable;

    private final String rerankingProviderName;

    private final String rerankingModelName;

    private final Double weights;

    private final List<DifyMetadataCondition> metadataConditions;

    private final String metadataLogicalOperator;

    private final long connectTimeoutMillis;

    private final long readTimeoutMillis;

    private DifyRetrieverConfig(Builder b) {
        this.server = b.server;
        this.apiKey = b.apiKey;
        this.datasetId = b.datasetId;
        this.datasetIds = b.datasetIds;
        this.searchMethod = b.searchMethod;
        this.topK = b.topK;
        this.scoreThresholdEnabled = b.scoreThresholdEnabled;
        this.scoreThreshold = b.scoreThreshold;
        this.rerankingEnable = b.rerankingEnable;
        this.rerankingProviderName = b.rerankingProviderName;
        this.rerankingModelName = b.rerankingModelName;
        this.weights = b.weights;
        this.metadataConditions = b.metadataConditions;
        this.metadataLogicalOperator = b.metadataLogicalOperator;
        this.connectTimeoutMillis = b.connectTimeoutMillis;
        this.readTimeoutMillis = b.readTimeoutMillis;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getServer() {
        return server;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getDatasetId() {
        return datasetId;
    }

    public List<String> getDatasetIds() {
        return datasetIds;
    }

    public String getSearchMethod() {
        return searchMethod;
    }

    public Integer getTopK() {
        return topK;
    }

    public Boolean getScoreThresholdEnabled() {
        return scoreThresholdEnabled;
    }

    public Double getScoreThreshold() {
        return scoreThreshold;
    }

    public Boolean getRerankingEnable() {
        return rerankingEnable;
    }

    public String getRerankingProviderName() {
        return rerankingProviderName;
    }

    public String getRerankingModelName() {
        return rerankingModelName;
    }

    public Double getWeights() {
        return weights;
    }

    public List<DifyMetadataCondition> getMetadataConditions() {
        return metadataConditions;
    }

    public String getMetadataLogicalOperator() {
        return metadataLogicalOperator;
    }

    public long getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public long getReadTimeoutMillis() {
        return readTimeoutMillis;
    }

    /** 是否已具备发起检索的最小信息：server + apiKey + 至少一个知识库。 */
    public boolean configured() {
        if (server == null || server.trim().isEmpty()) {
            return false;
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return false;
        }
        return (datasetId != null && !datasetId.trim().isEmpty())
                || (datasetIds != null && !datasetIds.isEmpty());
    }

    /** 解析本次要检索的知识库列表（datasetIds 优先，否则退化为单 datasetId）。 */
    public List<String> resolvedDatasetIds() {
        if (datasetIds != null && !datasetIds.isEmpty()) {
            return Collections.unmodifiableList(new ArrayList<String>(datasetIds));
        }
        List<String> single = new ArrayList<String>();
        if (datasetId != null && !datasetId.trim().isEmpty()) {
            single.add(datasetId);
        }
        return Collections.unmodifiableList(single);
    }

    /** 构造请求体里的 retrieval_model（仅在配置了任意参数时返回；全空则省略该字段）。 */
    public Map<String, Object> retrievalModelBody() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        if (searchMethod != null) {
            m.put("search_method", searchMethod);
        }
        if (topK != null) {
            m.put("top_k", topK);
        }
        if (scoreThresholdEnabled != null) {
            m.put("score_threshold_enabled", scoreThresholdEnabled);
        }
        if (scoreThreshold != null) {
            m.put("score_threshold", scoreThreshold);
        }
        if (rerankingEnable != null) {
            m.put("reranking_enable", rerankingEnable);
        }
        if (rerankingProviderName != null || rerankingModelName != null) {
            Map<String, Object> rm = new LinkedHashMap<String, Object>();
            if (rerankingProviderName != null) {
                rm.put("reranking_provider_name", rerankingProviderName);
            }
            if (rerankingModelName != null) {
                rm.put("reranking_model_name", rerankingModelName);
            }
            m.put("reranking_model", rm);
        }
        if (weights != null) {
            m.put("weights", weights);
        }
        if (metadataConditions != null && !metadataConditions.isEmpty()) {
            Map<String, Object> filter = new LinkedHashMap<String, Object>();
            filter.put("logical_operator",
                    metadataLogicalOperator == null ? "and" : metadataLogicalOperator);
            List<Map<String, Object>> conditions = new ArrayList<Map<String, Object>>();
            for (DifyMetadataCondition c : metadataConditions) {
                if (c.getName() == null || c.getName().trim().isEmpty()) {
                    continue;
                }
                Map<String, Object> cm = new LinkedHashMap<String, Object>();
                cm.put("name", c.getName());
                cm.put("comparison_operator",
                        c.getComparisonOperator() == null ? "equal" : c.getComparisonOperator());
                cm.put("value", c.getValue());
                conditions.add(cm);
            }
            filter.put("conditions", conditions);
            m.put("metadata_filtering_conditions", filter);
        }
        return m.isEmpty() ? null : m;
    }

    /** 构建检索请求体（query + 可选 retrieval_model）。 */
    public Map<String, Object> requestBody(String text) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("query", text);
        Map<String, Object> model = retrievalModelBody();
        if (model != null) {
            body.put("retrieval_model", model);
        }
        return body;
    }

    public static final class Builder {

        private String server = "https://api.dify.ai/v1";

        private String apiKey;

        private String datasetId;

        private List<String> datasetIds;

        private String searchMethod;

        private Integer topK;

        private Boolean scoreThresholdEnabled;

        private Double scoreThreshold;

        private Boolean rerankingEnable;

        private String rerankingProviderName;

        private String rerankingModelName;

        private Double weights;

        private List<DifyMetadataCondition> metadataConditions;

        private String metadataLogicalOperator = "and";

        private long connectTimeoutMillis = 30_000L;

        private long readTimeoutMillis = 30_000L;

        private Builder() {
        }

        public Builder server(String v) {
            this.server = v;
            return this;
        }

        public Builder apiKey(String v) {
            this.apiKey = v;
            return this;
        }

        public Builder datasetId(String v) {
            this.datasetId = v;
            return this;
        }

        public Builder datasetIds(List<String> v) {
            this.datasetIds = v;
            return this;
        }

        public Builder searchMethod(String v) {
            this.searchMethod = v;
            return this;
        }

        public Builder topK(Integer v) {
            this.topK = v;
            return this;
        }

        public Builder scoreThresholdEnabled(Boolean v) {
            this.scoreThresholdEnabled = v;
            return this;
        }

        public Builder scoreThreshold(Double v) {
            this.scoreThreshold = v;
            return this;
        }

        public Builder rerankingEnable(Boolean v) {
            this.rerankingEnable = v;
            return this;
        }

        public Builder rerankingProviderName(String v) {
            this.rerankingProviderName = v;
            return this;
        }

        public Builder rerankingModelName(String v) {
            this.rerankingModelName = v;
            return this;
        }

        public Builder weights(Double v) {
            this.weights = v;
            return this;
        }

        public Builder metadataConditions(List<DifyMetadataCondition> v) {
            this.metadataConditions = v;
            return this;
        }

        public Builder metadataLogicalOperator(String v) {
            this.metadataLogicalOperator = v;
            return this;
        }

        public Builder connectTimeoutMillis(long v) {
            this.connectTimeoutMillis = v;
            return this;
        }

        public Builder readTimeoutMillis(long v) {
            this.readTimeoutMillis = v;
            return this;
        }

        public DifyRetrieverConfig build() {
            return new DifyRetrieverConfig(this);
        }
    }

    /**
     * Dify 元数据字段查询条件（v1.10+ 的 {@code metadata_filtering_conditions}）。
     *
     * <p>对应 Dify 召回时按知识库元数据字段过滤：{@code name} 为字段名，{@code comparisonOperator}
     * 为比较算子（如 {@code equal} / {@code not equal} / {@code contains}），{@code value} 为比较值。</p>
     */
    public static final class DifyMetadataCondition {

        private final String name;

        private final String comparisonOperator;

        private final Object value;

        public DifyMetadataCondition(String name, String comparisonOperator, Object value) {
            this.name = name;
            this.comparisonOperator = comparisonOperator;
            this.value = value;
        }

        public static DifyMetadataCondition of(String name, String comparisonOperator, Object value) {
            return new DifyMetadataCondition(name, comparisonOperator, value);
        }

        public String getName() {
            return name;
        }

        public String getComparisonOperator() {
            return comparisonOperator;
        }

        public Object getValue() {
            return value;
        }
    }
}
