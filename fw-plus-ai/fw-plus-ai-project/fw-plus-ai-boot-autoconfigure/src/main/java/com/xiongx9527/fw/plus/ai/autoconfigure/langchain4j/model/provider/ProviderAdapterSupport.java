package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.LangChain4jModelDefinition;

final class ProviderAdapterSupport {

    private ProviderAdapterSupport() {
    }

    static boolean classPresent(String className) {
        try {
            Class.forName(className, false, ProviderAdapterSupport.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static Object property(LangChain4jModelDefinition definition, String name) {
        if (definition == null) {
            return null;
        }
        if ("baseUrl".equals(name)) {
            return definition.getBaseUrl();
        }
        if ("apiKey".equals(name)) {
            return definition.getApiKey();
        }
        if ("modelName".equals(name)) {
            return definition.getModelName();
        }
        if ("provider".equals(name)) {
            return definition.getProvider();
        }
        if ("modelType".equals(name)) {
            return definition.getModelType();
        }
        if ("key".equals(name)) {
            return definition.getKey();
        }
        if ("enabled".equals(name)) {
            return definition.isEnabled();
        }
        return definition.getProperties() == null ? null : definition.getProperties().get(name);
    }
}
