package com.xiongx9527.fw.plus.ai.store.embedding.listener;

import com.xiongx9527.fw.plus.ai.store.embedding.EmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.EmbeddingSearchResult;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量库响应上下文（抽象基类）。
 *
 * <p><b>作用</b>：在请求上下文之外补充响应结果（写入 ID、召回匹配项等），
 * 由 7 个嵌套子类按操作补充。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。逐成员镜像
 * {@code dev.langchain4j.store.embedding.listener.EmbeddingStoreResponseContext}
 * 及其嵌套子类 {@code Add} / {@code AddAll} / {@code Remove} / {@code RemoveAll} /
 * {@code RemoveAllFilter} / {@code RemoveAllIds} / {@code Search}。</p>
 *
 * @param <Embedded> 被嵌入对象类型
 * @author xiongxiao
 */
public abstract class EmbeddingStoreResponseContext<Embedded> {

    private final EmbeddingStore<Embedded> embeddingStore;
    private final EmbeddingStoreRequestContext<Embedded> requestContext;
    private final Map<Object, Object> attributes;

    /**
     * 构造响应上下文。
     *
     * @param requestContext 对应请求上下文
     * @param attributes     附加属性，可为 {@code null}
     */
    protected EmbeddingStoreResponseContext(EmbeddingStoreRequestContext<Embedded> requestContext,
                                            Map<Object, Object> attributes) {
        this.requestContext = requestContext;
        this.embeddingStore = requestContext == null ? null : requestContext.embeddingStore();
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
     * 对应请求上下文。
     *
     * @return 请求上下文
     */
    public EmbeddingStoreRequestContext<Embedded> requestContext() {
        return requestContext;
    }

    /**
     * {@code add} 系列操作的响应上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class Add<Embedded> extends EmbeddingStoreResponseContext<Embedded> {

        private final String returnedId;

        /**
         * 构造响应上下文。
         *
         * @param requestContext 请求上下文
         * @param attributes     附加属性
         * @param returnedId     实现返回的记录 ID，可为 {@code null}
         */
        public Add(EmbeddingStoreRequestContext<Embedded> requestContext, Map<Object, Object> attributes,
                   String returnedId) {
            super(requestContext, attributes);
            this.returnedId = returnedId;
        }

        /**
         * 实现返回的记录 ID。
         *
         * @return 记录 ID，可为 {@code null}
         */
        public String returnedId() {
            return returnedId;
        }
    }

    /**
     * {@code addAll} 批量写入的响应上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class AddAll<Embedded> extends EmbeddingStoreResponseContext<Embedded> {

        private final List<String> returnedIds;

        /**
         * 构造响应上下文。
         *
         * @param requestContext 请求上下文
         * @param attributes     附加属性
         * @param returnedIds    实现返回的记录 ID 列表
         */
        public AddAll(EmbeddingStoreRequestContext<Embedded> requestContext, Map<Object, Object> attributes,
                      List<String> returnedIds) {
            super(requestContext, attributes);
            this.returnedIds = returnedIds;
        }

        /**
         * 实现返回的记录 ID 列表。
         *
         * @return 记录 ID 列表
         */
        public List<String> returnedIds() {
            return returnedIds;
        }
    }

    /**
     * 按单 ID 删除的响应上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class Remove<Embedded> extends EmbeddingStoreResponseContext<Embedded> {

        /**
         * 构造响应上下文。
         *
         * @param requestContext 请求上下文
         * @param attributes     附加属性
         */
        public Remove(EmbeddingStoreRequestContext.Remove<Embedded> requestContext, Map<Object, Object> attributes) {
            super(requestContext, attributes);
        }
    }

    /**
     * 清空全部记录的响应上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class RemoveAll<Embedded> extends EmbeddingStoreResponseContext<Embedded> {

        /**
         * 构造响应上下文。
         *
         * @param requestContext 请求上下文
         * @param attributes     附加属性
         */
        public RemoveAll(EmbeddingStoreRequestContext.RemoveAll<Embedded> requestContext, Map<Object, Object> attributes) {
            super(requestContext, attributes);
        }
    }

    /**
     * 按过滤条件删除的响应上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class RemoveAllFilter<Embedded> extends EmbeddingStoreResponseContext<Embedded> {

        /**
         * 构造响应上下文。
         *
         * @param requestContext 请求上下文
         * @param attributes     附加属性
         */
        public RemoveAllFilter(EmbeddingStoreRequestContext.RemoveAllFilter<Embedded> requestContext,
                               Map<Object, Object> attributes) {
            super(requestContext, attributes);
        }
    }

    /**
     * 按 ID 批量删除的响应上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class RemoveAllIds<Embedded> extends EmbeddingStoreResponseContext<Embedded> {

        /**
         * 构造响应上下文。
         *
         * @param requestContext 请求上下文
         * @param attributes     附加属性
         */
        public RemoveAllIds(EmbeddingStoreRequestContext.RemoveAllIds<Embedded> requestContext,
                            Map<Object, Object> attributes) {
            super(requestContext, attributes);
        }
    }

    /**
     * 检索的响应上下文。
     *
     * @param <Embedded> 被嵌入对象类型
     */
    public static final class Search<Embedded> extends EmbeddingStoreResponseContext<Embedded> {

        private final EmbeddingSearchResult<Embedded> searchResult;

        /**
         * 构造响应上下文。
         *
         * @param requestContext 请求上下文
         * @param attributes     附加属性
         * @param searchResult   检索结果
         */
        public Search(EmbeddingStoreRequestContext<Embedded> requestContext, Map<Object, Object> attributes,
                      EmbeddingSearchResult<Embedded> searchResult) {
            super(requestContext, attributes);
            this.searchResult = searchResult;
        }

        /**
         * 检索结果。
         *
         * @return 检索结果
         */
        public EmbeddingSearchResult<Embedded> searchResult() {
            return searchResult;
        }
    }
}
