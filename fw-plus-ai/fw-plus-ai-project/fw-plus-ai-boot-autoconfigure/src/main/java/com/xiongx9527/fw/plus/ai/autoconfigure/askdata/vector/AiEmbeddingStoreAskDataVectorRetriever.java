package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataEmbeddingModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRequest;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelResult;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelRouter;
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
        if (query == null || isBlank(query.getQuestion())) {
            return Collections.emptyList();
        }
        AiEmbeddingStore store = resolveStore();
		if (!query.getFilters().isEmpty() && !store.supportsFilters()) {
			throw new AiEmbeddingStoreException("AskData 向量存储不支持授权过滤: " + store.getKey());
		}
        float[] values = embed(query);
        if (values == null || values.length == 0) {
            return Collections.emptyList();
        }
        AiEmbeddingSearchResult result = store.search(new AiEmbeddingQuery(new AiEmbeddingVector(values),
					query.getTopK() <= 0 ? 5 : query.getTopK(), 0.0D, query.getFilters()));
        if (result == null || result.getMatches().isEmpty()) {
            return Collections.emptyList();
        }
        List<AskDataRetrievedDocument> documents = new ArrayList<AskDataRetrievedDocument>();
        for (AiEmbeddingMatch match : result.getMatches()) {
            AiEmbeddingDocument document = match == null ? null : match.getDocument();
            if (document != null && document.getText() != null) {
                documents.add(new AskDataRetrievedDocument(document.getText(), document.getMetadata()));
            }
        }
        return documents;
    }

    private float[] embed(AskDataVectorQuery query) {
        if (neutralModelRegistry != null && modelRouter != null && query.getProjectContext() != null) {
            return embedNeutral(query);
        }
        AskDataEmbeddingModel embeddingModel = modelRegistry == null ? null
                : modelRegistry.getEmbeddingModel(query.getEmbeddingModelKey());
        if (embeddingModel == null) {
            return null;
        }
        List<Float> values = embeddingModel.embed(query.getQuestion());
        return values == null || values.isEmpty() ? null : toArray(values);
    }

    private float[] embedNeutral(AskDataVectorQuery query) {
        AskDataModelRequest routeRequest = embeddingRouteRequest(query);
        AskDataModelResult routed = modelRouter.route(routeRequest);
        while (true) {
            AiEmbeddingModel embeddingModel = neutralEmbeddingModel(routed.getModelKey());
            try {
                AiEmbeddingResult result = embeddingModel.embed(new AiEmbeddingRequest(
                        Collections.singletonList(query.getQuestion()), Collections.<String, Object>emptyMap(),
                        requestTimeoutMillis()));
                if (result == null || result.getEmbeddings().size() != 1) {
                    throw new AiModelException("Embedding 模型必须为单条输入返回一个向量");
                }
                return result.getEmbeddings().get(0);
            } catch (AiModelException ex) {
                if (!AiModelException.MODEL_INFRASTRUCTURE_FAILURE.equals(ex.getErrorCode()) || !ex.isRetryable()
                        || properties == null || properties.getModel() == null
                        || !properties.getModel().isFallbackEnabled()) {
                    throw ex;
                }
                routed = modelRouter.fallback(routeRequest,
                        AskDataModelResult.infrastructureFailure(routed, safeMessage(ex)));
            }
        }
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

    private AskDataModelRequest embeddingRouteRequest(AskDataVectorQuery query) {
        String[] routeKeys = { normalize(query.getEmbeddingModelKey()), "", "", "", defaultEmbeddingModelKey() };
        int routeIndex = 1;
        if (properties != null && properties.getModel() != null
                && properties.getModel().getEmbeddingCandidates() != null) {
            for (String candidate : properties.getModel().getEmbeddingCandidates()) {
                String candidateKey = normalize(candidate);
                if (candidateKey.length() > 0 && !candidateKey.equals(routeKeys[0])
                        && !candidateKey.equals(routeKeys[4]) && routeIndex < 4) {
                    routeKeys[routeIndex++] = candidateKey;
                }
            }
        }
        return new AskDataModelRequest(query.getProjectContext(), routeKeys[0], routeKeys[1], routeKeys[2],
                routeKeys[3], routeKeys[4]);
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
}
