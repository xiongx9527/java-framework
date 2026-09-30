package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.clickhouse.client.api.Client;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.community.store.embedding.clickhouse.ClickHouseEmbeddingStore;
import dev.langchain4j.community.store.embedding.clickhouse.ClickHouseSettings;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * ClickHouse 向量库工厂：必填 {@code url}（形如 http://host:8123/db），常用 {@code user/password/table/dimension}。
 */
public class Langchain4jClickHouseModelFactory implements ModelFactory {

    private static final String DEFAULT_TABLE = "embeddings";

    @Override
    public String provider() {
        return "clickhouse";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        Client client = new Client.Builder()
                .addEndpoint(o.required("url"))
                .setUsername(o.string("user", "default"))
                .setPassword(o.string("password", ""))
                .build();
        ClickHouseSettings settings = ClickHouseSettings.builder()
                .table(o.string("table", DEFAULT_TABLE))
                .dimension(dimension)
                .build();
        return ClickHouseEmbeddingStore.builder()
                .client(client)
                .settings(settings)
                .build();
    }
}
