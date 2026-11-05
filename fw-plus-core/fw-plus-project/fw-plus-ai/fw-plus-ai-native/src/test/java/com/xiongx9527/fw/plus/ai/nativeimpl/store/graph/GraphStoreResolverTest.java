package com.xiongx9527.fw.plus.ai.nativeimpl.store.graph;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GraphStoreResolverTest {

    @Test
    void resolvesProtocolToNeo4jStore() {
        assertTrue(GraphStoreResolver.create(GraphStoreConfig.builder()
                .protocol(GraphStoreProtocol.NEO4J)
                .baseUrl("http://localhost:7474")
                .build()) instanceof Neo4jGraphStore);
    }

    @Test
    void createsStoreThroughUnifiedFactory() {
        assertTrue(GraphStoreResolver.create(
                config(GraphStoreProtocol.NEO4J, GraphStoreVendor.NEO4J))
                instanceof Neo4jGraphStore);
    }

    @Test
    void protocolWinsWhenVendorAndProtocolAreBothProvided() {
        assertTrue(GraphStoreResolver.create(
                config(GraphStoreProtocol.NEO4J, GraphStoreVendor.NEO4J))
                instanceof Neo4jGraphStore);
    }

    @Test
    void vendorIsUsedWhenProtocolIsNotProvided() {
        assertTrue(GraphStoreResolver.create(configWithoutProtocol(GraphStoreVendor.NEO4J))
                instanceof Neo4jGraphStore);
    }

    @Test
    void acceptsTimeoutAndProperties() {
        assertTrue(GraphStoreResolver.create(GraphStoreConfig.builder()
                .protocol(GraphStoreProtocol.NEO4J)
                .baseUrl("http://localhost:7474")
                .username("neo4j")
                .password("secret")
                .database("neo4j")
                .connectTimeoutMillis(1234)
                .readTimeoutMillis(5678)
                .property("extra", "x")
                .build()) instanceof Neo4jGraphStore);
    }

    @Test
    void rejectsWhenNeitherProtocolNorVendorProvided() {
        assertThrows(IllegalArgumentException.class,
                () -> GraphStoreResolver.create(configWithoutProtocol(null)));
    }

    @Test
    void rejectsWhenBaseUrlMissing() {
        assertThrows(IllegalArgumentException.class, () -> GraphStoreConfig.builder()
                .protocol(GraphStoreProtocol.NEO4J)
                .build());
    }

    private GraphStoreConfig config(GraphStoreProtocol protocol) {
        return config(protocol, null);
    }

    private GraphStoreConfig config(GraphStoreProtocol protocol, GraphStoreVendor vendor) {
        return GraphStoreConfig.builder()
                .protocol(protocol)
                .vendor(vendor)
                .baseUrl("http://localhost:7474")
                .build();
    }

    private GraphStoreConfig configWithoutProtocol() {
        return GraphStoreConfig.builder()
                .baseUrl("http://localhost:7474")
                .build();
    }

    private GraphStoreConfig configWithoutProtocol(GraphStoreVendor vendor) {
        return GraphStoreConfig.builder()
                .vendor(vendor)
                .baseUrl("http://localhost:7474")
                .build();
    }

    @Test
    void resolvesNebulaViaProtocolAndVendor() {
        assertTrue(GraphStoreResolver.create(GraphStoreConfig.builder()
                .protocol(GraphStoreProtocol.NEBULA)
                .baseUrl("http://localhost:8080")
                .build()) instanceof NebulaGraphStore);
        assertTrue(GraphStoreResolver.create(GraphStoreConfig.builder()
                .vendor(GraphStoreVendor.NEBULA)
                .baseUrl("http://localhost:8080")
                .build()) instanceof NebulaGraphStore);
    }

}
