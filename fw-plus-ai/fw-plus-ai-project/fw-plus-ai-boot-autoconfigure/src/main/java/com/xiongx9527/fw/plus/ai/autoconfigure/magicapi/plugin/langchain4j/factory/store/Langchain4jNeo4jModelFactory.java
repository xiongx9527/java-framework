package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.community.store.embedding.neo4j.Neo4jEmbeddingStore;

/**
 * Neo4j 向量库工厂：Neo4jEmbeddingStore（Lombok @Builder 构造）。
 * 走 dimension 回退链（Neo4j 建向量索引需要显式 dimension）。
 */
public class Langchain4jNeo4jModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "neo4j";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        Integer dimension = EmbeddingStoreDimensions.resolve(o, embeddingModel);
        return Neo4jEmbeddingStore.builder()
                .withBasicAuth(o.string("uri", "bolt://localhost:7687"),
                        o.string("username", "neo4j"),
                        o.string("password", "password"))
                .dimension(dimension)
                .label(o.string("label", "Document"))
                .embeddingProperty(o.string("embedding-property", "embedding"))
                .idProperty(o.string("id-property", "id"))
                .metadataPrefix(o.string("metadata-prefix", ""))
                .textProperty(o.string("text-property", "text"))
                .indexName(o.string("index-name", "vector"))
                .databaseName(o.string("database-name", "neo4j"))
                .build();
    }
}
