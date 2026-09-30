package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;

/**
 * PGVector（PostgreSQL + pgvector 扩展）向量库工厂。
 * 必填无；常用 {@code host/port/user/password/database/table/dimension}。
 */
public class Langchain4jPgVectorModelFactory implements ModelFactory {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 5432;
    private static final String DEFAULT_TABLE = "embeddings";

    @Override
    public String provider() {
        return "pgvector";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        return PgVectorEmbeddingStore.builder()
                .host(o.string("host", DEFAULT_HOST))
                .port(o.integer("port", DEFAULT_PORT))
                .user(o.string("user", "postgres"))
                .password(o.string("password", null))
                .database(o.string("database", "postgres"))
                .table(o.string("table", DEFAULT_TABLE))
                .dimension(dimension)
                .dropTableFirst(o.bool("drop-table-first", Boolean.FALSE))
                .skipCreateVectorExtension(o.bool("skip-create-vector-extension", Boolean.FALSE))
                .build();
    }
}
