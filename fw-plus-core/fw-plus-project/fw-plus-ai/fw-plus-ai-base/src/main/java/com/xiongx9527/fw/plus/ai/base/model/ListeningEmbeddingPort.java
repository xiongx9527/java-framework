package com.xiongx9527.fw.plus.ai.base.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 给任意 {@link EmbeddingPort} 挂监听器（装饰器，不改端口签名）。
 */
public class ListeningEmbeddingPort implements EmbeddingPort {

    private final EmbeddingPort delegate;

    private final List<EmbeddingListener> listeners;

    public ListeningEmbeddingPort(EmbeddingPort delegate, List<EmbeddingListener> listeners) {
        if (delegate == null) {
            throw new IllegalArgumentException("embedding port is required");
        }
        this.delegate = delegate;
        this.listeners = listeners == null ? new ArrayList<EmbeddingListener>() : listeners;
    }

    @Override
    public double[] embed(String text) {
        notifyRequest(Arrays.asList(text));
        try {
            double[] vector = delegate.embed(text);
            notifyResponse(vector == null ? 0 : 1, vector == null ? 0 : vector.length);
            return vector;
        } catch (RuntimeException e) {
            notifyError(e);
            throw e;
        }
    }

    @Override
    public List<double[]> embedAll(List<String> texts) {
        notifyRequest(texts);
        try {
            List<double[]> vectors = delegate.embedAll(texts);
            int dimension = 0;
            int count = 0;
            if (vectors != null) {
                for (double[] vector : vectors) {
                    if (vector != null) {
                        count++;
                        dimension = vector.length;
                    }
                }
            }
            notifyResponse(count, dimension);
            return vectors;
        } catch (RuntimeException e) {
            notifyError(e);
            throw e;
        }
    }

    @Override
    public int dimension() {
        return delegate.dimension();
    }

    @Override
    public List<ModelInfo> listModels() {
        return delegate.listModels();
    }

    @Override
    public ModelValidation validateModel(String model) {
        return delegate.validateModel(model);
    }

    private void notifyRequest(List<String> texts) {
        for (EmbeddingListener listener : listeners) {
            try {
                listener.onRequest(texts);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void notifyResponse(int count, int dimension) {
        for (EmbeddingListener listener : listeners) {
            try {
                listener.onResponse(count, dimension);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void notifyError(RuntimeException e) {
        for (EmbeddingListener listener : listeners) {
            try {
                listener.onError(e.getMessage());
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }
}
