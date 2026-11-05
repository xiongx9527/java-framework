package com.xiongx9527.fw.plus.ai.base.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 给任意 {@link LlmPort} 挂「模型级监听」的装饰器：调用前 / 后 / 失败统一派发
 * {@link ChatModelListener}，无需业务每次请求手动挂监听。
 *
 * <p>与 {@link AbstractLlmPort} 的「按请求 {@link LlmListener}」正交：一个挂在模型实例上，
 * 一个挂在单次请求上。</p>
 */
public final class ListeningLlmPort implements LlmPort {

    private final LlmPort delegate;

    private final List<ChatModelListener> listeners;

    public ListeningLlmPort(LlmPort delegate, List<ChatModelListener> listeners) {
        this.delegate = delegate;
        this.listeners = Collections.unmodifiableList(listeners == null
                ? new ArrayList<ChatModelListener>() : new ArrayList<ChatModelListener>(listeners));
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String modelId = request.getModel();
        long start = System.currentTimeMillis();
        notifyRequest(modelId);
        try {
            LlmResponse response = delegate.complete(request);
            notifyResponse(modelId, System.currentTimeMillis() - start);
            return response;
        } catch (RuntimeException e) {
            notifyError(modelId, e.getMessage());
            throw e;
        }
    }

    @Override
    public LlmTokenStream stream(LlmRequest request) {
        String modelId = request.getModel();
        long start = System.currentTimeMillis();
        notifyRequest(modelId);
        return delegate.stream(request)
                .onComplete(() -> notifyResponse(modelId, System.currentTimeMillis() - start))
                .onError(t -> notifyError(modelId, t == null ? null : t.getMessage()));
    }

    @Override
    public List<ModelInfo> listModels() {
        return delegate.listModels();
    }

    @Override
    public ModelValidation validateModel(String model) {
        return delegate.validateModel(model);
    }

    private void notifyRequest(String modelId) {
        for (ChatModelListener listener : listeners) {
            try {
                listener.onModelRequest(modelId);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void notifyResponse(String modelId, long latency) {
        for (ChatModelListener listener : listeners) {
            try {
                listener.onModelResponse(modelId, latency);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }

    private void notifyError(String modelId, String message) {
        for (ChatModelListener listener : listeners) {
            try {
                listener.onModelError(modelId, message);
            } catch (RuntimeException ignored) {
                // 监听器异常不得影响主链路
            }
        }
    }
}
