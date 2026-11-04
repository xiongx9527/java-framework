package com.xiongx9527.fw.plus.ai.support.resilience;

import com.xiongx9527.fw.plus.ai.base.model.LlmException;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.base.resilience.ModelFailoverPort;
import com.xiongx9527.fw.plus.ai.base.resilience.ModelHealthPort;
import java.util.List;

/**
 * 故障转移：按优先级尝试候选链，前一个抛错或不可用则换下一个。
 *
 * <p>非流式通道整体重试；流式通道不做逐段重试（重放已发增量会重复内容）。</p>
 */
public final class FailoverLlmPort implements LlmPort {

    private final LlmPort primary;

    private final ModelFailoverPort failover;

    private final ModelHealthPort health;

    public FailoverLlmPort(LlmPort primary, ModelFailoverPort failover, ModelHealthPort health) {
        this.primary = primary;
        this.failover = failover;
        this.health = health;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        String modelId = request.getModel();
        List<String> candidates = failover == null ? null : failover.candidates(modelId);
        if (candidates == null || candidates.isEmpty()) {
            return primary.complete(request);
        }
        RuntimeException last = null;
        for (String candidate : candidates) {
            if (health != null && !health.available(candidate)) {
                continue;
            }
            try {
                LlmResponse response = primary.complete(withModel(request, candidate));
                if (health != null) {
                    health.record(candidate, ModelHealthPort.Outcome.SUCCESS);
                }
                return response;
            } catch (RuntimeException e) {
                last = e;
                if (health != null) {
                    health.record(candidate, ModelHealthPort.Outcome.FAILURE);
                }
            }
        }
        throw last == null ? new LlmException("no available model for: " + modelId) : last;
    }

    @Override
    public void stream(LlmRequest request, LlmStreamHandler handler) {
        // 流式不跨候选重试，直接走主模型
        primary.stream(request, handler);
    }

    @Override
    public List<ModelInfo> listModels() {
        return primary.listModels();
    }

    @Override
    public ModelValidation validateModel(String model) {
        return primary.validateModel(model);
    }

    private static LlmRequest withModel(LlmRequest request, String modelId) {
        return com.xiongx9527.fw.plus.ai.base.model.LlmRequest.builder()
                .messages(request.getMessages())
                .model(modelId)
                .temperature(request.getTemperature())
                .topP(request.getTopP())
                .maxTokens(request.getMaxTokens())
                .extraParams(request.getExtraParams())
                .listeners(request.getListeners())
                .tools(request.getTools())
                .toolChoice(request.getToolChoice())
                .build();
    }
}
