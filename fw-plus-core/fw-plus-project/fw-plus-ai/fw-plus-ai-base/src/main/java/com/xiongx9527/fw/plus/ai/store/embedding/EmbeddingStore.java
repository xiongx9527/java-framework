package com.xiongx9527.fw.plus.ai.store.embedding;

import com.xiongx9527.fw.plus.ai.data.embedding.Embedding;
import com.xiongx9527.fw.plus.ai.exception.UnsupportedFeatureException;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.Filter;
import com.xiongx9527.fw.plus.ai.store.embedding.listener.EmbeddingStoreListener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * 向量存储端口。
 *
 * <p><b>作用</b>：统一「写入向量 + 检索相似向量 + 按条件删除」三类能力，
 * 让 RAG 链路只依赖本接口，换向量库（Qdrant / Milvus / pgvector / Elasticsearch…）不改上层代码。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。逐方法镜像
 * {@code dev.langchain4j.store.embedding.EmbeddingStore}（包路径与官方一致）。
 * Spring AI 等价 {@code VectorStore}。</p>
 *
 * <p><b>实现约定</b>：
 * <ul>
 *   <li>只有 {@link #add(Embedding, Object)}、{@link #add(Embedding)}、{@link #add(String, Embedding)}、
 *       {@link #addAll(List)}、{@link #search(EmbeddingSearchRequest)} 是必须实现的核心能力；
 *       其余默认方法在不支持时抛 {@link UnsupportedFeatureException}，不要静默空实现。</li>
 *   <li>{@link #add(Embedding, Object)} 中 {@code embedded} 为被嵌入的原始对象（通常是
 *       {@code TextSegment}），实现需把它随向量一起持久化，检索时原样返回。</li>
 *   <li>{@link #addListeners(List)} 返回包装后的新实例，不修改 {@code this}。</li>
 * </ul>
 *
 * @param <Embedded> 被嵌入对象类型，通常为 {@code TextSegment}
 * @author xiongxiao
 */
public interface EmbeddingStore<Embedded> {

    /**
     * 写入一个向量，由实现生成并返回记录 ID。
     *
     * @param embedding 向量，不能为 {@code null}
     * @return 记录 ID
     */
    String add(Embedding embedding);

    /**
     * 以指定 ID 写入一个向量（覆盖同 ID 记录）。
     *
     * @param id        记录 ID，不能为空白
     * @param embedding 向量，不能为 {@code null}
     */
    void add(String id, Embedding embedding);

    /**
     * 写入一个向量及其原始对象。
     *
     * @param embedding 向量，不能为 {@code null}
     * @param embedded  被嵌入的原始对象，可为 {@code null}
     * @return 记录 ID
     */
    String add(Embedding embedding, Embedded embedded);

    /**
     * 批量写入向量（不带原始对象），由实现生成 ID。
     *
     * @param embeddings 向量列表，不能为 {@code null}
     * @return 与入参顺序一致的记录 ID 列表
     */
    List<String> addAll(List<Embedding> embeddings);

    /**
     * 批量写入向量及其原始对象，由实现生成 ID。
     *
     * @param embeddings 向量列表，不能为 {@code null}
     * @param embedded   原始对象列表，顺序需与 {@code embeddings} 对应，可为 {@code null}
     * @return 与入参顺序一致的记录 ID 列表
     */
    default List<String> addAll(List<Embedding> embeddings, List<Embedded> embedded) {
        List<String> ids = generateIds(embeddings.size());
        addAll(ids, embeddings, embedded);
        return ids;
    }

    /**
     * 生成指定数量的随机 ID。
     *
     * @param n 数量
     * @return 随机 UUID 字符串列表
     */
    default List<String> generateIds(int n) {
        List<String> ids = new ArrayList<String>(n);
        for (int i = 0; i < n; i++) {
            ids.add(UUID.randomUUID().toString());
        }
        return ids;
    }

    /**
     * 批量写入向量，ID 与原始对象均由调用方给定。
     *
     * @param ids        记录 ID 列表，顺序需与 {@code embeddings} 对应
     * @param embeddings 向量列表
     * @param embedded   原始对象列表，可为 {@code null}
     */
    default void addAll(List<String> ids, List<Embedding> embeddings, List<Embedded> embedded) {
        throw new UnsupportedFeatureException("Not supported yet.");
    }

    /**
     * 按 ID 删除一条记录。
     *
     * @param id 记录 ID，不能为空白
     */
    default void remove(String id) {
        removeAll(Collections.singletonList(id));
    }

    /**
     * 按 ID 批量删除。
     *
     * @param ids 记录 ID 集合
     */
    default void removeAll(Collection<String> ids) {
        throw new UnsupportedFeatureException("Not supported yet.");
    }

    /**
     * 按元数据条件批量删除。
     *
     * @param filter 过滤条件
     */
    default void removeAll(Filter filter) {
        throw new UnsupportedFeatureException("Not supported yet.");
    }

    /**
     * 清空全部记录。
     */
    default void removeAll() {
        throw new UnsupportedFeatureException("Not supported yet.");
    }

    /**
     * 相似向量检索。
     *
     * @param request 检索请求（查询向量、条数、最低分、过滤条件）
     * @return 检索结果，按相似度降序
     */
    EmbeddingSearchResult<Embedded> search(EmbeddingSearchRequest request);

    /**
     * 异步相似向量检索。
     *
     * @param request 检索请求
     * @return 异步结果；实现不支持时返回失败的 Future
     */
    default CompletableFuture<EmbeddingSearchResult<Embedded>> searchAsync(EmbeddingSearchRequest request) {
        CompletableFuture<EmbeddingSearchResult<Embedded>> future = new CompletableFuture<EmbeddingSearchResult<Embedded>>();
        future.completeExceptionally(new UnsupportedFeatureException(
                getClass().getName() + " does not support searchAsync() yet."));
        return future;
    }

    /**
     * 追加一个监听器。
     *
     * @param listener 监听器，可为 {@code null}
     * @return 带该监听器的新向量库实例
     */
    default EmbeddingStore<Embedded> addListener(EmbeddingStoreListener listener) {
        return addListeners(listener == null
                ? null
                : Collections.singletonList(listener));
    }

    /**
     * 追加一组监听器。
     *
     * @param listeners 监听器列表，为空时返回 {@code this}
     * @return 带这些监听器的新向量库实例
     */
    default EmbeddingStore<Embedded> addListeners(List<EmbeddingStoreListener> listeners) {
        if (listeners == null || listeners.isEmpty()) {
            return this;
        }
        return new ListeningEmbeddingStore<Embedded>(this, listeners);
    }
}
