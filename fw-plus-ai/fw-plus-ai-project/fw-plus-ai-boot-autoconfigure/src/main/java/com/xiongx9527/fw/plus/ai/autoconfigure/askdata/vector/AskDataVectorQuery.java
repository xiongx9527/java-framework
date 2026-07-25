package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class AskDataVectorQuery {

    private final String retrieveType;
    private final String datasourceKey;
    private final String vectorStoreKey;
    private final String question;
    private final int topK;
    private final Map<String, Object> filters;

    public AskDataVectorQuery(String retrieveType, String datasourceKey, String vectorStoreKey,
            String question, int topK, Map<String, Object> filters) {
        this.retrieveType = retrieveType;
        this.datasourceKey = datasourceKey;
        this.vectorStoreKey = vectorStoreKey;
        this.question = question;
        this.topK = topK;
        this.filters = filters == null ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, Object>(filters));
    }

    public String getRetrieveType() { return retrieveType; }
    public String getDatasourceKey() { return datasourceKey; }
    public String getVectorStoreKey() { return vectorStoreKey; }
    public String getQuestion() { return question; }
    public int getTopK() { return topK; }
    public Map<String, Object> getFilters() { return filters; }
}
