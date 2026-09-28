package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.model.embedding.EmbeddingModel;

/** {@code langchain4j-embedding-model} 数据源的实例持有者。 */
public class MagicDynamicLangchain4jEmbeddingModelDataSource
        extends AbstractMagicDynamicLangchain4jDataSource<EmbeddingModel> {

    public MagicDynamicLangchain4jEmbeddingModelDataSource() {
        super(Langchain4jModelTypes.EMBEDDING_MODEL);
    }
}
