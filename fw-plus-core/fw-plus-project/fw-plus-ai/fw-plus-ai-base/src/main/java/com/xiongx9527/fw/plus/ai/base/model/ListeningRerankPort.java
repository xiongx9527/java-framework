package com.xiongx9527.fw.plus.ai.base.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 给任意 {@link RerankPort} 挂监听器（装饰器）。
 */
public class ListeningRerankPort implements RerankPort {

    private final RerankPort delegate;

    private final List<RerankListener> listeners;

    public ListeningRerankPort(RerankPort delegate, List<RerankListener> listeners) {
        if (delegate == null) {
            throw new IllegalArgumentException("rerank port is required");
        }
        this.delegate = delegate;
        this.listeners = listeners == null ? new ArrayList<RerankListener>() : listeners;
    }

    @Override
    public List<Double> score(String query, List<String> documents) {
        for (RerankListener listener : listeners) {
            try {
                listener.onRequest(query, documents == null ? 0 : documents.size());
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
        try {
            List<Double> scores = delegate.score(query, documents);
            for (RerankListener listener : listeners) {
                try {
                    listener.onResponse(scores);
                } catch (RuntimeException ignored) {
                    // 监听器异常不得影响主链路
                }
            }
            return scores;
        } catch (RuntimeException e) {
            for (RerankListener listener : listeners) {
                try {
                    listener.onError(e.getMessage());
                } catch (RuntimeException ignored) {
                    // 监听器异常不得影响主链路
                }
            }
            throw e;
        }
    }
}
