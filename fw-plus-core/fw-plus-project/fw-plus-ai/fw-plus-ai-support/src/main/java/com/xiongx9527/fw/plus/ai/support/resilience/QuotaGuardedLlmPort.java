package com.xiongx9527.fw.plus.ai.support.resilience;

import com.xiongx9527.fw.plus.ai.base.model.LlmException;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.base.resilience.QuotaPort;
import java.util.List;

/**
 * 配额闸门：调用前按模型 id 占用配额，拒绝时抛 {@link LlmException}，不触达上游。
 */
public final class QuotaGuardedLlmPort implements LlmPort {

    private final LlmPort delegate;

    private final QuotaPort quota;

    private final long amount;

    public QuotaGuardedLlmPort(LlmPort delegate, QuotaPort quota, long amount) {
        this.delegate = delegate;
        this.quota = quota;
        this.amount = amount <= 0 ? 1L : amount;
    }

    @Override
    public LlmResponse complete(LlmRequest request) {
        acquire(request.getModel());
        return delegate.complete(request);
    }

    @Override
    public LlmTokenStream stream(LlmRequest request) {
        acquire(request.getModel());
        return delegate.stream(request);
    }

    @Override
    public List<ModelInfo> listModels() {
        return delegate.listModels();
    }

    @Override
    public ModelValidation validateModel(String model) {
        return delegate.validateModel(model);
    }

    private void acquire(String scope) {
        if (quota != null && !quota.tryAcquire(scope, amount)) {
            throw new LlmException("quota exhausted for scope: " + scope);
        }
    }
}
