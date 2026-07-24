package com.xiongx9527.fw.plus.ai.askdata.flow;

import com.xiongx9527.fw.plus.ai.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.askdata.vector.AskDataRetrievedDocument;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AskDataStepContext {

    private final AskDataRequest request;
    private final Map<String, Object> attributes = new LinkedHashMap<String, Object>();
    private AskDataMetadata metadata;
    private List<AskDataRetrievedDocument> retrievedDocuments = new ArrayList<AskDataRetrievedDocument>();
    private String prompt;
    private String sql;

    public AskDataStepContext(AskDataRequest request) {
        this.request = request;
    }

    public AskDataRequest getRequest() {
        return request;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public AskDataMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(AskDataMetadata metadata) {
        this.metadata = metadata;
    }

    public List<AskDataRetrievedDocument> getRetrievedDocuments() {
        return retrievedDocuments;
    }

    public void setRetrievedDocuments(List<AskDataRetrievedDocument> retrievedDocuments) {
        this.retrievedDocuments = retrievedDocuments == null ? new ArrayList<AskDataRetrievedDocument>()
                : new ArrayList<AskDataRetrievedDocument>(retrievedDocuments);
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }
}
