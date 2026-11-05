package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

/** 显式区分不同向量存储后端协议；厂商名称不能代替协议名称。 */
public enum VectorStoreProtocol {
    QDRANT,
    MILVUS
}
