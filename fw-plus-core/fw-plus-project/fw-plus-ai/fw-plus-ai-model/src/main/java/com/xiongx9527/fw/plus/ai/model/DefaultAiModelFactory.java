package com.xiongx9527.fw.plus.ai.model;

import java.util.List;

public class DefaultAiModelFactory implements AiModelFactory {

    @Override
    public AiModel create(AiModelDefinition definition, List<AiModelAdapter> adapters) {
        validateDefinition(definition);
        if (adapters != null) {
            for (AiModelAdapter adapter : adapters) {
                if (adapter != null && supports(adapter, definition)) {
                    return create(adapter, definition);
                }
            }
        }
        throw new AiModelException("No adapter supports " + context(definition));
    }

    private void validateDefinition(AiModelDefinition definition) {
        if (definition == null) {
            throw new AiModelException("Model definition must not be null");
        }
        if (isBlank(definition.getKey())) {
            throw new AiModelException("Model definition key must not be blank: " + context(definition));
        }
        if (isBlank(definition.getFramework())) {
            throw new AiModelException("Model definition framework must not be blank: " + context(definition));
        }
        if (isBlank(definition.getProvider())) {
            throw new AiModelException("Model definition provider must not be blank: " + context(definition));
        }
        if (definition.getType() == null) {
            throw new AiModelException("Model definition type must not be null: " + context(definition));
        }
    }

    private boolean supports(AiModelAdapter adapter, AiModelDefinition definition) {
        try {
            return adapter.supports(definition);
        } catch (Exception e) {
            throw new AiModelException("Adapter failed while checking support for " + context(definition), e);
        }
    }

    private AiModel create(AiModelAdapter adapter, AiModelDefinition definition) {
        AiModel model;
        try {
            model = adapter.create(definition);
        } catch (Exception e) {
            throw new AiModelException("Adapter failed to create model for " + context(definition), e);
        }
        if (model == null) {
            throw new AiModelException("Adapter returned null for " + context(definition));
        }
        return model;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String context(AiModelDefinition definition) {
        if (definition == null) {
            return "definition=null";
        }
        return "key=" + definition.getKey()
                + ", framework=" + definition.getFramework()
                + ", provider=" + definition.getProvider()
                + ", type=" + definition.getType();
    }
}
