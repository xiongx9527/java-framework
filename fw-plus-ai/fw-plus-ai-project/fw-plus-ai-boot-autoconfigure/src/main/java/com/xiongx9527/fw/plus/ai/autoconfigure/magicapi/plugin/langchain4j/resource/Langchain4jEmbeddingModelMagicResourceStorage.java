package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;

import java.util.Collections;
import java.util.Set;

/** embedding-model 资源存储：folder=langchain4j-embedding-model。 */
public class Langchain4jEmbeddingModelMagicResourceStorage
        extends AbstractLangchain4jModelResourceStorage<Langchain4jEmbeddingModelInfo> {

    public Langchain4jEmbeddingModelMagicResourceStorage() {
        this(Collections.<String>emptySet());
    }

    public Langchain4jEmbeddingModelMagicResourceStorage(Set<String> providers) {
        super(Langchain4jModelTypes.EMBEDDING_MODEL, Langchain4jEmbeddingModelInfo.class, providers);
    }
}
