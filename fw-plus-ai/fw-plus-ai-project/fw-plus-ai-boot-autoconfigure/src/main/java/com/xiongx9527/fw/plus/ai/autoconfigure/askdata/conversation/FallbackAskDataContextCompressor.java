package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataStepContext;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventCorrelation;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher;

/**
 * 先尝试语义压缩，失败或返回空摘要时使用确定性压缩。
 *
 * <p>无论语义实现返回什么结构化字段，最终结果都以确定性压缩器生成的恢复安全字段为准；语义实现只替换摘要正文。</p>
 */
public class FallbackAskDataContextCompressor implements AskDataContextCompressor {

    private final AskDataContextCompressor semanticCompressor;
    private final AskDataContextCompressor deterministicCompressor;
    private final AskDataEventPublisher diagnosticPublisher;

    public FallbackAskDataContextCompressor(AskDataContextCompressor semanticCompressor,
            AskDataContextCompressor deterministicCompressor, AskDataEventPublisher diagnosticPublisher) {
        if (semanticCompressor == null || deterministicCompressor == null) {
            throw new IllegalArgumentException("semanticCompressor 和 deterministicCompressor 不能为空");
        }
        this.semanticCompressor = semanticCompressor;
        this.deterministicCompressor = deterministicCompressor;
        this.diagnosticPublisher = diagnosticPublisher;
    }

    @Override
    public AskDataCompressedContext compress(AskDataStepContext context) {
        try {
            AskDataCompressedContext semantic = semanticCompressor.compress(context);
            if (semantic == null || isBlank(semantic.getCompressedSummary())) {
                throw new IllegalStateException("语义压缩未返回摘要");
            }
            AskDataCompressedContext safe = deterministicCompressor.compress(context);
            int budget = safe.getCompressedSummary() == null ? semantic.getCompressedSummary().length()
                    : safe.getCompressedSummary().length();
            safe.setCompressedSummary(semantic.getCompressedSummary().length() <= budget
                    ? semantic.getCompressedSummary() : semantic.getCompressedSummary().substring(0, budget));
            return safe;
        } catch (RuntimeException semanticFailure) {
            publishDegraded(context);
            return deterministicCompressor.compress(context);
        }
    }

    private void publishDegraded(AskDataStepContext context) {
        if (diagnosticPublisher == null || context == null || context.getRequest() == null
                || context.getRequest().getProjectContext() == null) {
            return;
        }
        AskDataRequest request = context.getRequest();
        String conversationId = isBlank(request.getConversationId()) ? "unassigned" : request.getConversationId();
        AskDataEventCorrelation correlation = AskDataEventCorrelation.builder(conversationId, conversationId)
                .graphNodeId(context.getCurrentStep()).build();
        try {
            diagnosticPublisher.publish(AskDataEvent.correlated(request.getProjectContext(), conversationId,
                    AskDataEvent.Type.CONTEXT_COMPRESSION_DEGRADED, "AskData 上下文压缩已降级", correlation));
        } catch (RuntimeException diagnosticFailure) {
            // 诊断失败不能覆盖确定性压缩结果。
        }
    }

    private boolean isBlank(String value) { return value == null || value.trim().length() == 0; }
}
