package com.xiongx9527.fw.plus.ai.langchain4j.adapter.model;

public class LangChain4jModel {

    private final String provider;

    private final LangChain4jModelType modelType;

    private final Object model;

    public LangChain4jModel(String provider, LangChain4jModelType modelType, Object model) {
        this.provider = provider;
        this.modelType = modelType;
        this.model = model;
    }

    public String getProvider() {
        return provider;
    }

    public LangChain4jModelType getModelType() {
        return modelType;
    }

    public Object getModel() {
        return model;
    }
}
