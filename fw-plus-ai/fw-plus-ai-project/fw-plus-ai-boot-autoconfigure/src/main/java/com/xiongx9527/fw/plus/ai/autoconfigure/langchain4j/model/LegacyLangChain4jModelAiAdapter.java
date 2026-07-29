package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model;

import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class LegacyLangChain4jModelAiAdapter implements AiModelAdapter {

    private static final String FRAMEWORK = "langchain4j";

    private final LangChain4jModelFactory factory;
    private final List<LangChain4jModelProviderAdapter> adapters;

    LegacyLangChain4jModelAiAdapter(LangChain4jModelFactory factory,
                                    List<LangChain4jModelProviderAdapter> adapters) {
        this.factory = factory;
        this.adapters = adapters == null
                ? Collections.<LangChain4jModelProviderAdapter>emptyList()
                : Collections.unmodifiableList(new ArrayList<LangChain4jModelProviderAdapter>(adapters));
    }

    @Override
    public String getFramework() {
        return FRAMEWORK;
    }

    @Override
    public boolean supports(AiModelDefinition definition) {
        return definition != null && FRAMEWORK.equalsIgnoreCase(definition.getFramework());
    }

    @Override
    public AiModel create(AiModelDefinition definition) {
        LangChain4jModel nativeModel = factory.create(toLegacy(definition), adapters);
        if (nativeModel == null) {
            throw new AiModelException("Legacy LangChain4j factory returned null for " + context(definition));
        }
        return new Facade(definition, nativeModel);
    }

    private LangChain4jModelDefinition toLegacy(AiModelDefinition definition) {
        Map<String, Object> properties = definition.getProperties() == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(definition.getProperties());
        LangChain4jModelDefinition legacy = new LangChain4jModelDefinition();
        legacy.setKey(definition.getKey());
        legacy.setProvider(definition.getProvider());
        legacy.setModelType(definition.getType() == null ? null
                : LangChain4jModelType.valueOf(definition.getType().name()));
        legacy.setEnabled(definition.isEnabled());
        legacy.setBaseUrl(stringValue(properties.remove("baseUrl")));
        legacy.setApiKey(stringValue(properties.remove("apiKey")));
        legacy.setModelName(stringValue(properties.remove("modelName")));
        legacy.setProperties(properties);
        return legacy;
    }

    private String context(AiModelDefinition definition) {
        return "key=" + definition.getKey()
                + ", framework=" + definition.getFramework()
                + ", provider=" + definition.getProvider()
                + ", type=" + definition.getType();
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static final class Facade implements AiModel {

        private final AiModelDefinition definition;
        @SuppressWarnings("unused")
        private final LangChain4jModel nativeModel;

        private Facade(AiModelDefinition definition, LangChain4jModel nativeModel) {
            this.definition = definition;
            this.nativeModel = nativeModel;
        }

        @Override public String getKey() { return definition.getKey(); }
        @Override public String getFramework() { return definition.getFramework(); }
        @Override public String getProvider() { return definition.getProvider(); }
        @Override public AiModelType getType() { return definition.getType(); }
    }
}
