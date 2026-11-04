package com.xiongx9527.fw.plus.ai.base.store.graph;

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
