package com.xiongx9527.fw.plus.ai.nativeimpl.store.graph;

/** 显式区分不同图存储后端协议；厂商名称不能代替协议名称。 */
public enum GraphStoreProtocol {
    NEO4J,
    NEBULA
}
