package com.xiongx9527.fw.plus.ai.langchain4j.adapter.store;

import dev.langchain4j.store.embedding.filter.Filter;

/**
 * 删除钩子：为不支持 {@code remove*} 的 {@code EmbeddingStore} 补齐删除能力。
 *
 * <p>LangChain4j 0.35 的 {@code EmbeddingStore.remove(String)} / {@code removeAll(Filter)}
 * 默认实现直接抛 {@link UnsupportedOperationException}，像 {@code Neo4jEmbeddingStore}
 * 就没有覆写它们。业务侧可注入本接口的实现（例如用 Neo4j Driver 执行
 * {@code MATCH (n) WHERE n.id = $id DETACH DELETE n}），由适配器在删除时调用。</p>
 */
public interface EmbeddingRemover {

    /** 按上游返回的 embeddingId 删除单条。 */
    void remove(String embeddingId);

    /** 按上游 filter 条件批量删除，用于集合清理与按元数据删除。 */
    void removeAll(Filter filter);
}
