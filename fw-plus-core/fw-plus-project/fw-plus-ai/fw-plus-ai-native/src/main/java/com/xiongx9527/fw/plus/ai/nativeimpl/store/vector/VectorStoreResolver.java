package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;

/** 创建向量存储端口；同时配置厂商和协议时，始终以协议为准。 */
public final class VectorStoreResolver {
    private VectorStoreResolver() { }

    /**
     * 创建端口。厂商与协议至少配置其一；二者皆有时以协议为准，协议缺失时回退到厂商路由。
     */
    public static VectorStorePort create(VectorStoreConfig config) {
        requireConfig(config);
        if (config.getProtocol() != null) {
            return createByProtocol(config);
        }
        if (config.getVendor() == null) {
            throw new IllegalArgumentException("vector store protocol or vendor is required");
        }
        return createByVendor(config.getVendor(), config);
    }

    private static VectorStorePort createByProtocol(VectorStoreConfig config) {
        switch (config.getProtocol()) {
            case QDRANT:
                return createQdrant(config);
            case MILVUS:
                return createMilvus(config);
            default:
                throw new IllegalArgumentException("unsupported vector store protocol: " + config.getProtocol());
        }
    }

    private static VectorStorePort createByVendor(VectorStoreVendor vendor, VectorStoreConfig config) {
        switch (vendor) {
            case QDRANT:
                return newQdrant(config);
            case MILVUS:
                return newMilvus(config);
            default:
                throw new IllegalArgumentException("unsupported vector store vendor: " + vendor);
        }
    }

    public static VectorStorePort createQdrant(VectorStoreConfig config) {
        requireProtocol(config, VectorStoreProtocol.QDRANT);
        return newQdrant(config);
    }

    public static VectorStorePort createMilvus(VectorStoreConfig config) {
        requireProtocol(config, VectorStoreProtocol.MILVUS);
        return newMilvus(config);
    }

    private static VectorStorePort newQdrant(VectorStoreConfig config) {
        int connect = config.getConnectTimeoutMillis() != null ? config.getConnectTimeoutMillis() : 5000;
        int read = config.getReadTimeoutMillis() != null ? config.getReadTimeoutMillis() : 10000;
        String collection = config.getDefaultCollection() != null ? config.getDefaultCollection() : "default";
        return new QdrantVectorStore(config.getBaseUrl(), config.getApiKey(), collection, connect, read);
    }

    private static VectorStorePort newMilvus(VectorStoreConfig config) {
        int connect = config.getConnectTimeoutMillis() != null ? config.getConnectTimeoutMillis() : 5000;
        int read = config.getReadTimeoutMillis() != null ? config.getReadTimeoutMillis() : 10000;
        String collection = config.getDefaultCollection() != null ? config.getDefaultCollection() : "default";
        // Milvus 用 Bearer token 鉴权，配置里的 apiKey 直接作为 token 传入
        return new MilvusVectorStore(config.getBaseUrl(), config.getApiKey(), collection, connect, read);
    }

    private static void requireProtocol(VectorStoreConfig config, VectorStoreProtocol... supported) {
        requireConfig(config);
        for (VectorStoreProtocol p : supported) {
            if (config.getProtocol() == p) {
                return;
            }
        }
        throw new IllegalArgumentException("protocol " + config.getProtocol()
                + " is not supported by this vendor factory");
    }

    private static void requireConfig(VectorStoreConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("vector store config is required");
        }
    }

}
