package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataEmbeddingModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model.AskDataModelRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingDocument;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingMatch;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingQuery;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingSearchResult;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingVector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 通过中立向量存储 API 检索 AskData 文档。 */
public class AiEmbeddingStoreAskDataVectorRetriever implements AskDataVectorRetriever {

    private final AiEmbeddingStoreRegistry embeddingStoreRegistry;
    private final AskDataModelRegistry modelRegistry;
    private final String storeKey;

    public AiEmbeddingStoreAskDataVectorRetriever(AiEmbeddingStoreRegistry embeddingStoreRegistry,
            AskDataModelRegistry modelRegistry, String storeKey) {
        this.embeddingStoreRegistry = embeddingStoreRegistry;
        this.modelRegistry = modelRegistry;
        this.storeKey = storeKey;
    }

    @Override
    public List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query) {
        if (query == null || isBlank(query.getQuestion())) {
            return Collections.emptyList();
        }
        AiEmbeddingStore store = resolveStore();
        AskDataEmbeddingModel embeddingModel = modelRegistry == null ? null : modelRegistry.getEmbeddingModel(null);
        if (embeddingModel == null) {
            return Collections.emptyList();
        }
        List<Float> values = embeddingModel.embed(query.getQuestion());
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        AiEmbeddingSearchResult result = store.search(new AiEmbeddingQuery(new AiEmbeddingVector(toArray(values)),
                query.getTopK() <= 0 ? 5 : query.getTopK(), 0.0D));
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

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
