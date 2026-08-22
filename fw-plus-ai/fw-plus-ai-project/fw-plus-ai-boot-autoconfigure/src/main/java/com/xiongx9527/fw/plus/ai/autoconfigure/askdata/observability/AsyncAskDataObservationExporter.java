package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventCorrelation;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/** 异步扇出 exporter；任何单个 exporter 的异常均被隔离。 */
public class AsyncAskDataObservationExporter implements AskDataEventPublisher {

    private final List<AskDataObservationExporter> exporters;
    private final Executor executor;
    private final AskDataEventPublisher diagnosticPublisher;

    public AsyncAskDataObservationExporter(List<AskDataObservationExporter> exporters, Executor executor) {
        this(exporters, executor, null);
    }

    public AsyncAskDataObservationExporter(List<AskDataObservationExporter> exporters, Executor executor,
            AskDataEventPublisher diagnosticPublisher) {
        this.exporters = exporters == null ? Collections.<AskDataObservationExporter>emptyList()
                : new ArrayList<AskDataObservationExporter>(exporters);
        this.executor = executor;
        this.diagnosticPublisher = diagnosticPublisher;
    }

    @Override
    public void publish(final AskDataEvent event) {
        if (executor == null || event == null) { return; }
        for (final AskDataObservationExporter exporter : exporters) {
            if (exporter == null) { continue; }
            try {
                executor.execute(new Runnable() {
                    @Override public void run() {
                        try { exporter.export(event); } catch (RuntimeException exportFailure) {
                            publishDiagnostic(event);
                        }
                    }
                });
            } catch (RuntimeException rejected) {
                publishDiagnostic(event);
            }
        }
    }

    private void publishDiagnostic(AskDataEvent source) {
        if (diagnosticPublisher == null || source == null) { return; }
        try {
            AskDataEventCorrelation original = source.getCorrelation();
            AskDataEvent diagnostic;
            if (original == null) {
                diagnostic = new AskDataEvent(source.getContext(), source.getTraceId(),
                        AskDataEvent.Type.OBSERVABILITY_EXPORT_DEGRADED, "AskData 观测导出已降级");
            } else {
                AskDataEventCorrelation correlation = AskDataEventCorrelation
                        .builder(original.getRequestId(), original.getConversationId())
                        .subQuestionId(original.getSubQuestionId()).graphNodeId(original.getGraphNodeId())
                        .agentKey(original.getAgentKey()).instanceId(original.getInstanceId())
                        .parentEventId(original.getEventId()).build();
                diagnostic = AskDataEvent.correlated(source.getContext(), source.getTraceId(),
                        AskDataEvent.Type.OBSERVABILITY_EXPORT_DEGRADED, "AskData 观测导出已降级", correlation);
            }
            diagnosticPublisher.publish(diagnostic);
        } catch (RuntimeException diagnosticFailure) {
            // 本地诊断失败不能影响业务线程或其他 exporter。
        }
    }
}
