package com.xiongx9527.fw.plus.ai.spring.adapter;

import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;

public interface SpringAiModelProviderAdapter {

    boolean supports(AiModelDefinition definition);

    Object create(AiModelDefinition definition);
}
