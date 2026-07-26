package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.context.ApplicationContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class LangChain4jEmbeddingStoreProviderAdapter implements EmbeddingStoreProviderAdapter {

    private static final String CONSTRUCTOR_ARGS = "constructorArgs";

    private final Map<String, String> storeClasses;

    private final ApplicationContext applicationContext;

    public LangChain4jEmbeddingStoreProviderAdapter() {
        this(defaultStoreClasses(), null);
    }

    LangChain4jEmbeddingStoreProviderAdapter(Map<String, String> storeClasses) {
        this(storeClasses, null);
    }

    LangChain4jEmbeddingStoreProviderAdapter(Map<String, String> storeClasses, ApplicationContext applicationContext) {
        this.storeClasses = Collections.unmodifiableMap(new LinkedHashMap<String, String>(storeClasses));
        this.applicationContext = applicationContext;
    }

    @Override
    public boolean supports(String provider) {
        return storeClasses.containsKey(normalize(provider));
    }

    @Override
    @SuppressWarnings("unchecked")
    public EmbeddingStore<TextSegment> create(EmbeddingStoreDefinition definition) {
        try {
            String className = storeClasses.get(normalize(definition.getProvider()));
            if (className == null) {
                throw new IllegalArgumentException("Unsupported embedding store provider: " + definition.getProvider());
            }
            Class<?> storeClass = Class.forName(className);
            Object store = createStore(storeClass, definition);
            if (!(store instanceof EmbeddingStore)) {
                throw new IllegalStateException(className + " is not an EmbeddingStore");
            }
            return (EmbeddingStore<TextSegment>) store;
        } catch (Exception ex) {
            throw ProviderAdapterSupport.exception(definition, ex);
        }
    }

    private Object createStore(Class<?> storeClass, EmbeddingStoreDefinition definition) throws Exception {
        Method builderMethod = findBuilderMethod(storeClass);
        if (builderMethod != null) {
            Object builder = builderMethod.invoke(null);
            configureBuilder(builder, definition);
            Method buildMethod = findBuildMethod(builder.getClass());
            return buildMethod.invoke(builder);
        }
        return createWithConstructorArgs(storeClass, definition);
    }

    private Method findBuilderMethod(Class<?> storeClass) {
        for (Method method : storeClass.getMethods()) {
            if ("builder".equals(method.getName())
                    && method.getParameterTypes().length == 0
                    && Modifier.isStatic(method.getModifiers())) {
                return method;
            }
        }
        return null;
    }

    private Method findBuildMethod(Class<?> builderClass) {
        for (Method method : builderClass.getMethods()) {
            if ("build".equals(method.getName()) && method.getParameterTypes().length == 0) {
                return method;
            }
        }
        throw new IllegalArgumentException("build method not found on " + builderClass.getName());
    }

    private void configureBuilder(Object builder, EmbeddingStoreDefinition definition) throws Exception {
        Map<String, Object> properties = new LinkedHashMap<String, Object>(definition.getProperties());
        if (definition.getDimension() != null && !properties.containsKey("dimension")) {
            properties.put("dimension", definition.getDimension());
        }
        if (definition.getMetadataKeys() != null
                && !definition.getMetadataKeys().isEmpty()
                && !properties.containsKey("metadataKeys")) {
            properties.put("metadataKeys", definition.getMetadataKeys());
        }
        properties.remove(CONSTRUCTOR_ARGS);
        for (Map.Entry<String, Object> entry : properties.entrySet()) {
            invokeBuilderSetter(builder, entry.getKey(), entry.getValue());
        }
    }

    private void invokeBuilderSetter(Object builder, String name, Object value) throws Exception {
        Method method = findSetter(builder.getClass(), name, value);
        if (method == null) {
            throw new IllegalArgumentException("Builder property is not supported: " + name);
        }
        method.invoke(builder, convertValue(value, method.getParameterTypes()[0]));
    }

    private Method findSetter(Class<?> builderClass, String name, Object value) {
        for (Method method : builderClass.getMethods()) {
            if (!name.equals(method.getName()) || method.getParameterTypes().length != 1) {
                continue;
            }
            if (canConvert(value, method.getParameterTypes()[0])) {
                return method;
            }
        }
        return null;
    }

    private Object createWithConstructorArgs(Class<?> storeClass, EmbeddingStoreDefinition definition) throws Exception {
        Object argsValue = definition.getProperties().get(CONSTRUCTOR_ARGS);
        if (argsValue == null) {
            Constructor<?> constructor = storeClass.getConstructor();
            return constructor.newInstance();
        }
        if (!(argsValue instanceof List)) {
            throw new IllegalArgumentException("builder method not found; constructorArgs list is required for "
                    + storeClass.getName());
        }
        List<?> args = (List<?>) argsValue;
        Constructor<?> constructor = findConstructor(storeClass, args);
        if (constructor == null) {
            throw new IllegalArgumentException("Matching constructor not found on " + storeClass.getName());
        }
        Object[] convertedArgs = new Object[args.size()];
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        for (int i = 0; i < args.size(); i++) {
            convertedArgs[i] = convertValue(args.get(i), parameterTypes[i]);
        }
        return constructor.newInstance(convertedArgs);
    }

    private Constructor<?> findConstructor(Class<?> storeClass, List<?> args) {
        for (Constructor<?> constructor : storeClass.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length != args.size()) {
                continue;
            }
            boolean matched = true;
            for (int i = 0; i < parameterTypes.length; i++) {
                if (!canConvert(args.get(i), parameterTypes[i])) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return constructor;
            }
        }
        return null;
    }

    private boolean canConvert(Object value, Class<?> targetType) {
        if (value == null) {
            return !targetType.isPrimitive();
        }
        Class<?> wrapperType = wrapperType(targetType);
        if (wrapperType.isInstance(value)) {
            return true;
        }
        if (String.class.equals(wrapperType)) {
            return true;
        }
        if (Number.class.isAssignableFrom(wrapperType) && (value instanceof Number || value instanceof String)) {
            return true;
        }
        if (Boolean.class.equals(wrapperType) && (value instanceof Boolean || value instanceof String)) {
            return true;
        }
        if (Enum.class.isAssignableFrom(wrapperType) && value instanceof String) {
            return true;
        }
        if (Collection.class.isAssignableFrom(wrapperType)
                && (value instanceof Collection || value instanceof String)) {
            return true;
        }
        if (Duration.class.equals(wrapperType) && (value instanceof Duration || value instanceof Number || value instanceof String)) {
            return true;
        }
        if (applicationContext != null && value instanceof String) {
            return true;
        }
        return false;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object convertValue(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
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
        if (Short.class.equals(wrapperType)) {
            return value instanceof Number ? Short.valueOf(((Number) value).shortValue()) : Short.valueOf(String.valueOf(value));
        }
        if (Byte.class.equals(wrapperType)) {
            return value instanceof Number ? Byte.valueOf(((Number) value).byteValue()) : Byte.valueOf(String.valueOf(value));
        }
        if (Boolean.class.equals(wrapperType)) {
            return value instanceof Boolean ? value : Boolean.valueOf(String.valueOf(value));
        }
        if (Enum.class.isAssignableFrom(wrapperType)) {
            return Enum.valueOf((Class<Enum>) wrapperType, normalizeEnumName(String.valueOf(value)));
        }
        if (Collection.class.isAssignableFrom(wrapperType)) {
            return toCollection(value);
        }
        if (Duration.class.equals(wrapperType)) {
            if (value instanceof Number) {
                return Duration.ofMillis(((Number) value).longValue());
            }
            return Duration.parse(String.valueOf(value));
        }
        if (applicationContext != null && value instanceof String) {
            return applicationContext.getBean(String.valueOf(value), targetType);
        }
        return value;
    }

    private Collection<String> toCollection(Object value) {
        if (value instanceof Collection) {
            return (Collection<String>) value;
        }
        String text = String.valueOf(value);
        List<String> items = new ArrayList<String>();
        if (text.trim().length() == 0) {
            return items;
        }
        String[] parts = text.split(",");
        for (String part : parts) {
            items.add(part.trim());
        }
        return items;
    }

    private Class<?> wrapperType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (int.class.equals(type)) {
            return Integer.class;
        }
        if (long.class.equals(type)) {
            return Long.class;
        }
        if (double.class.equals(type)) {
            return Double.class;
        }
        if (float.class.equals(type)) {
            return Float.class;
        }
        if (boolean.class.equals(type)) {
            return Boolean.class;
        }
        if (short.class.equals(type)) {
            return Short.class;
        }
        if (byte.class.equals(type)) {
            return Byte.class;
        }
        if (char.class.equals(type)) {
            return Character.class;
        }
        return type;
    }

    private String normalize(String provider) {
        return provider == null ? "" : provider.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeEnumName(String value) {
        return value.trim().replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private static Map<String, String> defaultStoreClasses() {
        return new LinkedHashMap<String, String>();
    }
}
