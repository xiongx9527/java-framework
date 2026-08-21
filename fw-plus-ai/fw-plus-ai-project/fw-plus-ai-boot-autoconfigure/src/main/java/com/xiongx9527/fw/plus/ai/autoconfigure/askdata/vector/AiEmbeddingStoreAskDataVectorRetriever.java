package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataEmbeddingModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRequest;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelResult;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelSelection;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelUsage;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingModel;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingRequest;
import com.xiongx9527.fw.plus.ai.model.AiEmbeddingResult;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingMatch;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 通过中立向量存储 API 检索 AskData 文档。 */
public class AiEmbeddingStoreAskDataVectorRetriever implements AskDataVectorRetriever {

    private final AiEmbeddingStoreRegistry embeddingStoreRegistry;
    private final AskDataModelRegistry modelRegistry;
    private final AiModelRegistry neutralModelRegistry;
    private final AskDataModelRouter modelRouter;
    private final AskDataProperties properties;
    private final String storeKey;

    public AiEmbeddingStoreAskDataVectorRetriever(AiEmbeddingStoreRegistry embeddingStoreRegistry,
            AskDataModelRegistry modelRegistry, String storeKey) {
        this(embeddingStoreRegistry, modelRegistry, null, null, null, storeKey);
    }

    public AiEmbeddingStoreAskDataVectorRetriever(AiEmbeddingStoreRegistry embeddingStoreRegistry,
            AskDataModelRegistry modelRegistry, AiModelRegistry neutralModelRegistry, AskDataModelRouter modelRouter,
            AskDataProperties properties, String storeKey) {
        this.embeddingStoreRegistry = embeddingStoreRegistry;
        this.modelRegistry = modelRegistry;
        this.neutralModelRegistry = neutralModelRegistry;
        this.modelRouter = modelRouter;
        this.properties = properties;
        this.storeKey = storeKey;
    }

    @Override
    public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
        return retrieveWithResult(query).getDocuments();
    }

    @Override
    public AskDataVectorRetrievalResult retrieveWithResult(AskDataVectorQuery query) {
        if (query == null || isBlank(query.getQuestion())) {
            return AskDataVectorRetrievalResult.documents(Collections.<AskDataRetrievedDocument>emptyList());
        }
        AiEmbeddingStore store = resolveStore();
		if (!query.getFilters().isEmpty() && !store.supportsFilters()) {
			throw new AiEmbeddingStoreException("AskData 向量存储不支持授权过滤: " + store.getKey());
		}
        EmbeddingExecution embedding = embed(query);
        if (embedding.values == null || embedding.values.length == 0) {
            return new AskDataVectorRetrievalResult(Collections.<AskDataRetrievedDocument>emptyList(),
                    embedding.modelAttempts, embedding.actualModelKey);
        }
        AiEmbeddingSearchResult result = store.search(new AiEmbeddingQuery(new AiEmbeddingVector(embedding.values),
					query.getTopK() <= 0 ? 5 : query.getTopK(), 0.0D, query.getFilters()));
        if (result == null || result.getMatches().isEmpty()) {
            return new AskDataVectorRetrievalResult(Collections.<AskDataRetrievedDocument>emptyList(),
                    embedding.modelAttempts, embedding.actualModelKey);
        }
        List<AskDataRetrievedDocument> documents = new ArrayList<AskDataRetrievedDocument>();
        for (AiEmbeddingMatch match : result.getMatches()) {
            AiEmbeddingDocument document = match == null ? null : match.getDocument();
            if (document != null && document.getText() != null) {
                documents.add(new AskDataRetrievedDocument(document.getText(), document.getMetadata()));
            }
        }
        return new AskDataVectorRetrievalResult(documents, embedding.modelAttempts, embedding.actualModelKey);
    }

    private EmbeddingExecution embed(AskDataVectorQuery query) {
        if (modelRouter != null && query.getProjectContext() != null) {
            return embedNeutral(query);
        }
        AskDataEmbeddingModel embeddingModel = modelRegistry == null ? null
                : modelRegistry.getEmbeddingModel(query.getEmbeddingModelKey());
        if (embeddingModel == null) {
            return EmbeddingExecution.empty();
        }
        List<Float> values = embeddingModel.embed(query.getQuestion());
        return new EmbeddingExecution(values == null || values.isEmpty() ? null : toArray(values),
                Collections.<AskDataModelAttemptTrace>emptyList(), null);
    }

    private EmbeddingExecution embedNeutral(AskDataVectorQuery query) {
        AskDataModelRequest routeRequest = embeddingRouteRequest(query);
        AskDataModelResult routed = modelRouter.route(routeRequest);
        List<AskDataModelAttemptTrace> attempts = new ArrayList<AskDataModelAttemptTrace>();
        while (true) {
            attempts.add(AskDataModelAttemptTrace.attempted(routed));
            try {
                AiEmbeddingResult result = executeEmbedding(query, routed.getModelKey());
                if (result == null || result.getEmbeddings().size() != 1) {
                    attempts.add(AskDataModelAttemptTrace.failed(routed,
                            AiModelException.MODEL_ERROR, "INVALID_MODEL_RESPONSE", null));
                    throw new AskDataVectorRetrievalException(
                            new AiModelException("Embedding 模型必须为单条输入返回一个向量"), attempts);
                }
                String actualModelKey = isBlank(result.getActualModelKey())
                        ? routed.getModelKey() : result.getActualModelKey();
                attempts.add(AskDataModelAttemptTrace.succeeded(routed, actualModelKey, usage(result)));
                return new EmbeddingExecution(result.getEmbeddings().get(0), attempts, actualModelKey);
            } catch (AskDataVectorRetrievalException ex) {
                throw ex;
            } catch (AiModelException ex) {
                boolean retryableInfrastructureFailure = AiModelException.MODEL_INFRASTRUCTURE_FAILURE
                        .equals(ex.getErrorCode()) && ex.isRetryable();
                if (!retryableInfrastructureFailure || !fallbackEnabled()) {
                    attempts.add(AskDataModelAttemptTrace.failed(routed,
                            ex.getErrorCode(), retryableInfrastructureFailure
                                    ? "FALLBACK_DISABLED" : "NON_RETRYABLE_FAILURE", null));
                    throw new AskDataVectorRetrievalException(ex, attempts);
                }
                try {
                    AskDataModelResult next = modelRouter.fallback(routeRequest,
                            AskDataModelResult.infrastructureFailure(routed, safeMessage(ex)));
                    attempts.add(AskDataModelAttemptTrace.failed(routed,
                            ex.getErrorCode(), "RETRYABLE_INFRASTRUCTURE_FAILURE", null));
                    routed = next;
                } catch (IllegalArgumentException noCandidate) {
                    attempts.add(AskDataModelAttemptTrace.failed(routed,
                            ex.getErrorCode(), "CANDIDATES_EXHAUSTED", null));
                    throw new AskDataVectorRetrievalException(ex, attempts);
                }
            }
        }
    }

    private boolean fallbackEnabled() {
        return properties != null && properties.getModel() != null && properties.getModel().isFallbackEnabled();
    }

    private AiEmbeddingModel neutralEmbeddingModel(String modelKey) {
        if (isBlank(modelKey) || !neutralModelRegistry.contains(modelKey)) {
            throw AiModelException.requestRejected("未找到 Embedding 模型: " + modelKey, null);
        }
        AiModel model = neutralModelRegistry.get(modelKey);
        if (!(model instanceof AiEmbeddingModel) || model.getType() != AiModelType.EMBEDDING) {
            throw AiModelException.requestRejected("模型不支持类型化 Embedding 契约: " + modelKey, null);
        }
        return (AiEmbeddingModel) model;
    }

    private AiEmbeddingResult executeEmbedding(AskDataVectorQuery query, String modelKey) {
        if (neutralModelRegistry != null) {
            return neutralEmbeddingModel(modelKey).embed(new AiEmbeddingRequest(
                    Collections.singletonList(query.getQuestion()), Collections.<String, Object>emptyMap(),
                    requestTimeoutMillis()));
        }
        AskDataEmbeddingModel legacy = modelRegistry == null ? null : modelRegistry.getEmbeddingModel(modelKey);
        if (legacy == null) {
            throw AiModelException.requestRejected("未找到 Embedding 模型: " + modelKey, null);
        }
        List<Float> values = legacy.embed(query.getQuestion());
        if (values == null || values.isEmpty()) {
            throw new AiModelException("Embedding 模型未返回向量");
        }
        return new AiEmbeddingResult(Collections.singletonList(toArray(values)), modelKey, null);
    }

    private AskDataModelRequest embeddingRouteRequest(AskDataVectorQuery query) {
        AskDataModelSelection selection = query.getModelSelection() == null
                ? AskDataModelSelection.empty() : query.getModelSelection();
        List<String> fallbacks = new ArrayList<String>(selection.getFallbackModelKeys());
        if (properties != null && properties.getModel() != null
                && properties.getModel().getEmbeddingCandidates() != null) {
            for (String candidate : properties.getModel().getEmbeddingCandidates()) {
                String candidateKey = normalize(candidate);
                if (candidateKey.length() > 0 && !fallbacks.contains(candidateKey)) fallbacks.add(candidateKey);
            }
        }
        return new AskDataModelRequest(query.getProjectContext(), selection.getRequestModelKey(),
                selection.getProjectModelKey(), selection.getDatasourceModelKey(), selection.getAgentTaskModelKey(),
                selection.getPolicyModelKey(), fallbacks, defaultEmbeddingModelKey(), selection.getPolicyTags());
    }

    private AskDataModelUsage usage(AiEmbeddingResult result) {
        if (result == null || result.getUsage() == null || result.getUsage().isUnknown()) {
            return AskDataModelUsage.unknown();
        }
        return new AskDataModelUsage(result.getUsage().getInputTokens(), result.getUsage().getOutputTokens(),
                result.getUsage().getTotalTokens(), null);
    }

    private String defaultEmbeddingModelKey() {
        return normalize(properties == null || properties.getModel() == null
                ? null : properties.getModel().getDefaultEmbeddingModel());
    }

    private long requestTimeoutMillis() {
        Integer timeout = properties == null || properties.getModel() == null
                ? null : properties.getModel().getRequestTimeoutMillis();
        return timeout == null ? 0L : Math.max(0L, timeout.longValue());
    }

    private String safeMessage(AiModelException exception) {
        return isBlank(exception.getMessage()) ? "Embedding 模型执行失败" : exception.getMessage();
    }

    private AiEmbeddingStore resolveStore() {
        if (embeddingStoreRegistry == null) {
            throw new IllegalStateException("AskData 未配置默认向量存储注册表");
        }
        AiEmbeddingStore store = isBlank(storeKey) ? embeddingStoreRegistry.getDefault()
                : embeddingStoreRegistry.get(storeKey);
        if (store == null) {
            String key = isBlank(storeKey) ? embeddingStoreRegistry.getDefaultKey() : storeKey;
            throw new IllegalStateException("AskData 未找到向量存储: "
                    + (isBlank(key) ? "默认向量存储" : key));
        }
        return store;
    }

    private float[] toArray(List<Float> values) {
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            Float value = values.get(i);
            result[i] = value == null ? 0.0F : value.floatValue();
        }
        return result;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static final class EmbeddingExecution {

        private final float[] values;
        private final List<AskDataModelAttemptTrace> modelAttempts;
        private final String actualModelKey;

        private EmbeddingExecution(float[] values, List<AskDataModelAttemptTrace> modelAttempts,
                String actualModelKey) {
            this.values = values;
            this.modelAttempts = modelAttempts;
            this.actualModelKey = actualModelKey;
        }

        private static EmbeddingExecution empty() {
            return new EmbeddingExecution(null, Collections.<AskDataModelAttemptTrace>emptyList(), null);
        }
    }
}
