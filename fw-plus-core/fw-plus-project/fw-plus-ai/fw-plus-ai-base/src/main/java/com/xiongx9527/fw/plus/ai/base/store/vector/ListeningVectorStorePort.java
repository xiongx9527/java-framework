package com.xiongx9527.fw.plus.ai.base.store.vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 给任意 {@link VectorStorePort} 挂监听器（装饰器）。
 */
public class ListeningVectorStorePort implements VectorStorePort {

    private final VectorStorePort delegate;

    private final List<VectorStoreListener> listeners;

    public ListeningVectorStorePort(VectorStorePort delegate, List<VectorStoreListener> listeners) {
        if (delegate == null) {
            throw new IllegalArgumentException("vector store port is required");
        }
        this.delegate = delegate;
        this.listeners = listeners == null ? new ArrayList<VectorStoreListener>() : listeners;
    }

    @Override
    public boolean configured() {
        return delegate.configured();
    }

    @Override
    public boolean health() {
        return delegate.health();
    }

    @Override
    public void ensureCollection(String collection, int dimension) {
        guard(() -> delegate.ensureCollection(collection, dimension));
    }

    @Override
    public Integer collectionDimension(String collection) {
        return delegate.collectionDimension(collection);
    }

    @Override
    public void dropCollection(String collection) {
        guard(() -> delegate.dropCollection(collection));
    }

    @Override
    public void upsert(String collection, String id, double[] vector, Map<String, Object> metadata) {
        guard(() -> delegate.upsert(collection, id, vector, metadata));
        notifyUpsert(collection, id);
    }

    @Override
    public void delete(String collection, String id) {
        guard(() -> delegate.delete(collection, id));
        notifyDelete(collection, id);
    }

    @Override
    public void deleteBatch(String collection, Collection<String> ids) {
        guard(() -> delegate.deleteBatch(collection, ids));
        if (ids != null) {
            for (String id : ids) {
                notifyDelete(collection, id);
            }
        }
    }

    @Override
    public void deleteByMetadata(String collection, String key, Object value) {
        guard(() -> delegate.deleteByMetadata(collection, key, value));
    }

    @Override
    public VectorSearchResult search(VectorSearchRequest request) {
        try {
            VectorSearchResult result = delegate.search(request);
            int count = result == null ? 0 : result.size();
            for (VectorStoreListener listener : listeners) {
                try {
                    listener.onSearch(request == null ? null : request.getCollection(),
                            request == null ? 0 : request.getTopK(), count);
                } catch (RuntimeException ignored) {
                    // 监听器异常不得影响主链路
                }
            }
            return result;
        } catch (RuntimeException e) {
            notifyError(e);
            throw e;
        }
    }

    private void guard(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            notifyError(e);
            throw e;
        }
    }

    private void notifyUpsert(String collection, String id) {
        for (VectorStoreListener listener : listeners) {
            try {
                listener.onUpsert(collection, id);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void notifyDelete(String collection, String id) {
        for (VectorStoreListener listener : listeners) {
            try {
                listener.onDelete(collection, id);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void notifyError(RuntimeException e) {
        for (VectorStoreListener listener : listeners) {
            try {
                listener.onError(e.getMessage());
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }
}
