package com.xiongx9527.fw.plus.ai.support.resilience;

import com.xiongx9527.fw.plus.ai.base.model.LlmListener;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.base.resilience.ModelHealthPort;
import com.xiongx9527.fw.plus.ai.base.resilience.ModelListener;
import java.util.List;

/**
 * 给 LLM 调用挂「健康度记录 + 模型级监听」的装饰器：成功 / 失败都回写健康端口，
 * 并按模型 id 派发 {@link ModelListener}。
 */
public final class MonitoredLlmPort implements LlmPort {

    private final LlmPort delegate;

    private final ModelHealthPort health;

    private final ModelListener listener;

    public MonitoredLlmPort(LlmPort delegate, ModelHealthPort health, ModelListener listener) {
        this.delegate = delegate;
        this.health = health;
        this.listener = listener;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String modelId = request.getModel();
        long start = System.currentTimeMillis();
        if (listener != null) {
            listener.onRequest(modelId);
        }
        try {
            LlmResponse response = delegate.complete(request);
            if (health != null) {
                health.record(modelId, ModelHealthPort.Outcome.SUCCESS);
            }
            if (listener != null) {
                listener.onResponse(modelId, System.currentTimeMillis() - start);
            }
            return response;
        } catch (RuntimeException e) {
            if (health != null) {
                health.record(modelId, ModelHealthPort.Outcome.FAILURE);
            }
            if (listener != null) {
                listener.onFailure(modelId, e.getMessage());
            }
            throw e;
        }
    }

    @Override
    public List<ModelInfo> listModels() {
        return delegate.listModels();
    }

    @Override
    public ModelValidation validateModel(String model) {
        return delegate.validateModel(model);
    }

    @Override
    public LlmTokenStream stream(LlmRequest request) {
        String modelId = request.getModel();
        long start = System.currentTimeMillis();
        if (listener != null) {
            listener.onRequest(modelId);
        }
        return delegate.stream(request)
                .onComplete(() -> {
                    if (health != null) {
                        health.record(modelId, ModelHealthPort.Outcome.SUCCESS);
                    }
                    if (listener != null) {
                        listener.onResponse(modelId, System.currentTimeMillis() - start);
                    }
                })
                .onError(t -> {
                    if (health != null) {
                        health.record(modelId, ModelHealthPort.Outcome.FAILURE);
                    }
                    if (listener != null) {
                        listener.onFailure(modelId, t == null ? null : t.getMessage());
                    }
                });
    }
}
