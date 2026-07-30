package com.xiongx9527.fw.plus.ai.langchain4j.adapter;

import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelAdapter;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.model.DefaultAiModelFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DefaultLangChain4jModelFactory implements LangChain4jModelFactory, AiModelAdapter {

    private static final String FRAMEWORK = "langchain4j";
    private static final String BASE_URL = "baseUrl";
    private static final String API_KEY = "apiKey";
    private static final String MODEL_NAME = "modelName";

    private final List<LangChain4jModelProviderAdapter> providerAdapters;

    public DefaultLangChain4jModelFactory() {
        this(Collections.<LangChain4jModelProviderAdapter>emptyList());
    }

    public DefaultLangChain4jModelFactory(List<LangChain4jModelProviderAdapter> providerAdapters) {
        this.providerAdapters = providerAdapters == null
                ? Collections.<LangChain4jModelProviderAdapter>emptyList()
                : Collections.unmodifiableList(new ArrayList<LangChain4jModelProviderAdapter>(providerAdapters));
    }

    @Override
    public String getFramework() {
        return FRAMEWORK;
    }

    @Override
    public LangChain4jModel create(LangChain4jModelDefinition definition,
                                   List<LangChain4jModelProviderAdapter> adapters) {
        validateLegacyDefinition(definition);
        try {
            AiModel model = new DefaultAiModelFactory().create(toNeutral(definition),
                    Collections.<AiModelAdapter>singletonList(new DefaultLangChain4jModelFactory(adapters)));
            return ((LangChain4jAiModel) model).getNativeModel();
        } catch (AiModelException e) {
            AiModelException translated = e.getCause() instanceof AiModelException
                    ? (AiModelException) e.getCause() : e;
            throw new LangChain4jModelException(translated.getMessage(), translated.getCause());
        }
    }

    @Override
    public boolean supports(AiModelDefinition definition) {
        return definition != null
                && FRAMEWORK.equalsIgnoreCase(definition.getFramework())
                && findAdapter(toLegacy(definition)) != null;
    }

    @Override
    public AiModel create(AiModelDefinition definition) {
        if (definition == null || !FRAMEWORK.equalsIgnoreCase(definition.getFramework())) {
            throw new AiModelException("LangChain4j adapter does not support " + context(definition));
        }
        LangChain4jModelDefinition legacyDefinition = toLegacy(definition);
        LangChain4jModelProviderAdapter adapter = findAdapter(legacyDefinition);
        if (adapter == null) {
            throw new AiModelException("No LangChain4j provider adapter supports " + context(definition));
        }
        LangChain4jModel nativeModel;
        try {
            nativeModel = adapter.create(legacyDefinition);
        } catch (Exception e) {
            throw new AiModelException("LangChain4j provider adapter failed to create " + context(definition), e);
        }
        if (nativeModel == null) {
            throw new AiModelException("LangChain4j provider adapter returned null for " + context(definition));
        }
        return new LangChain4jAiModel(definition, nativeModel);
    }

    private LangChain4jModelProviderAdapter findAdapter(LangChain4jModelDefinition definition) {
        for (LangChain4jModelProviderAdapter adapter : providerAdapters) {
            if (adapter != null && adapter.supports(definition)) {
                return adapter;
            }
        }
        return null;
    }

    private void validateLegacyDefinition(LangChain4jModelDefinition definition) {
        if (definition == null) {
            throw new LangChain4jModelException("Model definition must not be null");
        }
        if (isBlank(definition.getKey())) {
            throw new LangChain4jModelException("Model definition key must not be blank: " + legacyContext(definition));
        }
        if (isBlank(definition.getProvider())) {
            throw new LangChain4jModelException("Model definition provider must not be blank: " + legacyContext(definition));
        }
        if (definition.getModelType() == null) {
            throw new LangChain4jModelException("Model definition modelType must not be null: " + legacyContext(definition));
        }
    }

    static AiModelDefinition toNeutral(LangChain4jModelDefinition definition) {
        if (definition == null) {
            return null;
        }
        Map<String, Object> properties = definition.getProperties() == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(definition.getProperties());
        put(properties, BASE_URL, definition.getBaseUrl());
        put(properties, API_KEY, definition.getApiKey());
        put(properties, MODEL_NAME, definition.getModelName());

        AiModelDefinition neutral = new AiModelDefinition();
        neutral.setKey(definition.getKey());
        neutral.setFramework(FRAMEWORK);
        neutral.setProvider(definition.getProvider());
        neutral.setType(definition.getModelType() == null ? null
                : AiModelType.valueOf(definition.getModelType().name()));
        neutral.setEnabled(definition.isEnabled());
        neutral.setProperties(properties);
        return neutral;
    }

    static LangChain4jModelDefinition toLegacy(AiModelDefinition definition) {
        LangChain4jModelDefinition legacy = new LangChain4jModelDefinition();
        legacy.setKey(definition.getKey());
        legacy.setProvider(definition.getProvider());
        legacy.setModelType(definition.getType() == null ? null
                : LangChain4jModelType.valueOf(definition.getType().name()));
        legacy.setEnabled(definition.isEnabled());
        Map<String, Object> properties = definition.getProperties() == null
                ? new LinkedHashMap<String, Object>()
                : new LinkedHashMap<String, Object>(definition.getProperties());
        legacy.setBaseUrl(stringValue(properties.remove(BASE_URL)));
        legacy.setApiKey(stringValue(properties.remove(API_KEY)));
        legacy.setModelName(stringValue(properties.remove(MODEL_NAME)));
        legacy.setProperties(properties);
        return legacy;
    }

    private static void put(Map<String, Object> properties, String key, String value) {
        if (value != null) {
            properties.put(key, value);
        }
    }

    private static String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String context(AiModelDefinition definition) {
        if (definition == null) {
            return "definition=null";
        }
        return "key=" + definition.getKey()
                + ", framework=" + definition.getFramework()
                + ", provider=" + definition.getProvider()
                + ", type=" + definition.getType();
    }

    private static String legacyContext(LangChain4jModelDefinition definition) {
        return "key=" + definition.getKey()
                + ", provider=" + definition.getProvider()
                + ", modelType=" + definition.getModelType();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
