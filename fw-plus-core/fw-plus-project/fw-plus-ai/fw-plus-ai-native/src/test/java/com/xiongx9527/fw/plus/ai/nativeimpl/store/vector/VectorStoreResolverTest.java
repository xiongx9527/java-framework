package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class VectorStoreResolverTest {

    @Test
    void resolvesProtocolToDedicatedStore() {
        assertTrue(VectorStoreResolver.create(VectorStoreConfig.builder()
                .protocol(VectorStoreProtocol.QDRANT)
                .baseUrl("http://localhost:6333")
                .build()) instanceof QdrantVectorStore);
        assertTrue(VectorStoreResolver.create(VectorStoreConfig.builder()
                .protocol(VectorStoreProtocol.MILVUS)
                .baseUrl("http://localhost:19530")
                .build()) instanceof MilvusVectorStore);
    }

    @Test
    void createsStoresThroughUnifiedFactory() {
        assertTrue(VectorStoreResolver.create(
                config(VectorStoreProtocol.QDRANT, VectorStoreVendor.QDRANT))
                instanceof QdrantVectorStore);
        assertTrue(VectorStoreResolver.create(
                config(VectorStoreProtocol.MILVUS, VectorStoreVendor.MILVUS))
                instanceof MilvusVectorStore);
    }

    @Test
    void protocolWinsWhenVendorAndProtocolAreBothProvided() {
        assertTrue(VectorStoreResolver.create(
                config(VectorStoreProtocol.MILVUS, VectorStoreVendor.QDRANT))
                instanceof MilvusVectorStore);
    }

    @Test
    void vendorIsUsedWhenProtocolIsNotProvided() {
        assertTrue(VectorStoreResolver.create(configWithoutProtocol(VectorStoreVendor.QDRANT))
                instanceof QdrantVectorStore);
        assertTrue(VectorStoreResolver.create(configWithoutProtocol(VectorStoreVendor.MILVUS))
                instanceof MilvusVectorStore);
    }

    @Test
    void acceptsTimeoutAndProperties() {
        assertTrue(VectorStoreResolver.create(VectorStoreConfig.builder()
                .protocol(VectorStoreProtocol.QDRANT)
                .baseUrl("http://localhost:6333")
                .defaultCollection("docs")
                .connectTimeoutMillis(1234)
                .readTimeoutMillis(5678)
                .property("extra", "x")
                .build()) instanceof QdrantVectorStore);
    }

    @Test
    void rejectsWhenNeitherProtocolNorVendorProvided() {
        assertThrows(IllegalArgumentException.class,
                () -> VectorStoreResolver.create(configWithoutProtocol(null)));
    }

    @Test
    void rejectsWhenBaseUrlMissing() {
        assertThrows(IllegalArgumentException.class, () -> VectorStoreConfig.builder()
                .protocol(VectorStoreProtocol.QDRANT)
                .build());
    }

    private VectorStoreConfig config(VectorStoreProtocol protocol) {
        return config(protocol, null);
    }

    private VectorStoreConfig config(VectorStoreProtocol protocol, VectorStoreVendor vendor) {
        return VectorStoreConfig.builder()
                .protocol(protocol)
                .vendor(vendor)
                .baseUrl("http://localhost:6333")
                .defaultCollection("docs")
                .build();
    }

    private VectorStoreConfig configWithoutProtocol() {
        return VectorStoreConfig.builder()
                .baseUrl("http://localhost:6333")
                .defaultCollection("docs")
                .build();
    }

    private VectorStoreConfig configWithoutProtocol(VectorStoreVendor vendor) {
        return VectorStoreConfig.builder()
                .vendor(vendor)
                .baseUrl("http://localhost:6333")
                .defaultCollection("docs")
                .build();
    }
}
