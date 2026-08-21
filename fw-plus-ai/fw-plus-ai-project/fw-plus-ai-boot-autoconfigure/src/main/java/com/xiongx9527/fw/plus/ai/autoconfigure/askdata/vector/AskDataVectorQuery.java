package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelSelection;

import java.util.Map;

public class AskDataVectorQuery {

    private final AskDataProjectContext projectContext;
    private final String embeddingModelKey;
    private final AskDataModelSelection modelSelection;
    private final String retrieveType;
    private final String datasourceKey;
    private final String vectorStoreKey;
    private final String question;
    private final int topK;
    private final Map<String, Object> filters;

    public AskDataVectorQuery(String retrieveType, String datasourceKey, String vectorStoreKey,
            String question, int topK, Map<String, Object> filters) {
        this(null, AskDataModelSelection.empty(), retrieveType, datasourceKey, vectorStoreKey, question, topK,
                filters);
    }

    public AskDataVectorQuery(AskDataProjectContext projectContext, String embeddingModelKey, String retrieveType,
            String datasourceKey, String vectorStoreKey, String question, int topK, Map<String, Object> filters) {
        this(projectContext, AskDataModelSelection.builder().requestModelKey(embeddingModelKey).build(), retrieveType,
                datasourceKey, vectorStoreKey, question, topK, filters);
    }

    public static AskDataVectorQuery withModelSelection(AskDataProjectContext projectContext,
            AskDataModelSelection modelSelection, String retrieveType, String datasourceKey, String vectorStoreKey,
            String question, int topK, Map<String, Object> filters) {
        return new AskDataVectorQuery(projectContext, modelSelection, retrieveType, datasourceKey, vectorStoreKey,
                question, topK, filters);
    }

    private AskDataVectorQuery(AskDataProjectContext projectContext, AskDataModelSelection modelSelection,
            String retrieveType, String datasourceKey, String vectorStoreKey, String question, int topK,
            Map<String, Object> filters) {
        this.projectContext = projectContext;
        this.modelSelection = modelSelection == null ? AskDataModelSelection.empty() : modelSelection;
        this.embeddingModelKey = normalize(this.modelSelection.getRequestModelKey());
        this.retrieveType = retrieveType;
        this.datasourceKey = datasourceKey;
        this.vectorStoreKey = vectorStoreKey;
        this.question = question;
        this.topK = topK;
        this.filters = AskDataVectorImmutableValues.immutableMap(filters);
    }

    public AskDataProjectContext getProjectContext() { return projectContext; }
    public String getEmbeddingModelKey() { return embeddingModelKey; }
    public AskDataModelSelection getModelSelection() { return modelSelection; }
    public String getRetrieveType() { return retrieveType; }
    public String getDatasourceKey() { return datasourceKey; }
    public String getVectorStoreKey() { return vectorStoreKey; }
    public String getQuestion() { return question; }
    public int getTopK() { return topK; }
    public Map<String, Object> getFilters() { return AskDataVectorImmutableValues.immutableMap(filters); }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
