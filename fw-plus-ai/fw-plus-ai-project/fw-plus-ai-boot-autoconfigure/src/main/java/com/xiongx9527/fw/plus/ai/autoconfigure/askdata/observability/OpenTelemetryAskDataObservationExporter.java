package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.observability;

import com.xiongx9527.fw.plus.ai.askdata.observability.AskDataEvent;

/** OpenTelemetry 通用占位 exporter；应用可替换为实际 OTLP 实现。 */
public class OpenTelemetryAskDataObservationExporter implements AskDataObservationExporter {
    @Override public void export(AskDataEvent event) { }
}
