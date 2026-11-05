package com.xiongx9527.fw.plus.ai.base.store;

import java.util.Collection;
import java.util.List;

/**
 * 支持按 id 随机读写与文本召回的存储契约。
 *
 * <p>从 {@link Store} 分离出来：向量库通常不具备随机读与「是否存在」语义，
 * 不应被迫实现这些方法的降级版本（返回 {@code null} 冒充不存在、恒返回删除成功等）。
 * 图库、内存表等能完整支持 CRUD 的后端实现本接口；向量库只实现 {@link Store}
 * 与 {@link com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort}。</p>
 */
public interface RecordStore extends Store {

    /** 新增 / 更新：按 id 幂等写入（图库写节点）。 */
    void save(StoreRecord record);

    /** 按 id 取单条；不存在返回 {@code null}。 */
    StoreRecord get(String id);

    /** 批量按 id 取多条；缺失的 id 不出现在结果中。 */
    List<StoreRecord> getAll(Collection<String> ids);

    /** 删除单条；不存在视为成功，返回是否实际删除。 */
    boolean delete(String id);

    /** 召回：按文本 + 元数据过滤，最多 topK 条。 */
    List<StoreRecord> search(StoreQuery query);

    /** 批量新增 / 更新（按 id 幂等写入）。 */
    void saveAll(Collection<StoreRecord> records);

    /** 更新单条：要求该 id 已存在；不存在抛 {@link IllegalStateException}。 */
    void update(StoreRecord record);

    /** 批量删除：不存在视为成功，返回实际删除条数。 */
    long deleteAll(Collection<String> ids);
}
