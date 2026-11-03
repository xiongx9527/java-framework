package com.xiongx9527.fw.plus.ai.base.store;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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
 */
public interface VectorStorePort {

    /** 是否已完成配置（未配置后端地址时为 {@code false}，用于区分「未配置」与「不可达」）。 */
    default boolean configured() {
        return true;
    }

    /** 连通性探测；不可用时返回 {@code false}。 */
    boolean health();

    /**
     * 确保集合存在且维度一致（不存在则创建，余弦距离）。
     *
     * @throws DimensionMismatchException 集合已存在但维度与 {@code dimension} 不一致
     */
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

    /**
     * 批量删除（同一集合）。默认逐条删除，后端应覆写为单次批量请求：
     * 删一个数据源可能涉及上千条向量，逐条删会把请求数和失败窗口放大到同数量级。
     */
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

    /**
     * 按元数据等值删除（{@code key == value}）。
     *
     * <p>与 {@link #deleteBatch} 互补：批量删需要先知道 id，按元数据删只依赖写入时带的属性，
     * 因此「删数据源」即使进程在删库和删向量之间崩溃也不会留下孤儿向量。</p>
     *
     * @throws UnsupportedOperationException 后端不支持按元数据删除
     */
    default void deleteByMetadata(String collection, String key, Object value) {
        throw new UnsupportedOperationException("deleteByMetadata is not supported by this vector store");
    }

    /** 最近邻检索，结果按分数降序。 */
    List<ScoredDocument> search(VectorSearchRequest request);

    /** 不限候选、不带元数据过滤的检索。 */
    default List<ScoredDocument> search(String collection, double[] vector, int topK) {
        return search(VectorSearchRequest.builder()
                .collection(collection)
                .vector(vector)
                .topK(topK)
                .build());
    }

    /**
     * 集合维度与待写入向量不一致：写入被拒绝，集合与既有向量保持不变。
     *
     * <p>单独成类型而不是靠消息文本判断——调用方要能区分「维度不对，需重建」与
     * 「后端不可达，稍后重试」，后者不能被当成永久损坏而短路后续写入。</p>
     */
    class DimensionMismatchException extends IllegalStateException {

        private static final long serialVersionUID = 1L;

        public DimensionMismatchException(String message) {
            super(message);
        }
    }
}
