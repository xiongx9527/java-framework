package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.store.embedding.sqlserver.SQLServerEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.sqlserver.CreateOption;
import dev.langchain4j.store.embedding.sqlserver.EmbeddingTable;

/**
 * SQL Server（2025 VECTOR 列）向量库工厂。
 * 常用 {@code host/port/database/user/password/table/dimension}。
 */
public class Langchain4jSqlServerModelFactory implements ModelFactory {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 1433;
    private static final String DEFAULT_TABLE = "embeddings";

    @Override
    public String provider() {
        return "sqlserver";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        return SQLServerEmbeddingStore.connectionBuilder()
                .host(o.string("host", DEFAULT_HOST))
                .port(o.integer("port", DEFAULT_PORT))
                .database(o.string("database", "master"))
                .userName(o.string("user", "sa"))
                .password(o.string("password", null))
                .embeddingTable(EmbeddingTable.builder()
                        .name(o.string("table", DEFAULT_TABLE))
                        .dimension(dimension)
                        .createOption(CreateOption.CREATE_IF_NOT_EXISTS)
                        .build())
                .build();
    }
}
