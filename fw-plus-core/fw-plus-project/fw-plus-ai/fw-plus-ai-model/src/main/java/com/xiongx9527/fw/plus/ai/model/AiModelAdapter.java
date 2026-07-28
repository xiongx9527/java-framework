package com.xiongx9527.fw.plus.ai.model;

public interface AiModelAdapter {

    boolean supports(AiModelDefinition definition);

    AiModel create(AiModelDefinition definition);
}
