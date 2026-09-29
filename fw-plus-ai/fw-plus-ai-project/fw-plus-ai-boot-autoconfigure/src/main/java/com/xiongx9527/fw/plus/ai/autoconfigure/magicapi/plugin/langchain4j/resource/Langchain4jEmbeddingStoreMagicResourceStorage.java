package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;

import java.util.Collections;
import java.util.Set;

/** embedding-store 资源存储：folder=langchain4j-embedding-store；额外必填 embeddingModelKey。 */
public class Langchain4jEmbeddingStoreMagicResourceStorage
        extends AbstractLangchain4jModelResourceStorage<Langchain4jEmbeddingStoreInfo> {

    public Langchain4jEmbeddingStoreMagicResourceStorage() {
        this(Collections.<String>emptySet());
    }

    public Langchain4jEmbeddingStoreMagicResourceStorage(Set<String> providers) {
        super(Langchain4jModelTypes.EMBEDDING_STORE, Langchain4jEmbeddingStoreInfo.class, providers);
    }

    @Override
    public void validate(Langchain4jEmbeddingStoreInfo entity) {
        super.validate(entity);
        validateRequired(entity.getEmbeddingModelKey(), "embeddingModelKey");
    }
}
