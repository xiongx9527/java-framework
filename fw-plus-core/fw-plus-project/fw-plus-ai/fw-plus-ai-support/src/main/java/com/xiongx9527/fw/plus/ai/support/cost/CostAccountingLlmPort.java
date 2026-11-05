package com.xiongx9527.fw.plus.ai.support.cost;

import com.xiongx9527.fw.plus.ai.base.cost.CostPort;
import com.xiongx9527.fw.plus.ai.base.cost.CostUsage;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import java.util.List;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;

/**
 * 成本核算装饰器：调用结束后把用量折算成 {@link CostUsage} 落库 / 上报。
 *
 * <p>金额换算由 {@link CostPort} 的实现负责（单价表），这里只把 token 用量传过去。</p>
 */
public final class CostAccountingLlmPort implements LlmPort {

    private final LlmPort delegate;

    private final CostPort cost;

    public CostAccountingLlmPort(LlmPort delegate, CostPort cost) {
        this.delegate = delegate;
        this.cost = cost;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        LlmResponse response = delegate.complete(request);
        record(request.getModel(), response.getUsage());
        return response;
    }

    @Override
    public LlmTokenStream stream(LlmRequest request) {
        return delegate.stream(request).handler(new CostStreamHandler(this, request.getModel()));
    }

    @Override
    public List<ModelInfo> listModels() {
        return delegate.listModels();
    }

    @Override
    public ModelValidation validateModel(String model) {
        return delegate.validateModel(model);
    }

    private void record(String modelId, LlmUsage usage) {
        if (cost == null || usage == null) {
            return;
        }
        cost.record(modelId, new CostUsage(usage.getInputTokens(), usage.getOutputTokens(), 0.0, null));
    }

    private static final class CostStreamHandler implements LlmStreamHandler {

        private final CostAccountingLlmPort port;

        private final String modelId;

        private LlmUsage usage = LlmUsage.ZERO;

        private CostStreamHandler(CostAccountingLlmPort port, String modelId) {
            this.port = port;
            this.modelId = modelId;
        }

        @Override
        public void onUsageDetail(long inputTokens, long outputTokens, long totalTokens) {
            this.usage = new LlmUsage(inputTokens, outputTokens, totalTokens);
        }

        @Override
        public void onComplete() {
            port.record(modelId, usage);
        }
    }
}
