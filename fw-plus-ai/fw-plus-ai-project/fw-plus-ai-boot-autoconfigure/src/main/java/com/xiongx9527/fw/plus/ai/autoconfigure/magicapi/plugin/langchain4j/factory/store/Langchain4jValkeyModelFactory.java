package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.community.store.embedding.valkey.ValkeyEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import glide.api.GlideClient;
import glide.api.models.configuration.GlideClientConfiguration;
import glide.api.models.configuration.GlideClientConfiguration.GlideClientConfigurationBuilder;
import glide.api.models.configuration.NodeAddress;
import glide.api.models.configuration.ServerCredentials;

import java.util.concurrent.ExecutionException;

/**
 * Valkey 向量库工厂（Valkey GLIDE 客户端）：常用 {@code host/port/password/index-name/dimension}。
 */
public class Langchain4jValkeyModelFactory implements ModelFactory {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6379;
    private static final String DEFAULT_INDEX_NAME = "magic-langchain4j";

    @Override
    public String provider() {
        return "valkey";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        String host = o.string("host", DEFAULT_HOST);
        int port = o.integer("port", DEFAULT_PORT);
        GlideClientConfigurationBuilder<?, ?> config = GlideClientConfiguration.builder()
                .address(NodeAddress.builder().host(host).port(port).build());
        String password = o.string("password", null);
        if (password != null) {
            config.credentials(ServerCredentials.builder().password(password).build());
        }
        GlideClient client;
        try {
            client = GlideClient.createClient(config.build()).get();
        } catch (InterruptedException | ExecutionException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("创建 Valkey GLIDE 客户端失败：" + e.getMessage(), e);
        }
        return ValkeyEmbeddingStore.builder()
                .client(client)
                .dimension(dimension)
                .indexName(o.string("index-name", DEFAULT_INDEX_NAME))
                .prefix(o.string("prefix", null))
                .build();
    }
}
