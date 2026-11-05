package com.xiongx9527.fw.plus.ai.base.store;

/**
 * 存储后端的公共身份与生命周期契约。
 *
 * <p>所有检索后端（向量库 / 图库 / 自定义）都能诚实实现这里的全部方法：身份
 * （{@link #name()}、{@link #type()}）、可用性（{@link #configured()}、{@link #health()}）
 * 与生命周期（{@link #clear()}、{@link #count()}）。由此可统一登记到
 * {@link StoreRegistry} 按名 / 按类型取用。</p>
 *
 * <p><b>读写语义不在这里声明</b>：按 id 的 CRUD 与文本召回由 {@link RecordStore} 承接
 * （图库等支持随机读的后端实现），向量库的最近邻检索由
 * {@link com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort} 自行定义。
 * 这样按 {@code Store} 多态取用时不会遇到「不支持随机读」「恒返回删除成功」这类
 * 与声明不符的行为；需要读写能力时按 {@link RecordStore} 或 {@code VectorStorePort}
 * 的具体类型取用。</p>
 */
public interface Store {

    /** 逻辑名（与 {@link StoreRegistry} 的登记 key 对应，便于排查与路由）。 */
    String name();

    /** 存储类型：向量 / 图 / 自定义。 */
    StoreType type();

    /** 是否已完成配置（未配置后端地址时为 {@code false}，区分「未配置」与「不可达」）。 */
    boolean configured();

    /** 连通性探测；不可用时返回 {@code false}。 */
    boolean health();

    /** 清空全部数据（仅显式重建链路调用，绝不静默触发）。 */
    void clear();

    /** 条目规模估计（向量条数 / 图节点数）；未知返回负数。 */
    long count();
}
