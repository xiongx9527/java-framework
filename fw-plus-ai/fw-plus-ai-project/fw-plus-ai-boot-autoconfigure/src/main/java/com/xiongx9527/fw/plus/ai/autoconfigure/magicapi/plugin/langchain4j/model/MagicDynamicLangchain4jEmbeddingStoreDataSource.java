package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;

/** {@code langchain4j-embedding-store} 数据源的实例持有者。 */
public class MagicDynamicLangchain4jEmbeddingStoreDataSource
        extends AbstractMagicDynamicLangchain4jDataSource<EmbeddingStore<TextSegment>> {

    public MagicDynamicLangchain4jEmbeddingStoreDataSource() {
        super(Langchain4jModelTypes.EMBEDDING_STORE);
    }
}
