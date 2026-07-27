package com.xiongx9527.fw.plus.ai.langchain4j.model;

import java.util.List;

public class DefaultLangChain4jModelFactory implements LangChain4jModelFactory {

    @Override
    public LangChain4jModel create(LangChain4jModelDefinition definition, List<LangChain4jModelProviderAdapter> adapters) {
        validateDefinition(definition);
        if (adapters != null) {
            for (LangChain4jModelProviderAdapter adapter : adapters) {
                if (adapter != null && supports(adapter, definition)) {
                    return create(adapter, definition);
                }
            }
        }
        throw new LangChain4jModelException("No adapter supports " + context(definition));
    }

    private void validateDefinition(LangChain4jModelDefinition definition) {
        if (definition == null) {
            throw new LangChain4jModelException("Model definition must not be null");
        }
        if (isBlank(definition.getKey())) {
            throw new LangChain4jModelException("Model definition key must not be blank: " + context(definition));
        }
        if (isBlank(definition.getProvider())) {
            throw new LangChain4jModelException("Model definition provider must not be blank: " + context(definition));
        }
        if (definition.getModelType() == null) {
            throw new LangChain4jModelException("Model definition modelType must not be null: " + context(definition));
        }
    }

    private boolean supports(LangChain4jModelProviderAdapter adapter, LangChain4jModelDefinition definition) {
        try {
            return adapter.supports(definition);
        } catch (Exception e) {
            throw new LangChain4jModelException("Adapter failed while checking support for " + context(definition), e);
        }
    }

    private LangChain4jModel create(LangChain4jModelProviderAdapter adapter, LangChain4jModelDefinition definition) {
        LangChain4jModel model;
        try {
            model = adapter.create(definition);
        } catch (Exception e) {
            throw new LangChain4jModelException("Adapter failed to create model for " + context(definition), e);
        }
        if (model == null) {
            throw new LangChain4jModelException("Adapter returned null for " + context(definition));
        }
        return model;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String context(LangChain4jModelDefinition definition) {
        return "key=" + definition.getKey()
                + ", provider=" + definition.getProvider()
                + ", modelType=" + definition.getModelType();
    }
}
