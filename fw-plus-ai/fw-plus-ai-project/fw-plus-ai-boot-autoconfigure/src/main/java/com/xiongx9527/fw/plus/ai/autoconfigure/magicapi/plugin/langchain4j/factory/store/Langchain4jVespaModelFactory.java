package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.vespa.VespaEmbeddingStore;

/**
 * Vespa 向量库工厂：必填 {@code url}，常用 {@code namespace/document-type/cluster-name}。
 */
public class Langchain4jVespaModelFactory implements ModelFactory {

    private static final String DEFAULT_NAMESPACE = "default";
    private static final String DEFAULT_DOCUMENT_TYPE = "embeddings";

    @Override
    public String provider() {
        return "vespa";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        return VespaEmbeddingStore.builder()
                .url(o.required("url"))
                .namespace(o.string("namespace", DEFAULT_NAMESPACE))
                .documentType(o.string("document-type", DEFAULT_DOCUMENT_TYPE))
                .clusterName(o.string("cluster-name", null))
                .rankProfile(o.string("rank-profile", null))
                .certPath(o.string("cert-path", null))
                .keyPath(o.string("key-path", null))
                .avoidDups(o.bool("avoid-dups", Boolean.FALSE))
                .logRequests(o.bool("log-requests", null))
                .logResponses(o.bool("log-responses", null))
                .build();
    }
}
