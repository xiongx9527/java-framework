package com.xiongx9527.fw.plus.ai.model;

public interface AiModelAdapter {

    default String getFramework() {
        return null;
    }

    boolean supports(AiModelDefinition definition);

    AiModel create(AiModelDefinition definition);
}
