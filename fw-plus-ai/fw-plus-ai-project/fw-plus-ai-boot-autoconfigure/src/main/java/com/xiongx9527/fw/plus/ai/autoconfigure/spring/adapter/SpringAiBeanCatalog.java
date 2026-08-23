package com.xiongx9527.fw.plus.ai.autoconfigure.spring.adapter;

import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.util.ClassUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

final class SpringAiBeanCatalog {

    private static final String CHAT_MODEL = "org.springframework.ai.chat.model.ChatModel";
    private static final String EMBEDDING_MODEL = "org.springframework.ai.embedding.EmbeddingModel";
    private static final String DASHSCOPE_CHAT_MODEL = "com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel";
    private static final String DASHSCOPE_EMBEDDING_MODEL =
            "com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel";
    private static final boolean INCLUDE_NON_SINGLETONS = true;
    private static final boolean ALLOW_EAGER_INIT = false;

    private final ListableBeanFactory beanFactory;
    private final Map<AiModelType, Class<?>> modelTypes = new EnumMap<>(AiModelType.class);
    private final Map<AiModelType, Set<String>> modelNames = new EnumMap<>(AiModelType.class);
    private final Map<AiModelType, Set<String>> dashScopeNames = new EnumMap<>(AiModelType.class);

    SpringAiBeanCatalog(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
        register(AiModelType.CHAT, CHAT_MODEL, DASHSCOPE_CHAT_MODEL);
        register(AiModelType.EMBEDDING, EMBEDDING_MODEL, DASHSCOPE_EMBEDDING_MODEL);
    }

    boolean contains(String beanName, AiModelType type) {
        return names(type).contains(beanName);
    }

    boolean isDashScope(String beanName, AiModelType type) {
        Set<String> names = dashScopeNames.get(type);
        return names != null && names.contains(beanName);
    }

    int candidateCount(AiModelType type) {
        return names(type).size();
    }

    String beanRef(String beanName) {
        if (beanName == null) {
            return "none";
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(beanName.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(12);
            for (int index = 0; index < 6; index++) {
                value.append(String.format("%02x", digest[index] & 0xff));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException ignored) {
            return "unavailable";
        }
    }

    Object get(String beanName, AiModelType type) {
        Class<?> modelType = modelTypes.get(type);
        if (modelType == null || !contains(beanName, type)) {
            throw new AiModelException("Spring AI model bean is missing or has the wrong type: type=" + type
                    + ", beanRef=" + beanRef(beanName) + ", candidates=" + candidateCount(type));
        }
        try {
            return beanFactory.getBean(beanName, modelType);
        } catch (RuntimeException failure) {
            throw new AiModelException("Spring AI model bean could not be created: type=" + type
                    + ", beanRef=" + beanRef(beanName) + ", candidates=" + candidateCount(type),
                    new IllegalStateException("Bean failure type: " + failure.getClass().getName()));
        }
    }

    private Set<String> names(AiModelType type) {
        Set<String> names = modelNames.get(type);
        return names == null ? Collections.emptySet() : names;
    }

    private void register(AiModelType type, String modelClassName, String dashScopeClassName) {
        Class<?> modelType = optionalClass(modelClassName);
        if (modelType == null) {
            return;
        }
        modelTypes.put(type, modelType);
        modelNames.put(type, namesForType(modelType));
        Class<?> dashScopeType = optionalClass(dashScopeClassName);
        if (dashScopeType != null) {
            dashScopeNames.put(type, namesForType(dashScopeType));
        }
    }

    private Set<String> namesForType(Class<?> type) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
                beanFactory.getBeanNamesForType(type, INCLUDE_NON_SINGLETONS, ALLOW_EAGER_INIT))));
    }

    private Class<?> optionalClass(String className) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return ClassUtils.forName(className, classLoader);
        } catch (ClassNotFoundException | LinkageError ignored) {
            return null;
        }
    }
}
