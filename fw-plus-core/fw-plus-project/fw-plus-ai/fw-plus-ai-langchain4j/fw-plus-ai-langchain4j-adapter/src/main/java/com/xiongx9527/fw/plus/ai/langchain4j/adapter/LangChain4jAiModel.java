package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelType;

public final class LangChain4jAiModel implements AiModel {

    private final AiModelDefinition definition;
    private final LangChain4jModel nativeModel;

    public LangChain4jAiModel(AiModelDefinition definition, LangChain4jModel nativeModel) {
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

    public LangChain4jModel getNativeModel() {
        return nativeModel;
    }
}
