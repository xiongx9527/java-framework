package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;
import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEventPublisher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

/** 异步扇出 exporter；任何单个 exporter 的异常均被隔离。 */
public class AsyncAskDataObservationExporter implements AskDataEventPublisher {

    private final List<AskDataObservationExporter> exporters;
    private final Executor executor;

    public AsyncAskDataObservationExporter(List<AskDataObservationExporter> exporters, Executor executor) {
        this.exporters = exporters == null ? Collections.<AskDataObservationExporter>emptyList()
                : new ArrayList<AskDataObservationExporter>(exporters);
        this.executor = executor;
    }

    @Override
    public void publish(final AskDataEvent event) {
        if (executor == null || event == null) { return; }
        for (final AskDataObservationExporter exporter : exporters) {
            if (exporter == null) { continue; }
            executor.execute(new Runnable() {
                @Override public void run() {
                    try { exporter.export(event); } catch (RuntimeException ignored) { }
                }
            });
        }
    }
}
