package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore;

/**
 * Weaviate 向量库工厂：dev.langchain4j.store.embedding.weaviate.WeaviateEmbeddingStore builder 链。
 * weaviate 自动处理维度，不校验 dimension 参数。
 */
public class Langchain4jWeaviateModelFactory implements ModelFactory {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 8080;
    private static final String DEFAULT_SCHEME = "http";
    private static final String DEFAULT_OBJECT_CLASS = "MagicLangchain4j";

    @Override
    public String provider() {
        return "weaviate";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        return WeaviateEmbeddingStore.builder()
                .scheme(o.string("scheme", DEFAULT_SCHEME))
                .host(o.string("host", DEFAULT_HOST))
                .port(o.integer("port", DEFAULT_PORT))
                .apiKey(o.string("api-key", null))
                .objectClass(o.string("object-class", DEFAULT_OBJECT_CLASS))
                .useGrpcForInserts(o.bool("use-grpc-for-inserts", false))
                .securedGrpc(o.bool("secured-grpc", false))
                .grpcPort(nullIfZero(o.integer("grpc-port", null)))
                .avoidDups(o.bool("avoid-dups", false))
                .consistencyLevel(o.string("consistency-level", null))
                .metadataKeys(o.stringList("metadata-keys", null))
                .build();
    }

    private static Integer nullIfZero(Integer value) {
        return value != null && value == 0 ? null : value;
    }
}
