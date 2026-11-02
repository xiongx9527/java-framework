package com.xiongx9527.fw.plus.ai.store.embedding;

import com.xiongx9527.fw.plus.ai.data.embedding.Embedding;
import com.xiongx9527.fw.plus.ai.store.embedding.EmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.filter.Filter;
import com.xiongx9527.fw.plus.ai.store.embedding.listener.EmbeddingStoreErrorContext;
import com.xiongx9527.fw.plus.ai.store.embedding.listener.EmbeddingStoreListener;
import com.xiongx9527.fw.plus.ai.store.embedding.listener.EmbeddingStoreRequestContext;
import com.xiongx9527.fw.plus.ai.store.embedding.listener.EmbeddingStoreResponseContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 带监听器的向量库包装器（包内可见）。
 *
 * <p><b>作用</b>：用装饰器把监听回调织入 {@link EmbeddingStore} 的每个写 / 查动作，
 * 让所有向量库实现共享同一套埋点，而不必各自复制。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.store.embedding.ListeningEmbeddingStore}。</p>
 *
 * <p><b>实现约定</b>：监听器抛出的运行时异常由 {@link EmbeddingStoreListenerUtils} 吞掉，
 * 绝不影响主链路；{@code withAdditionalListeners} 返回新实例，不修改当前实例。</p>
 *
 * @param <Embedded> 被嵌入对象类型
 * @author xiongxiao
 */
final class ListeningEmbeddingStore<Embedded> implements EmbeddingStore<Embedded> {

    private final EmbeddingStore<Embedded> delegate;
    private final List<EmbeddingStoreListener> listeners;

    /**
     * 构造包装器。
     *
     * @param delegate  被包装的向量库
     * @param listeners 监听器列表
     */
    ListeningEmbeddingStore(EmbeddingStore<Embedded> delegate, List<EmbeddingStoreListener> listeners) {
        this.delegate = delegate;
        this.listeners = new ArrayList<EmbeddingStoreListener>(listeners);
    }

    /**
     * 在当前监听器基础上追加监听器。
     *
     * @param additional 追加的监听器
     * @return 新的包装实例
     */
    EmbeddingStore<Embedded> withAdditionalListeners(List<EmbeddingStoreListener> additional) {
        List<EmbeddingStoreListener> merged = new ArrayList<EmbeddingStoreListener>(listeners);
        merged.addAll(additional);
        return new ListeningEmbeddingStore<Embedded>(delegate, merged);
    }

    @Override
    public String add(Embedding embedding) {
        Map<Object, Object> attributes = Collections.emptyMap();
        EmbeddingStoreRequestContext.Add<Embedded> request =
                new EmbeddingStoreRequestContext.Add<Embedded>(delegate, attributes, embedding);
        EmbeddingStoreListenerUtils.onRequest(request, listeners);
        try {
            String id = delegate.add(embedding);
            EmbeddingStoreListenerUtils.onResponse(
                    new EmbeddingStoreResponseContext.Add<Embedded>(request, attributes, id), listeners);
            return id;
        } catch (RuntimeException e) {
            EmbeddingStoreListenerUtils.onError(
                    new EmbeddingStoreErrorContext<Embedded>(e, request, attributes), listeners);
            throw e;
        }
    }

    @Override
    public void add(String id, Embedding embedding) {
        Map<Object, Object> attributes = Collections.emptyMap();
        EmbeddingStoreRequestContext.Add<Embedded> request =
                new EmbeddingStoreRequestContext.Add<Embedded>(delegate, attributes, id, embedding);
        EmbeddingStoreListenerUtils.onRequest(request, listeners);
        try {
            delegate.add(id, embedding);
            EmbeddingStoreListenerUtils.onResponse(
                    new EmbeddingStoreResponseContext.Add<Embedded>(request, attributes, id), listeners);
        } catch (RuntimeException e) {
            EmbeddingStoreListenerUtils.onError(
                    new EmbeddingStoreErrorContext<Embedded>(e, request, attributes), listeners);
            throw e;
        }
    }

    @Override
    public String add(Embedding embedding, Embedded embedded) {
        Map<Object, Object> attributes = Collections.emptyMap();
        EmbeddingStoreRequestContext.Add<Embedded> request =
                new EmbeddingStoreRequestContext.Add<Embedded>(delegate, attributes, null, embedding, embedded);
        EmbeddingStoreListenerUtils.onRequest(request, listeners);
        try {
            String id = delegate.add(embedding, embedded);
            EmbeddingStoreListenerUtils.onResponse(
                    new EmbeddingStoreResponseContext.Add<Embedded>(request, attributes, id), listeners);
            return id;
        } catch (RuntimeException e) {
            EmbeddingStoreListenerUtils.onError(
                    new EmbeddingStoreErrorContext<Embedded>(e, request, attributes), listeners);
            throw e;
        }
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        Map<Object, Object> attributes = Collections.emptyMap();
        EmbeddingStoreRequestContext.AddAll<Embedded> request =
                new EmbeddingStoreRequestContext.AddAll<Embedded>(delegate, attributes, null, embeddings, null);
        EmbeddingStoreListenerUtils.onRequest(request, listeners);
        try {
            List<String> ids = delegate.addAll(embeddings);
            EmbeddingStoreListenerUtils.onResponse(
                    new EmbeddingStoreResponseContext.AddAll<Embedded>(request, attributes, ids), listeners);
            return ids;
        } catch (RuntimeException e) {
            EmbeddingStoreListenerUtils.onError(
                    new EmbeddingStoreErrorContext<Embedded>(e, request, attributes), listeners);
            throw e;
        }
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<Embedded> embedded) {
        Map<Object, Object> attributes = Collections.emptyMap();
        EmbeddingStoreRequestContext.AddAll<Embedded> request =
                new EmbeddingStoreRequestContext.AddAll<Embedded>(delegate, attributes, null, embeddings, embedded);
        EmbeddingStoreListenerUtils.onRequest(request, listeners);
        try {
            List<String> ids = delegate.addAll(embeddings, embedded);
            EmbeddingStoreListenerUtils.onResponse(
                    new EmbeddingStoreResponseContext.AddAll<Embedded>(request, attributes, ids), listeners);
            return ids;
        } catch (RuntimeException e) {
            EmbeddingStoreListenerUtils.onError(
                    new EmbeddingStoreErrorContext<Embedded>(e, request, attributes), listeners);
            throw e;
        }
    }

    @Override
    public void addAll(List<String> ids, List<Embedding> embeddings, List<Embedded> embedded) {
        Map<Object, Object> attributes = Collections.emptyMap();
        EmbeddingStoreRequestContext.AddAll<Embedded> request =
                new EmbeddingStoreRequestContext.AddAll<Embedded>(delegate, attributes, ids, embeddings, embedded);
        EmbeddingStoreListenerUtils.onRequest(request, listeners);
        try {
            delegate.addAll(ids, embeddings, embedded);
            EmbeddingStoreListenerUtils.onResponse(
                    new EmbeddingStoreResponseContext.AddAll<Embedded>(request, attributes, ids), listeners);
        } catch (RuntimeException e) {
            EmbeddingStoreListenerUtils.onError(
                    new EmbeddingStoreErrorContext<Embedded>(e, request, attributes), listeners);
            throw e;
        }
    }

    @Override
    public void remove(String id) {
        Map<Object, Object> attributes = Collections.emptyMap();
        EmbeddingStoreRequestContext.Remove<Embedded> request =
                new EmbeddingStoreRequestContext.Remove<Embedded>(delegate, attributes, id);
        EmbeddingStoreListenerUtils.onRequest(request, listeners);
        try {
            delegate.remove(id);
            EmbeddingStoreListenerUtils.onResponse(
                    new EmbeddingStoreResponseContext.Remove<Embedded>(request, attributes), listeners);
        } catch (RuntimeException e) {
            EmbeddingStoreListenerUtils.onError(
                    new EmbeddingStoreErrorContext<Embedded>(e, request, attributes), listeners);
            throw e;
        }
    }

    @Override
    public void removeAll(Collection<String> ids) {
        Map<Object, Object> attributes = Collections.emptyMap();
        EmbeddingStoreRequestContext.RemoveAllIds<Embedded> request =
                new EmbeddingStoreRequestContext.RemoveAllIds<Embedded>(delegate, attributes,
                        ids == null ? null : new ArrayList<String>(ids));
        EmbeddingStoreListenerUtils.onRequest(request, listeners);
        try {
            delegate.removeAll(ids);
            EmbeddingStoreListenerUtils.onResponse(
                    new EmbeddingStoreResponseContext.RemoveAllIds<Embedded>(request, attributes), listeners);
        } catch (RuntimeException e) {
            EmbeddingStoreListenerUtils.onError(
                    new EmbeddingStoreErrorContext<Embedded>(e, request, attributes), listeners);
            throw e;
        }
    }

    @Override
    public void removeAll(Filter filter) {
        Map<Object, Object> attributes = Collections.emptyMap();
        EmbeddingStoreRequestContext.RemoveAllFilter<Embedded> request =
                new EmbeddingStoreRequestContext.RemoveAllFilter<Embedded>(delegate, attributes, filter);
        EmbeddingStoreListenerUtils.onRequest(request, listeners);
        try {
            delegate.removeAll(filter);
            EmbeddingStoreListenerUtils.onResponse(
                    new EmbeddingStoreResponseContext.RemoveAllFilter<Embedded>(request, attributes), listeners);
        } catch (RuntimeException e) {
            EmbeddingStoreListenerUtils.onError(
                    new EmbeddingStoreErrorContext<Embedded>(e, request, attributes), listeners);
            throw e;
        }
    }

    @Override
    public void removeAll() {
        Map<Object, Object> attributes = Collections.emptyMap();
        EmbeddingStoreRequestContext.RemoveAll<Embedded> request =
                new EmbeddingStoreRequestContext.RemoveAll<Embedded>(delegate, attributes);
        EmbeddingStoreListenerUtils.onRequest(request, listeners);
        try {
            delegate.removeAll();
            EmbeddingStoreListenerUtils.onResponse(
                    new EmbeddingStoreResponseContext.RemoveAll<Embedded>(request, attributes), listeners);
        } catch (RuntimeException e) {
            EmbeddingStoreListenerUtils.onError(
                    new EmbeddingStoreErrorContext<Embedded>(e, request, attributes), listeners);
            throw e;
        }
    }

    @Override
    public EmbeddingSearchResult<Embedded> search(EmbeddingSearchRequest searchRequest) {
        Map<Object, Object> attributes = Collections.emptyMap();
        EmbeddingStoreRequestContext.Search<Embedded> request =
                new EmbeddingStoreRequestContext.Search<Embedded>(delegate, attributes, searchRequest);
        EmbeddingStoreListenerUtils.onRequest(request, listeners);
        try {
            EmbeddingSearchResult<Embedded> result = delegate.search(searchRequest);
            EmbeddingStoreListenerUtils.onResponse(
                    new EmbeddingStoreResponseContext.Search<Embedded>(request, attributes, result), listeners);
            return result;
        } catch (RuntimeException e) {
            EmbeddingStoreListenerUtils.onError(
                    new EmbeddingStoreErrorContext<Embedded>(e, request, attributes), listeners);
            throw e;
        }
    }
}
