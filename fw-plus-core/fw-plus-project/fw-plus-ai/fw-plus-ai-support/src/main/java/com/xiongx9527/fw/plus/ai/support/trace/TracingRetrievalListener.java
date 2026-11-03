package com.xiongx9527.fw.plus.ai.support.trace;

import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalListener;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.trace.Span;
import com.xiongx9527.fw.plus.ai.base.trace.SpanKind;
import com.xiongx9527.fw.plus.ai.base.trace.TraceContext;
import com.xiongx9527.fw.plus.ai.base.trace.TracePort;
import java.util.List;

/**
 * 把检索事件转成链路节点：记录查询文本、命中数与最高分。
 */
public final class TracingRetrievalListener implements RetrievalListener {

    private final TracePort trace;

    private Span.Builder span;

    public TracingRetrievalListener(TracePort trace) {
        this.trace = trace == null ? TracePort.noop() : trace;
    }

    @Override
    public void onQuery(RetrievalQuery query) {
        String traceId = TraceContext.traceId();
        if (traceId == null) {
            traceId = java.util.UUID.randomUUID().toString();
            TraceContext.bind(traceId, null);
        }
        span = Span.start(traceId, "retrieval", SpanKind.RETRIEVAL);
        String parent = TraceContext.spanId();
        if (parent != null) {
            span.parent(parent);
        }
        span.input(query.getText());
        span.attribute("topK", query.getTopK());
    }

    @Override
    public void onHits(List<? extends Retrieved<?>> hits) {
        if (span == null) {
            return;
        }
        int count = hits == null ? 0 : hits.size();
        double top = 0.0;
        if (hits != null) {
            for (Retrieved<?> hit : hits) {
                if (hit.getScore() > top) {
                    top = hit.getScore();
                }
            }
        }
        span.attribute("hitCount", count);
        span.attribute("topScore", top);
        span.end(System.currentTimeMillis());
        trace.end(span);
        span = null;
    }
}
