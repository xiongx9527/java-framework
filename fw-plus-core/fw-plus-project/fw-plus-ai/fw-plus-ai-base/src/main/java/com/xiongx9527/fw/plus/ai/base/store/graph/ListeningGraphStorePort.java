package com.xiongx9527.fw.plus.ai.base.store.graph;

import com.xiongx9527.fw.plus.ai.base.store.StoreQuery;
import com.xiongx9527.fw.plus.ai.base.store.StoreRecord;
import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 给任意 {@link GraphStorePort} 挂监听器（装饰器），与 {@link ListeningVectorStorePort} 同构。
 *
 * <p>监听器异常不得影响主链路：每个通知都吞掉 {@link RuntimeException}。</p>
 */
public class ListeningGraphStorePort implements GraphStorePort {

    private final GraphStorePort delegate;

    private final List<GraphStoreListener> listeners;

    public ListeningGraphStorePort(GraphStorePort delegate, List<GraphStoreListener> listeners) {
        if (delegate == null) {
            throw new IllegalArgumentException("graph store port is required");
        }
        this.delegate = delegate;
        this.listeners = listeners == null ? new ArrayList<GraphStoreListener>() : listeners;
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
    public String name() {
        return delegate.name();
    }

    @Override
    public StoreType type() {
        return delegate.type();
    }

    @Override
    public void save(StoreRecord record) {
        guard(() -> delegate.save(record));
    }

    @Override
    public void saveAll(Collection<StoreRecord> records) {
        if (records != null) {
            for (StoreRecord r : records) {
                save(r);
            }
        }
    }

    @Override
    public void update(StoreRecord record) {
        delegate.update(record);
    }

    @Override
    public StoreRecord get(String id) {
        return delegate.get(id);
    }

    @Override
    public List<StoreRecord> getAll(Collection<String> ids) {
        return delegate.getAll(ids);
    }

    @Override
    public boolean delete(String id) {
        return delegate.delete(id);
    }

    @Override
    public long deleteAll(Collection<String> ids) {
        long removed = 0L;
        if (ids != null) {
            for (String id : ids) {
                if (delegate.delete(id)) {
                    removed++;
                }
            }
        }
        return removed;
    }

    @Override
    public List<StoreRecord> search(StoreQuery query) {
        return delegate.search(query);
    }

    @Override
    public void clear() {
        guard(delegate::clear);
    }

    @Override
    public long count() {
        return delegate.count();
    }

    @Override
    public List<GraphEdge> neighbors(String nodeId, Set<String> edgeTypes) {
        try {
            List<GraphEdge> result = delegate.neighbors(nodeId, edgeTypes);
            int count = result == null ? 0 : result.size();
            for (GraphStoreListener listener : listeners) {
                try {
                    listener.onNeighbors(nodeId, count);
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

    @Override
    public List<GraphNode> nodes(Collection<String> ids) {
        try {
            List<GraphNode> result = delegate.nodes(ids);
            int requested = ids == null ? 0 : ids.size();
            int hit = result == null ? 0 : result.size();
            for (GraphStoreListener listener : listeners) {
                try {
                    listener.onNodes(requested, hit);
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

    @Override
    public List<GraphNode> searchNodes(String text, int topK) {
        try {
            List<GraphNode> result = delegate.searchNodes(text, topK);
            int count = result == null ? 0 : result.size();
            for (GraphStoreListener listener : listeners) {
                try {
                    listener.onSearch(topK, count);
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

    private void notifyError(RuntimeException e) {
        for (GraphStoreListener listener : listeners) {
            try {
                listener.onError(e.getMessage());
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }
}
