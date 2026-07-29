package com.xiongx9527.fw.plus.ai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultAiModelFactory implements AiModelFactory {

    @Override
    public AiModel create(AiModelDefinition definition, List<AiModelAdapter> adapters) {
        validateDefinition(definition);
        List<String> candidateFrameworks = candidateFrameworks(adapters, definition);
        List<AiModelAdapter> supportedAdapters = new ArrayList<AiModelAdapter>();
        if (adapters != null) {
            for (AiModelAdapter adapter : adapters) {
                if (adapter != null && matchesFramework(adapter, definition)
                        && supports(adapter, definition)) {
                    supportedAdapters.add(adapter);
                }
            }
        }
        if (supportedAdapters.size() == 1) {
            return create(supportedAdapters.get(0), definition, candidateFrameworks);
        }
        if (supportedAdapters.isEmpty()) {
            throw new AiModelException("No adapter supports " + context(definition, candidateFrameworks));
        }
        throw new AiModelException("Multiple adapters support " + context(definition, candidateFrameworks));
    }

    private void validateDefinition(AiModelDefinition definition) {
        if (definition == null) {
            throw new AiModelException("Model definition must not be null: " + context(null));
        }
        if (isBlank(definition.getKey())) {
            throw new AiModelException("Model definition key must not be blank: " + context(definition));
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

    private boolean matchesFramework(AiModelAdapter adapter, AiModelDefinition definition) {
        if (isBlank(definition.getFramework())) {
            return true;
        }
        return definition.getFramework().equals(adapterFramework(adapter, definition));
    }

    private List<String> candidateFrameworks(List<AiModelAdapter> adapters, AiModelDefinition definition) {
        List<String> frameworks = new ArrayList<String>();
        if (adapters != null) {
            for (AiModelAdapter adapter : adapters) {
                if (adapter != null) {
                    String framework = adapterFramework(adapter, definition);
                    if (!isBlank(framework) && !frameworks.contains(framework)) {
                        frameworks.add(framework);
                    }
                }
            }
        }
        Collections.sort(frameworks);
        return frameworks;
    }

    private String adapterFramework(AiModelAdapter adapter, AiModelDefinition definition) {
        try {
            return adapter.getFramework();
        } catch (Exception e) {
            throw new AiModelException("Adapter failed while reading framework for " + context(definition, Collections.<String>emptyList()), e);
        }
    }

    private AiModel create(AiModelAdapter adapter, AiModelDefinition definition, List<String> candidateFrameworks) {
        AiModel model;
        try {
            model = adapter.create(definition);
        } catch (Exception e) {
            throw new AiModelException("Adapter failed to create model for " + context(definition, candidateFrameworks), e);
        }
        if (model == null) {
            throw new AiModelException("Adapter returned null for " + context(definition, candidateFrameworks));
        }
        return model;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String context(AiModelDefinition definition) {
        return context(definition, Collections.<String>emptyList());
    }

    private String context(AiModelDefinition definition, List<String> candidateFrameworks) {
        if (definition == null) {
            return "key=null, framework=null, provider=null, type=null, candidates=" + candidateFrameworks;
        }
        return "key=" + definition.getKey()
                + ", framework=" + definition.getFramework()
                + ", provider=" + definition.getProvider()
                + ", type=" + definition.getType()
                + ", candidates=" + candidateFrameworks;
    }
}
