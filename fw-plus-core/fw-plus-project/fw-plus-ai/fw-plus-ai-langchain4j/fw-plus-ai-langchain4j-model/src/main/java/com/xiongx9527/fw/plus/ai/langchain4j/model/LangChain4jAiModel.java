package com.xiongx9527.fw.plus.ai.langchain4j.model;

import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelType;

final class LangChain4jAiModel implements AiModel {

    private final AiModelDefinition definition;
    private final LangChain4jModel nativeModel;

    LangChain4jAiModel(AiModelDefinition definition, LangChain4jModel nativeModel) {
        this.definition = definition;
        this.nativeModel = nativeModel;
    }

    @Override
    public String getKey() {
        return definition.getKey();
    }

    @Override
    public String getFramework() {
        return definition.getFramework();
    }

    @Override
    public String getProvider() {
        return definition.getProvider();
    }

    @Override
    public AiModelType getType() {
        return definition.getType();
    }

    LangChain4jModel nativeModel() {
        return nativeModel;
    }
}
