package com.xiongx9527.fw.plus.ai.base.store.vector;

import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.store.Store;
import com.xiongx9527.fw.plus.ai.base.store.StoreQuery;
import com.xiongx9527.fw.plus.ai.base.store.StoreRecord;
import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import com.xiongx9527.fw.plus.common.exception.BizException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 向量存储端口：持久化 + 最近邻检索。
 *
 * <p>只负责「向量怎么存、怎么取」，不关心向量怎么生成（那是
 * {@link com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort} 的职责）。</p>
 *
 * <p><b>集合维度</b>：{@link #ensureCollection(String, int)} 只在集合缺失时创建，
 * 已存在但维度不一致时抛 {@link DimensionMismatchException}，绝不隐式删除集合——
 * 一次维度误配不应该变成无人值守的数据清空，重建必须走显式的
 * {@link #dropCollection(String)} + 回填。</p>
 *
 * <p>只实现 {@link Store} 的最小身份 / 生命周期契约。向量库不具备按 id 随机读与
 * 「是否存在」语义，因此不实现 {@link com.xiongx9527.fw.plus.ai.base.store.RecordStore}；
 * 下面的 {@code save / delete / search / get} 等是 {@code VectorStorePort} 自己提供的
 * 便利方法，语义由本接口声明，不存在对父接口契约的降级覆写。</p>
 */
public interface VectorStorePort extends Store {

    /** 向量库通用 CRUD 所依托的默认集合名；由具体 adapter 提供。 */
    String defaultCollection();

    /** 新增 / 更新（按默认集合幂等写入）。 */
    default void save(StoreRecord record) {
        if (record == null || record.getId() == null) {
            return;
        }
        upsert(defaultCollection(), record.getId(), record.getVector(), record.getMetadata());
    }

    /** 删除单条（按默认集合）；向量库无「是否实际存在」语义，恒返回 true。 */
    default boolean delete(String id) {
        if (id == null) {
            return false;
        }
        delete(defaultCollection(), id);
        return true;
    }

    /** 批量新增 / 更新（逐条 save）。 */
    default void saveAll(Collection<StoreRecord> records) {
        if (records != null) {
            for (StoreRecord r : records) {
                save(r);
            }
        }
    }

    /** 更新单条：向量库无「是否存在」语义，等同 save（按 id 幂等覆盖）。 */
    default void update(StoreRecord record) {
        save(record);
    }

    /** 批量删除：逐条 delete，返回实际删除条数。 */
    default long deleteAll(Collection<String> ids) {
        long removed = 0L;
        if (ids != null) {
            for (String id : ids) {
                if (delete(id)) {
                    removed++;
                }
            }
        }
        return removed;
    }

    /** 清空默认集合。 */
    @Override
    default void clear() {
        dropCollection(defaultCollection());
    }

    /** 向量库无随机计数能力，返回 -1 表示未知。 */
    @Override
    default long count() {
        return -1L;
    }

    /** 按向量召回（默认集合）。文本查询不被向量库支持，抛 {@link UnsupportedOperationException}。 */
    default List<StoreRecord> search(StoreQuery query) {
        if (query == null || query.getVector() == null) {
            throw new UnsupportedOperationException(
                    "vector store search requires a query vector; use search(VectorSearchRequest) for typed access");
        }
        VectorSearchResult result = search(VectorSearchRequest.builder()
                .collection(defaultCollection())
                .vector(query.getVector())
                .topK(query.getTopK())
                .filter(query.getFilter())
                .build());
        if (result == null) {
            return Collections.emptyList();
        }
        List<StoreRecord> out = new ArrayList<StoreRecord>();
        for (ScoredDocument hit : result.getHits()) {
            Document doc = hit.getDocument();
            out.add(StoreRecord.builder().id(doc.getId()).content(doc.getContent())
                    .metadata(doc.getMetadata()).score(hit.getScore()).build());
        }
        return out;
    }

    /** 向量库默认不支持按 id 随机取单条（需先召回再取），抛 {@link UnsupportedOperationException}。 */
    default StoreRecord get(String id) {
        throw new UnsupportedOperationException(
                "random get-by-id is not supported by vector stores; use search(VectorSearchRequest)");
    }

    default List<StoreRecord> getAll(Collection<String> ids) {
        List<StoreRecord> out = new ArrayList<StoreRecord>();
        if (ids != null) {
            for (String id : ids) {
                StoreRecord r = get(id);
                if (r != null) {
                    out.add(r);
                }
            }
        }
        return out;
    }

    /** 是否已完成配置（未配置后端地址时为 {@code false}，区分「未配置」与「不可达」）。 */
    default boolean configured() {
        return true;
    }

    /** 连通性探测；不可用时返回 {@code false}。 */
    boolean health();

    /** 确保集合存在且维度一致（不存在则创建，余弦距离）。 */
    void ensureCollection(String collection, int dimension);

    /** 集合当前维度；集合不存在或后端不可用时返回 {@code null}（表示尚未建立索引）。 */
    default Integer collectionDimension(String collection) {
        return null;
    }

    /** 删除集合（不存在视为成功）。会丢弃集合内全部向量，只允许显式重建链路调用。 */
    void dropCollection(String collection);

    /** 写入 / 更新单条向量并附带元数据（业务维度属性，如数据源 id、对象 id）。 */
    void upsert(String collection, String id, double[] vector, Map<String, Object> metadata);

    /** 不带元数据的写入。 */
    default void upsert(String collection, String id, double[] vector) {
        upsert(collection, id, vector, null);
    }

    /** 删除单条向量（不存在视为成功）。 */
    void delete(String collection, String id);

    /** 批量删除（同一集合）。默认逐条删除，后端应覆写为单次批量请求。 */
    default void deleteBatch(String collection, Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (String id : ids) {
            if (id != null) {
                delete(collection, id);
            }
        }
    }

    /** 按元数据等值删除。 */
    default void deleteByMetadata(String collection, String key, Object value) {
        throw new UnsupportedOperationException("deleteByMetadata is not supported by this vector store");
    }

    /** 最近邻检索，结果按分数降序。 */
    VectorSearchResult search(VectorSearchRequest request);

    /** 异步最近邻检索；默认在公共线程池执行 {@link #search(VectorSearchRequest)}，后端可覆写为原生异步。 */
    default CompletableFuture<VectorSearchResult> searchAsync(VectorSearchRequest request) {
        return CompletableFuture.supplyAsync(() -> search(request));
    }

    /** 不限候选、不带元数据过滤的检索。 */
    default VectorSearchResult search(String collection, double[] vector, int topK) {
        return search(VectorSearchRequest.builder()
                .collection(collection)
                .vector(vector)
                .topK(topK)
                .build());
    }

    /** 集合维度与待写入向量不一致：写入被拒绝，集合与既有向量保持不变。 */
    class DimensionMismatchException extends BizException {

        private static final long serialVersionUID = 1L;

        public static final int CODE = 50_010_002;

        public DimensionMismatchException(String message) {
            super(CODE, message);
        }
    }
}
