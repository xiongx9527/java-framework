package com.xiongx9527.fw.plus.ai.spring.adapter;

import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;

import java.util.List;

public final class SpringAiModelAdapter implements AiModelAdapter {

    public static final String FRAMEWORK = "spring-ai";

    private final SpringAiModelAdapterSupport support;

    public SpringAiModelAdapter(List<SpringAiModelProviderAdapter> providers) {
        this.support = new SpringAiModelAdapterSupport(FRAMEWORK, providers);
    }

    @Override
    public String getFramework() {
        return FRAMEWORK;
    }

    @Override
    public boolean supports(AiModelDefinition definition) {
        return support.supports(definition);
    }

    @Override
    public AiModel create(AiModelDefinition definition) {
        return support.create(definition);
    }
}
