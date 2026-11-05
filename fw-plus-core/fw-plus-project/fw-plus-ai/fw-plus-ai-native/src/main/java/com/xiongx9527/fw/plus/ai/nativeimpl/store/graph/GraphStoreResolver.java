package com.xiongx9527.fw.plus.ai.nativeimpl.store.graph;

import com.xiongx9527.fw.plus.ai.base.store.graph.GraphStorePort;

/** 创建图存储端口；同时配置厂商和协议时，始终以协议为准。 */
public final class GraphStoreResolver {
    private GraphStoreResolver() { }

    /**
     * 创建端口。厂商与协议至少配置其一；二者皆有时以协议为准，协议缺失时回退到厂商路由。
     */
    public static GraphStorePort create(GraphStoreConfig config) {
        requireConfig(config);
        if (config.getProtocol() != null) {
            return createByProtocol(config);
        }
        if (config.getVendor() == null) {
            throw new IllegalArgumentException("graph store protocol or vendor is required");
        }
        return createByVendor(config.getVendor(), config);
    }

    private static GraphStorePort createByProtocol(GraphStoreConfig config) {
        switch (config.getProtocol()) {
            case NEO4J:
                return createNeo4j(config);
            case NEBULA:
                return createNebula(config);
            default:
                throw new IllegalArgumentException("unsupported graph store protocol: " + config.getProtocol());
        }
    }

    private static GraphStorePort createByVendor(GraphStoreVendor vendor, GraphStoreConfig config) {
        switch (vendor) {
            case NEO4J:
                return newNeo4j(config);
            case NEBULA:
                return newNebula(config);
            default:
                throw new IllegalArgumentException("unsupported graph store vendor: " + vendor);
        }
    }

    public static GraphStorePort createNeo4j(GraphStoreConfig config) {
        requireProtocol(config, GraphStoreProtocol.NEO4J);
        return newNeo4j(config);
    }

    public static GraphStorePort createNebula(GraphStoreConfig config) {
        requireProtocol(config, GraphStoreProtocol.NEBULA);
        return newNebula(config);
    }

    private static GraphStorePort newNeo4j(GraphStoreConfig config) {
        int connect = config.getConnectTimeoutMillis() != null ? config.getConnectTimeoutMillis() : 5000;
        int read = config.getReadTimeoutMillis() != null ? config.getReadTimeoutMillis() : 10000;
        return new Neo4jGraphStore(config.getBaseUrl(), config.getUsername(), config.getPassword(),
                config.getDatabase(), connect, read);
    }

    private static GraphStorePort newNebula(GraphStoreConfig config) {
        int connect = config.getConnectTimeoutMillis() != null ? config.getConnectTimeoutMillis() : 5000;
        int read = config.getReadTimeoutMillis() != null ? config.getReadTimeoutMillis() : 10000;
        return new NebulaGraphStore(config.getBaseUrl(), config.getUsername(), config.getPassword(),
                config.getDatabase(), connect, read);
    }

    private static void requireProtocol(GraphStoreConfig config, GraphStoreProtocol... supported) {
        requireConfig(config);
        for (GraphStoreProtocol p : supported) {
            if (config.getProtocol() == p) {
                return;
            }
        }
        throw new IllegalArgumentException("protocol " + config.getProtocol()
                + " is not supported by this vendor factory");
    }

    private static void requireConfig(GraphStoreConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("graph store config is required");
        }
    }

}
