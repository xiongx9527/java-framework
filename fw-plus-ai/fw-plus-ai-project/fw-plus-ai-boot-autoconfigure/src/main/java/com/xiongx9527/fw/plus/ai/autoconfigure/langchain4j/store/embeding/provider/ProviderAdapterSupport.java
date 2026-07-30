package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider;

import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.EmbeddingStoreException;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

final class ProviderAdapterSupport {

    private ProviderAdapterSupport() {
    }

    static boolean supports(String provider, String expected) {
        return provider != null && expected.equalsIgnoreCase(provider);
    }

    static String stringProperty(EmbeddingStoreDefinition definition, String name) {
        Object value = definition.getProperties().get(name);
        return value == null ? null : String.valueOf(value);
    }

    static Integer integerProperty(EmbeddingStoreDefinition definition, String name) {
        Object value = definition.getProperties().get(name);
        if (value == null) {
            return "dimension".equals(name) ? definition.getDimension() : null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.valueOf(String.valueOf(value));
    }

    static Boolean booleanProperty(EmbeddingStoreDefinition definition, String name) {
        Object value = definition.getProperties().get(name);
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return Boolean.valueOf(String.valueOf(value));
    }

    static <T> T beanProperty(EmbeddingStoreDefinition definition, String name, Class<T> type,
                              ApplicationContext applicationContext) {
        Object value = definition.getProperties().get(name);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        String beanName = String.valueOf(value);
        if (beanName.trim().length() == 0) {
            return null;
        }
        if (applicationContext == null) {
            throw new IllegalArgumentException(name + " requires Spring ApplicationContext");
        }
        return applicationContext.getBean(beanName, type);
    }

    static <T> T requiredBeanProperty(EmbeddingStoreDefinition definition, String name, Class<T> type,
                                      ApplicationContext applicationContext) {
        T value = beanProperty(definition, name, type, applicationContext);
        if (value == null) {
            throw new IllegalArgumentException(name + " must be a bean name or " + type.getSimpleName() + " instance");
        }
        return value;
    }

    static <T extends Enum<T>> T enumProperty(EmbeddingStoreDefinition definition, String name, Class<T> type) {
        Object value = definition.getProperties().get(name);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return Enum.valueOf(type, String.valueOf(value).trim().toUpperCase(Locale.ROOT));
    }

    static String requiredStringProperty(EmbeddingStoreDefinition definition, String name) {
        String value = stringProperty(definition, name);
        if (value == null || value.trim().length() == 0) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    static Integer requiredIntegerProperty(EmbeddingStoreDefinition definition, String name) {
        Integer value = integerProperty(definition, name);
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        return value;
    }

    static void requireOneStringProperty(EmbeddingStoreDefinition definition, String firstName, String secondName) {
        String first = stringProperty(definition, firstName);
        String second = stringProperty(definition, secondName);
        if ((first == null || first.trim().length() == 0) && (second == null || second.trim().length() == 0)) {
            throw new IllegalArgumentException(firstName + " or " + secondName + " must not be blank");
        }
    }

    static Collection<String> metadataKeys(EmbeddingStoreDefinition definition) {
        Object value = definition.getProperties().get("metadataKeys");
        if (value == null) {
            return definition.getMetadataKeys();
        }
        if (!(value instanceof Collection)) {
            throw new IllegalArgumentException("metadataKeys must be a collection");
        }
        List<String> metadataKeys = new ArrayList<String>();
        for (Object item : (Collection<?>) value) {
            metadataKeys.add(String.valueOf(item));
        }
        return metadataKeys;
    }

    static EmbeddingStoreException exception(EmbeddingStoreDefinition definition, Exception cause) {
        String provider = definition == null ? null : definition.getProvider();
        String key = definition == null ? null : definition.getKey();
        return new EmbeddingStoreException("Failed to create embedding store: provider=" + provider + ", key=" + key, cause);
    }
}
