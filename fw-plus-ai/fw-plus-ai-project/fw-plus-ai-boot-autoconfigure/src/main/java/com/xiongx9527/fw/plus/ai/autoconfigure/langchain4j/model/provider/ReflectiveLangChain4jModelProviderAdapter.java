package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelException;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class ReflectiveLangChain4jModelProviderAdapter implements LangChain4jModelProviderAdapter {

    private final String provider;
    private final LangChain4jModelType modelType;
    private final String builderClassName;
    private final String builderMethod;

    public ReflectiveLangChain4jModelProviderAdapter(String provider, LangChain4jModelType modelType,
                                                     String builderClassName, String builderMethod) {
        this.provider = provider;
        this.modelType = modelType;
        this.builderClassName = builderClassName;
        this.builderMethod = builderMethod;
    }

    @Override
    public boolean supports(LangChain4jModelDefinition definition) {
        return definition != null
                && provider.equalsIgnoreCase(definition.getProvider())
                && modelType == definition.getModelType()
                && ProviderAdapterSupport.classPresent(builderClassName);
    }

    @Override
    public LangChain4jModel create(LangChain4jModelDefinition definition) {
        try {
            Object builder = builder(definition);
            builder = set(builder, "baseUrl", value(definition, "baseUrl"));
            builder = set(builder, "endpoint", value(definition, "endpoint"));
            builder = set(builder, "apiKey", value(definition, "apiKey"));
            builder = set(builder, "accessToken", value(definition, "accessToken", "apiKey"));
            builder = set(builder, "secretKey", value(definition, "secretKey", "apiKey"));
            builder = set(builder, "modelName", value(definition, "modelName"));
            builder = set(builder, "deploymentName", value(definition, "deploymentName", "modelName"));
            builder = set(builder, "model", value(definition, "model", "modelName"));
            builder = set(builder, "modelId", value(definition, "modelId", "modelName"));
            return new LangChain4jModel(provider, modelType, isDirectModel() ? builder : build(builder));
        } catch (Exception e) {
            throw new LangChain4jModelException("Failed to create LangChain4j model: provider=" + provider
                    + ", modelType=" + modelType + ", key=" + key(definition), e);
        }
    }

    private Object builder(LangChain4jModelDefinition definition) throws Exception {
        Class<?> builderClass = Class.forName(builderClassName, true,
                Thread.currentThread().getContextClassLoader());
        if (builderMethod == null || builderMethod.trim().length() == 0) {
            Constructor<?> constructor = builderClass.getDeclaredConstructor();
            return constructor.newInstance();
        }
        return builderClass.getMethod(builderMethod).invoke(null);
    }

    private Object set(Object target, String name, Object value) throws Exception {
        if (value == null) {
            return target;
        }
        for (Method method : target.getClass().getMethods()) {
            if (name.equals(method.getName()) && method.getParameterTypes().length == 1
                    && method.getParameterTypes()[0].isAssignableFrom(value.getClass())) {
                Object result = method.invoke(target, value);
                return result == null ? target : result;
            }
        }
        return target;
    }

    private Object build(Object builder) throws Exception {
        return builder.getClass().getMethod("build").invoke(builder);
    }

    private Object value(LangChain4jModelDefinition definition, String name) {
        return ProviderAdapterSupport.property(definition, name);
    }

    private Object value(LangChain4jModelDefinition definition, String primary, String fallback) {
        Object value = value(definition, primary);
        return value == null ? value(definition, fallback) : value;
    }

    private String key(LangChain4jModelDefinition definition) {
        Object key = ProviderAdapterSupport.property(definition, "key");
        return key == null ? null : String.valueOf(key);
    }

    private boolean isDirectModel() {
        return builderMethod == null || builderMethod.trim().length() == 0;
    }
}
