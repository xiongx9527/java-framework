package com.xiongx9527.fw.plus.ai.support.trace;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmListener;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import com.xiongx9527.fw.plus.ai.base.trace.Span;
import com.xiongx9527.fw.plus.ai.base.trace.SpanKind;
import com.xiongx9527.fw.plus.ai.base.trace.TraceContext;
import com.xiongx9527.fw.plus.ai.base.trace.TracePort;
import java.util.UUID;

/**
 * 把 LLM 调用事件转成链路节点：请求开始开 span，结束/出错关 span 并带上用量与输出。
 *
 * <p>与具体观测后端解耦——{@link TracePort} 背后是 Langfuse / LangSmith / OTLP 的 exporter。</p>
 */
public final class TracingLlmListener implements LlmListener {

    private final TracePort trace;

    private Span.Builder span;

    private final StringBuilder content = new StringBuilder();

    private LlmUsage usage = LlmUsage.ZERO;

    public TracingLlmListener(TracePort trace) {
        this.trace = trace == null ? TracePort.noop() : trace;
    }

    @Override
    public void onRequest(LlmRequest request) {
        String traceId = TraceContext.traceId();
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            TraceContext.bind(traceId, null);
        }
        this.span = Span.start(traceId, "llm", SpanKind.LLM);
        String parent = TraceContext.spanId();
        if (parent != null) {
            this.span.parent(parent);
        }
        this.span.modelId(request.getModel());
        this.span.input(summarize(request));
    }

    @Override
    public void onToken(String token, String reasoning) {
        if (token != null) {
            content.append(token);
        }
    }

    @Override
    public void onUsage(LlmUsage value) {
        if (value != null) {
            this.usage = value;
        }
    }

    @Override
    public void onComplete() {
        close(null);
    }

    @Override
    public void onError(String message) {
        close(message);
    }

    private void close(String error) {
        if (span == null) {
            return;
        }
        span.output(content.length() == 0 ? null : content.toString());
        span.usage(usage);
        if (error != null) {
            span.error(error);
        }
        span.end(System.currentTimeMillis());
        trace.end(span);
        this.span = null;
    }

    private static String summarize(LlmRequest request) {
        StringBuilder sb = new StringBuilder();
        if (request.getMessages() != null) {
            for (ChatMessage message : request.getMessages()) {
                if (message.getRole() != null) {
                    sb.append(message.getRole().wireName()).append(": ");
                }
                String text = message.getContent();
                if (text != null) {
                    sb.append(text.length() > 500 ? text.substring(0, 500) : text);
                }
                sb.append('\n');
            }
        }
        return sb.toString();
    }
}
