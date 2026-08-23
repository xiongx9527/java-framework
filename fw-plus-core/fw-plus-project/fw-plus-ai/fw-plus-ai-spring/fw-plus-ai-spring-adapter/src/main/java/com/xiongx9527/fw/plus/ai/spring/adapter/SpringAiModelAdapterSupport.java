package com.xiongx9527.fw.plus.ai.spring.adapter;

import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelDefinition;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class SpringAiModelAdapterSupport {

    private final String framework;
    private final List<SpringAiModelProviderAdapter> providers;

    SpringAiModelAdapterSupport(String framework, List<SpringAiModelProviderAdapter> providers) {
        this.framework = framework;
        this.providers = providers == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(providers));
    }

    boolean supports(AiModelDefinition definition) {
        if (!validDefinition(definition)) {
            return false;
        }
        return candidates(definition).size() == 1;
    }

    AiModel create(AiModelDefinition definition) {
        if (definition == null || !framework.equals(definition.getFramework())) {
            throw new AiModelException("Spring AI adapter does not support " + context(definition));
        }
        if (isBlank(definition.getKey())) {
            throw new AiModelException("Spring AI model key must not be blank: " + context(definition));
        }
        if (isBlank(definition.getProvider())) {
            throw new AiModelException("Spring AI model provider must not be blank: " + context(definition));
        }
        if (!supportedType(definition.getType())) {
            throw new AiModelException("Spring AI adapter does not support model type: " + context(definition));
        }
        List<SpringAiModelProviderAdapter> candidates = candidates(definition);
        if (candidates.size() != 1) {
            throw new AiModelException("Expected one Spring AI provider adapter: "
                    + diagnosticContext(definition, candidates.size()));
        }
        Object nativeModel;
        try {
            nativeModel = candidates.get(0).create(definition);
        } catch (AiModelException exception) {
            throw safeModelException(exception, definition);
        } catch (RuntimeException exception) {
            throw new AiModelException("Spring AI provider adapter failed: " + context(definition),
                    sanitizedCause(exception));
        }
        if (definition.getType() == AiModelType.CHAT) {
            if (!(nativeModel instanceof ChatModel)) {
                throw new AiModelException("Spring AI provider returned a non-chat model: " + context(definition));
            }
            return new SpringAiChatModel(definition, (ChatModel) nativeModel);
        }
        if (!(nativeModel instanceof EmbeddingModel)) {
            throw new AiModelException("Spring AI provider returned a non-embedding model: " + context(definition));
        }
        return new SpringAiEmbeddingModel(definition, (EmbeddingModel) nativeModel);
    }

    private List<SpringAiModelProviderAdapter> candidates(AiModelDefinition definition) {
        List<SpringAiModelProviderAdapter> matches = new ArrayList<>();
        for (SpringAiModelProviderAdapter provider : providers) {
            if (provider == null) {
                continue;
            }
            try {
                if (provider.supports(definition)) {
                    matches.add(provider);
                }
            } catch (SpringAiProviderResolutionException failure) {
                throw new AiModelException("Spring AI provider resolution failed: " + context(definition)
                        + ", candidates=" + failure.getCandidateCount()
                        + ", beanRefs=" + failure.getBeanRefs());
            } catch (RuntimeException failure) {
                throw new AiModelException("Spring AI provider support check failed: "
                        + diagnosticContext(definition, matches.size()), sanitizedCause(failure));
            }
        }
        return matches;
    }

    private boolean validDefinition(AiModelDefinition definition) {
        return definition != null && framework.equals(definition.getFramework())
                && !isBlank(definition.getKey()) && !isBlank(definition.getProvider())
                && supportedType(definition.getType());
    }

    private boolean supportedType(AiModelType type) {
        return type == AiModelType.CHAT || type == AiModelType.EMBEDDING;
    }

    private String diagnosticContext(AiModelDefinition definition, int candidateCount) {
        StringBuilder value = new StringBuilder(context(definition)).append(", candidates=").append(candidateCount);
        Object beanName = definition == null ? null : definition.getProperties().get("beanName");
        if (beanName == null && definition != null) {
            beanName = definition.getProperties().get("bean-name");
        }
        if (beanName != null) {
            value.append(", beanRef=").append(beanRef(String.valueOf(beanName).trim()));
        }
        return value.toString();
    }

    private String beanRef(String beanName) {
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

    private AiModelException safeModelException(AiModelException failure, AiModelDefinition definition) {
        String message = "Spring AI provider adapter rejected model: " + context(definition);
        RuntimeException cause = sanitizedCause(failure);
        if (AiModelException.MODEL_INFRASTRUCTURE_FAILURE.equals(failure.getErrorCode()) || failure.isRetryable()) {
            return AiModelException.infrastructureFailure(message, cause);
        }
        if (AiModelException.MODEL_REQUEST_REJECTED.equals(failure.getErrorCode())) {
            return AiModelException.requestRejected(message, cause);
        }
        return new AiModelException(failure.getErrorCode(), false, message, cause);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static RuntimeException sanitizedCause(RuntimeException failure) {
        return new IllegalStateException("Upstream failure type: " + failure.getClass().getName());
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
}
