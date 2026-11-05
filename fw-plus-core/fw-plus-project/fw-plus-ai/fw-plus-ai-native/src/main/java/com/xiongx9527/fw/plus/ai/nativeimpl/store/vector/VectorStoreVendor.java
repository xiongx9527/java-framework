package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

/**
 * 向量存储厂商。协议显式配置时由协议决定具体端口；无协议的厂商只按厂商名路由。
 *
 * <p>REDIS / PGVECTOR / ELASTICSEARCH / WEAVIATE 只有一种连接方式，不单独声明协议枚举，
 * 避免对“没有协议的 store”强行套用协议抽象。</p>
 */
public enum VectorStoreVendor {
    QDRANT,
    MILVUS,
    REDIS,
    PGVECTOR,
    ELASTICSEARCH,
    WEAVIATE
}
