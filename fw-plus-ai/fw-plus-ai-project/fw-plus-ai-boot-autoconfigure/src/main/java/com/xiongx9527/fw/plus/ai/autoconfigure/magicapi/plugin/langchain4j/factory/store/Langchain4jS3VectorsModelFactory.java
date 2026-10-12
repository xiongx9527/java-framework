package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.community.store.embedding.s3.S3VectorsEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * AWS S3 Vectors 向量库工厂：必填 {@code region/vector-bucket}，常用 {@code index-name}。
 * 凭据走 AWS 默认链（环境变量 / ~/.aws 等）。
 */
public class Langchain4jS3VectorsModelFactory implements ModelFactory {

    private static final String DEFAULT_INDEX_NAME = "magic-langchain4j";

    @Override
    public String provider() {
        return "s3-vectors";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        return S3VectorsEmbeddingStore.builder()
                .region(o.required("region"))
                .vectorBucketName(o.required("vector-bucket"))
                .indexName(o.string("index-name", DEFAULT_INDEX_NAME))
                .createIndexIfNotExists(o.bool("create-index-if-not-exists", Boolean.TRUE))
                .build();
    }
}
