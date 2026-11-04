package com.xiongx9527.fw.plus.ai.base.store;

import java.util.Collection;
import java.util.List;

/**
 * 存储后端的公共契约：向量库 / 图库 / 任何检索后端共享的身份、生命周期与基础 CRUD。
 *
 * <p>具体的读写 / 检索语义由各子包特化（{@code vector} 的
 * {@link com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort}、
 * {@code graph} 的 {@link com.xiongx9527.fw.plus.ai.base.store.graph.GraphStorePort}）；本接口只声明两者都该有的
 * 通用能力，从而能统一登记到 {@link StoreRegistry} 按名 / 按类型取用。</p>
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

    /** 新增 / 更新：按 id 幂等写入（向量库写向量 + 元数据；图库写节点）。 */
    void save(StoreRecord record);

    /** 按 id 取单条；不存在返回 {@code null}。 */
    StoreRecord get(String id);

    /** 批量按 id 取多条；缺失的 id 不出现在结果中。 */
    List<StoreRecord> getAll(Collection<String> ids);

    /** 删除单条；不存在视为成功，返回是否实际删除。 */
    boolean delete(String id);

    /** 召回：图库按文本召回；向量库按向量召回，最多 topK 条。 */
    List<StoreRecord> search(StoreQuery query);

    /** 批量新增 / 更新（按 id 幂等写入）。 */
    void saveAll(Collection<StoreRecord> records);

    /** 更新单条：要求该 id 已存在；不存在抛 {@link IllegalStateException}。 */
    void update(StoreRecord record);

    /** 批量删除：不存在视为成功，返回实际删除条数。 */
    long deleteAll(Collection<String> ids);

    /** 清空全部数据（仅显式重建链路调用，绝不静默触发）。 */
    void clear();

    /** 条目规模估计（向量条数 / 图节点数）；未知返回负数。 */
    long count();
}
