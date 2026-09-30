package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.mariadb.MariaDbEmbeddingStore;

/**
 * MariaDB（VECTOR 列）向量库工厂。
 * 常用 {@code url/user/password/table/dimension}，url 形如 jdbc:mariadb://host:3306/db。
 */
public class Langchain4jMariaDbModelFactory implements ModelFactory {

    private static final String DEFAULT_TABLE = "embeddings";

    @Override
    public String provider() {
        return "mariadb";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        return MariaDbEmbeddingStore.builder()
                .url(o.required("url"))
                .user(o.string("user", null))
                .password(o.string("password", null))
                .table(o.string("table", DEFAULT_TABLE))
                .dimension(dimension)
                .createTable(o.bool("create-table", Boolean.TRUE))
                .dropTableFirst(o.bool("drop-table-first", Boolean.FALSE))
                .build();
    }
}
