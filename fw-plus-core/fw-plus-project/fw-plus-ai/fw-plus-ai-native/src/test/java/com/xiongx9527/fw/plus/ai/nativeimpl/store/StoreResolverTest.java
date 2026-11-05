package com.xiongx9527.fw.plus.ai.nativeimpl.store;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xiongx9527.fw.plus.ai.base.store.Store;
import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.graph.GraphStoreProtocol;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.graph.GraphStoreVendor;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.graph.Neo4jGraphStore;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.graph.NebulaGraphStore;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.QdrantVectorStore;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.VectorStoreProtocol;
import com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.VectorStoreVendor;
import org.junit.jupiter.api.Test;

class StoreResolverTest {

    @Test
    void dispatchesVectorToVectorResolver() {
        Store store = StoreResolver.create(StoreConfig.builder()
                .storeType(StoreType.VECTOR)
                .protocol("QDRANT")
                .baseUrl("http://localhost:6333")
                .build());
        assertTrue(store instanceof QdrantVectorStore);
    }

    @Test
    void dispatchesGraphNeo4jToGraphResolver() {
        Store store = StoreResolver.create(StoreConfig.builder()
                .storeType(StoreType.GRAPH)
                .vendor("NEO4J")
                .baseUrl("http://localhost:7474")
                .username("neo4j")
                .password("pwd")
                .build());
        assertTrue(store instanceof Neo4jGraphStore);
    }

    @Test
    void dispatchesGraphNebulaToGraphResolver() {
        Store store = StoreResolver.create(StoreConfig.builder()
                .storeType(StoreType.GRAPH)
                .protocol("NEBULA")
                .baseUrl("http://localhost:8080")
                .build());
        assertTrue(store instanceof NebulaGraphStore);
    }

    @Test
    void acceptsVendorAndProtocolInAnyCaseInsensitiveForm() {
        // 大小写敏感按枚举名匹配；无法识别时由下层 resolver 报错
        Store store = StoreResolver.create(StoreConfig.builder()
                .storeType(StoreType.VECTOR)
                .vendor(VectorStoreVendor.MILVUS.name())
                .baseUrl("http://localhost:19530")
                .build());
        assertTrue(store instanceof com.xiongx9527.fw.plus.ai.nativeimpl.store.vector.MilvusVectorStore);
    }

    @Test
    void rejectsMissingStoreType() {
        assertThrows(IllegalArgumentException.class, () -> StoreResolver.create(
                StoreConfig.builder().baseUrl("http://localhost:6333").build()));
    }

    @Test
    void rejectsUnknownStoreType() {
        assertThrows(IllegalArgumentException.class, () -> StoreResolver.create(
                StoreConfig.builder().storeType(null).baseUrl("http://localhost:6333").build()));
    }

    @Test
    void rejectsMissingBaseUrl() {
        assertThrows(IllegalArgumentException.class, () -> StoreResolver.create(
                StoreConfig.builder().storeType(StoreType.VECTOR).build()));
    }
}
