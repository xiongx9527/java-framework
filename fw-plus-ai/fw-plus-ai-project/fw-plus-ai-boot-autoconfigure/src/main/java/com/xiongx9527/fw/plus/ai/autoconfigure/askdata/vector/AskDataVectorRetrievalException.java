package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.vector;

import com.xiongx9527.fw.plus.ai.model.AiModelException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 保留失败 Embedding 尝试轨迹的模型异常。 */
public final class AskDataVectorRetrievalException extends AiModelException {

    private final List<AskDataModelAttemptTrace> modelAttempts;

    public AskDataVectorRetrievalException(AiModelException cause, List<AskDataModelAttemptTrace> modelAttempts) {
        super(cause == null ? MODEL_ERROR : cause.getErrorCode(), cause != null && cause.isRetryable(),
                cause == null ? "Embedding 模型执行失败" : cause.getMessage(), cause);
        this.modelAttempts = modelAttempts == null || modelAttempts.isEmpty()
                ? Collections.<AskDataModelAttemptTrace>emptyList()
                : Collections.unmodifiableList(new ArrayList<AskDataModelAttemptTrace>(modelAttempts));
    }

    public List<AskDataModelAttemptTrace> getModelAttempts() {
        return modelAttempts;
    }
}
