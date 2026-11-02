package com.xiongx9527.fw.plus.ai.store.embedding.listener;

import com.xiongx9527.fw.plus.ai.data.embedding.Embedding;
import com.xiongx9527.fw.plus.ai.store.embedding.EmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.EmbeddingSearchRequest;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.Filter;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量库请求上下文（抽象基类）。
 *
 * <p><b>作用</b>：不同操作（add / addAll / remove / removeAll / search）的参数差异很大，
 * 因此基类只保留「哪个向量库 + 附加属性」，具体参数由 7 个嵌套子类按操作补充。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。逐成员镜像
 * {@code dev.langchain4j.store.embedding.listener.EmbeddingStoreRequestContext}
 * 及其 7 个嵌套子类 {@code Add} / {@code AddAll} / {@code Remove} / {@code RemoveAll} /
 * {@code RemoveAllFilter} / {@code RemoveAllIds} / {@code Search}。</p>
 *
 * <p><b>实现约定</b>：属性 Map 在构造时复制为只读快照，监听器无法反向修改请求。</p>
 *
 * @param <Embedded> 被嵌入对象类型
 * @author xiongxiao
 */
public abstract class EmbeddingStoreRequestContext<Embedded> {

    private final EmbeddingStore<Embedded> embeddingStore;
    private final Map<Object, Object> attributes;

    /**
     * 构造请求上下文。
     *
     * @param embeddingStore 向量库
     * @param attributes     附加属性，可为 {@code null}
     */
    protected EmbeddingStoreRequestContext(EmbeddingStore<Embedded> embeddingStore, Map<Object, Object> attributes) {
        this.embeddingStore = embeddingStore;
        this.attributes = attributes == null
                ? Collections.emptyMap()
                : Collections.unmodifiableMap(new LinkedHashMap<Object, Object>(attributes));
    }

    /**
     * 向量库。
     *
     * @return 向量库
     */
    public EmbeddingStore<Embedded> embeddingStore() {
        return embeddingStore;
    }

    /**
     * 附加属性。
     *
     * @return 只读属性 Map，非 {@code null}
     */
    public Map<Object, Object> attributes() {
        return attributes;
    }

    /**
     * {@code add} 系列操作的请求上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class Add<Embedded> extends EmbeddingStoreRequestContext<Embedded> {

        private final String id;
        private final Embedding embedding;
        private final Embedded embedded;

        /**
         * 只写向量（由实现生成 ID）。
         *
         * @param embeddingStore 向量库
         * @param attributes     附加属性
         * @param embedding      向量
         */
        public Add(EmbeddingStore<Embedded> embeddingStore, Map<Object, Object> attributes, Embedding embedding) {
            this(embeddingStore, attributes, null, embedding, null);
        }

        /**
         * 指定 ID 写向量。
         *
         * @param embeddingStore 向量库
         * @param attributes     附加属性
         * @param id             记录 ID
         * @param embedding      向量
         */
        public Add(EmbeddingStore<Embedded> embeddingStore, Map<Object, Object> attributes, String id,
                   Embedding embedding) {
            this(embeddingStore, attributes, id, embedding, null);
        }

        /**
         * 写向量及其原始对象。
         *
         * @param embeddingStore 向量库
         * @param attributes     附加属性
         * @param id             记录 ID，可为 {@code null}
         * @param embedding      向量
         * @param embedded       原始对象，可为 {@code null}
         */
        public Add(EmbeddingStore<Embedded> embeddingStore, Map<Object, Object> attributes, String id,
                   Embedding embedding, Embedded embedded) {
            super(embeddingStore, attributes);
            this.id = id;
            this.embedding = embedding;
            this.embedded = embedded;
        }

        /**
         * 记录 ID。
         *
         * @return ID；由实现生成时为 {@code null}
         */
        public String id() {
            return id;
        }

        /**
         * 向量。
         *
         * @return 向量
         */
        public Embedding embedding() {
            return embedding;
        }

        /**
         * 原始对象。
         *
         * @return 原始对象，可为 {@code null}
         */
        public Embedded embedded() {
            return embedded;
        }
    }

    /**
     * {@code addAll} 批量写入的请求上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class AddAll<Embedded> extends EmbeddingStoreRequestContext<Embedded> {

        private final List<String> ids;
        private final List<Embedding> embeddings;
        private final List<Embedded> embeddedList;

        /**
         * 构造批量写入上下文。
         *
         * @param embeddingStore 向量库
         * @param attributes     附加属性
         * @param ids            记录 ID 列表
         * @param embeddings     向量列表
         * @param embeddedList   原始对象列表，可为 {@code null}
         */
        public AddAll(EmbeddingStore<Embedded> embeddingStore, Map<Object, Object> attributes, List<String> ids,
                      List<Embedding> embeddings, List<Embedded> embeddedList) {
            super(embeddingStore, attributes);
            this.ids = ids;
            this.embeddings = embeddings;
            this.embeddedList = embeddedList;
        }

        /**
         * 记录 ID 列表。
         *
         * @return ID 列表
         */
        public List<String> ids() {
            return ids;
        }

        /**
         * 向量列表。
         *
         * @return 向量列表
         */
        public List<Embedding> embeddings() {
            return embeddings;
        }

        /**
         * 原始对象列表。
         *
         * @return 原始对象列表，可为 {@code null}
         */
        public List<Embedded> embeddedList() {
            return embeddedList;
        }
    }

    /**
     * 按单 ID 删除的请求上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class Remove<Embedded> extends EmbeddingStoreRequestContext<Embedded> {

        private final String id;

        /**
         * 构造删除上下文。
         *
         * @param embeddingStore 向量库
         * @param attributes     附加属性
         * @param id             记录 ID
         */
        public Remove(EmbeddingStore<Embedded> embeddingStore, Map<Object, Object> attributes, String id) {
            super(embeddingStore, attributes);
            this.id = id;
        }

        /**
         * 记录 ID。
         *
         * @return 记录 ID
         */
        public String id() {
            return id;
        }
    }

    /**
     * 清空全部记录的请求上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class RemoveAll<Embedded> extends EmbeddingStoreRequestContext<Embedded> {

        /**
         * 构造清空上下文。
         *
         * @param embeddingStore 向量库
         * @param attributes     附加属性
         */
        public RemoveAll(EmbeddingStore<Embedded> embeddingStore, Map<Object, Object> attributes) {
            super(embeddingStore, attributes);
        }
    }

    /**
     * 按过滤条件删除的请求上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class RemoveAllFilter<Embedded> extends EmbeddingStoreRequestContext<Embedded> {

        private final Filter filter;

        /**
         * 构造按条件删除上下文。
         *
         * @param embeddingStore 向量库
         * @param attributes     附加属性
         * @param filter         过滤条件
         */
        public RemoveAllFilter(EmbeddingStore<Embedded> embeddingStore, Map<Object, Object> attributes, Filter filter) {
            super(embeddingStore, attributes);
            this.filter = filter;
        }

        /**
         * 过滤条件。
         *
         * @return 过滤条件
         */
        public Filter filter() {
            return filter;
        }
    }

    /**
     * 按 ID 批量删除的请求上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class RemoveAllIds<Embedded> extends EmbeddingStoreRequestContext<Embedded> {

        private final List<String> ids;

        /**
         * 构造按 ID 批量删除上下文。
         *
         * @param embeddingStore 向量库
         * @param attributes     附加属性
         * @param ids            记录 ID 列表
         */
        public RemoveAllIds(EmbeddingStore<Embedded> embeddingStore, Map<Object, Object> attributes, List<String> ids) {
            super(embeddingStore, attributes);
            this.ids = ids;
        }

        /**
         * 记录 ID 列表。
         *
         * @return ID 列表
         */
        public List<String> ids() {
            return ids;
        }
    }

    /**
     * 检索的请求上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class Search<Embedded> extends EmbeddingStoreRequestContext<Embedded> {

        private final EmbeddingSearchRequest searchRequest;

        /**
         * 构造检索上下文。
         *
         * @param embeddingStore 向量库
         * @param attributes     附加属性
         * @param searchRequest  检索请求
         */
        public Search(EmbeddingStore<Embedded> embeddingStore, Map<Object, Object> attributes,
                      EmbeddingSearchRequest searchRequest) {
            super(embeddingStore, attributes);
            this.searchRequest = searchRequest;
        }

        /**
         * 检索请求。
         *
         * @return 检索请求
         */
        public EmbeddingSearchRequest searchRequest() {
            return searchRequest;
        }
    }
}
