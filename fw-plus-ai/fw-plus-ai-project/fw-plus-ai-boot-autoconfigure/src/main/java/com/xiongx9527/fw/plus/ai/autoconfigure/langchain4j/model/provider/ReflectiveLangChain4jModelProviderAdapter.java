package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelException;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ReflectiveLangChain4jModelProviderAdapter implements LangChain4jModelProviderAdapter {

    private final String provider;
    private final LangChain4jModelType modelType;
    private final String builderClassName;
    private final String builderMethod;
    private final Map<String, String> propertyAliases;

    public ReflectiveLangChain4jModelProviderAdapter(String provider, LangChain4jModelType modelType,
                                                     String builderClassName, String builderMethod) {
        this(provider, modelType, builderClassName, builderMethod, Collections.<String, String>emptyMap());
    }

    public ReflectiveLangChain4jModelProviderAdapter(String provider, LangChain4jModelType modelType,
                                                     String builderClassName, String builderMethod,
                                                     Map<String, String> propertyAliases) {
        this.provider = provider;
        this.modelType = modelType;
        this.builderClassName = builderClassName;
        this.builderMethod = builderMethod;
        this.propertyAliases = propertyAliases == null
                ? Collections.<String, String>emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<String, String>(propertyAliases));
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
            for (Map.Entry<String, String> alias : propertyAliases.entrySet()) {
                if (value(definition, alias.getKey()) == null) {
                    builder = set(builder, alias.getKey(), value(definition, alias.getValue()));
                }
            }
            if (definition.getProperties() != null) {
                for (Map.Entry<String, Object> property : definition.getProperties().entrySet()) {
                    builder = setRequired(builder, property.getKey(), property.getValue());
                }
            }
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
        Method method = findSetter(target.getClass(), name, value);
        if (method == null) {
            return target;
        }
        Object result = method.invoke(target, convertValue(value, method.getParameterTypes()[0]));
        return result == null ? target : result;
    }

    private Object setRequired(Object target, String name, Object value) throws Exception {
        if (value == null) {
            return target;
        }
        Method method = findSetter(target.getClass(), name, value);
        if (method == null) {
            throw new IllegalArgumentException("Builder property is not supported: " + name);
        }
        Object result = method.invoke(target, convertValue(value, method.getParameterTypes()[0]));
        return result == null ? target : result;
    }

    private Method findSetter(Class<?> targetClass, String name, Object value) {
        for (Method method : targetClass.getMethods()) {
            if (name.equals(method.getName()) && method.getParameterTypes().length == 1
                    && canConvert(value, method.getParameterTypes()[0])) {
                return method;
            }
        }
        return null;
    }

    private boolean canConvert(Object value, Class<?> targetType) {
        if (value == null) {
            return !targetType.isPrimitive();
        }
        Class<?> wrapperType = wrapperType(targetType);
        if (wrapperType.isInstance(value) || String.class.equals(wrapperType)) {
            return true;
        }
        if (Number.class.isAssignableFrom(wrapperType) && (value instanceof Number || value instanceof String)) {
            return true;
        }
        if (Boolean.class.equals(wrapperType) && (value instanceof Boolean || value instanceof String)) {
            return true;
        }
        return Duration.class.equals(wrapperType)
                && (value instanceof Duration || value instanceof Number || value instanceof String);
    }

    private Object convertValue(Object value, Class<?> targetType) {
        Class<?> wrapperType = wrapperType(targetType);
        if (wrapperType.isInstance(value)) {
            return value;
        }
        if (String.class.equals(wrapperType)) {
            return String.valueOf(value);
        }
        if (Integer.class.equals(wrapperType)) {
            return value instanceof Number ? Integer.valueOf(((Number) value).intValue()) : Integer.valueOf(String.valueOf(value));
        }
        if (Long.class.equals(wrapperType)) {
            return value instanceof Number ? Long.valueOf(((Number) value).longValue()) : Long.valueOf(String.valueOf(value));
        }
        if (Double.class.equals(wrapperType)) {
            return value instanceof Number ? Double.valueOf(((Number) value).doubleValue()) : Double.valueOf(String.valueOf(value));
        }
        if (Float.class.equals(wrapperType)) {
            return value instanceof Number ? Float.valueOf(((Number) value).floatValue()) : Float.valueOf(String.valueOf(value));
        }
        if (Boolean.class.equals(wrapperType)) {
            return value instanceof Boolean ? value : Boolean.valueOf(String.valueOf(value));
        }
        if (Duration.class.equals(wrapperType)) {
            return value instanceof Number
                    ? Duration.ofMillis(((Number) value).longValue())
                    : Duration.parse(String.valueOf(value));
        }
        return value;
    }

    private Class<?> wrapperType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (Integer.TYPE.equals(type)) {
            return Integer.class;
        }
        if (Long.TYPE.equals(type)) {
            return Long.class;
        }
        if (Double.TYPE.equals(type)) {
            return Double.class;
        }
        if (Float.TYPE.equals(type)) {
            return Float.class;
        }
        if (Boolean.TYPE.equals(type)) {
            return Boolean.class;
        }
        return type;
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
