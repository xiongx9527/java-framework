package com.xiongx9527.fw.plus.ai.base.store;

/**
 * 存储后端的公共生命周期门面：向量库 / 图库 / 任何检索后端共享的「是否配置 / 是否可用」观测。
 *
 * <p>具体的读写与检索语义由各子包自定（{@code vector} 的 {@link VectorStorePort}、
 * {@code graph} 的 {@link GraphStorePort}），但它们都先实现本接口，从而能统一登记到
 * {@link StoreRegistry} 并按名 / 按类型取用，互不影响。</p>
 */
public interface Store {

    /** 是否已完成配置（未配置后端地址时为 {@code false}，用于区分「未配置」与「不可达」）。 */
    default boolean configured() {
        return true;
    }

    /** 连通性探测；不可用时返回 {@code false}。 */
    boolean health();
}
