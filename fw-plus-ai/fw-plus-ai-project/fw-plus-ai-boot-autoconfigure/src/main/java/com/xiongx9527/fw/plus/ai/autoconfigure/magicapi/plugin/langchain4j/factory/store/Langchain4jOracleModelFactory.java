package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.oracle.CreateOption;
import dev.langchain4j.store.embedding.oracle.OracleEmbeddingStore;
import oracle.jdbc.pool.OracleDataSource;

import java.sql.SQLException;

/**
 * Oracle（23ai Vector 列）向量库工厂：必填 {@code url}，常用 {@code user/password/table/dimension}。
 */
public class Langchain4jOracleModelFactory implements ModelFactory {

    private static final String DEFAULT_TABLE = "embeddings";

    @Override
    public String provider() {
        return "oracle";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        OracleDataSource dataSource;
        try {
            dataSource = new OracleDataSource();
            dataSource.setURL(o.required("url"));
            dataSource.setUser(o.string("user", null));
            dataSource.setPassword(o.string("password", null));
        } catch (SQLException e) {
            throw new IllegalStateException("创建 Oracle 数据源失败：" + e.getMessage(), e);
        }
        return OracleEmbeddingStore.builder()
                .dataSource(dataSource)
                .embeddingTable(o.string("table", DEFAULT_TABLE), CreateOption.CREATE_IF_NOT_EXISTS)
                .exactSearch(o.bool("exact-search", Boolean.FALSE))
                .build();
    }
}
